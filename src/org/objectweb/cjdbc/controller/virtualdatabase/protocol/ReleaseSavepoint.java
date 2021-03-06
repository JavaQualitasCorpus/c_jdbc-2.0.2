/**
 * C-JDBC: Clustered JDBC.
 * Copyright (C) 2002-2005 French National Institute For Research In Computer
 * Science And Control (INRIA).
 * Contact: c-jdbc@objectweb.org
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or any later
 * version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * Initial developer(s): Jean-Bernard van Zuylen.
 * Contributor(s): ______________________.
 */

package org.objectweb.cjdbc.controller.virtualdatabase.protocol;

import java.sql.SQLException;

import org.objectweb.cjdbc.common.exceptions.NoMoreBackendException;
import org.objectweb.cjdbc.common.i18n.Translate;
import org.objectweb.cjdbc.common.sql.AbstractRequest;
import org.objectweb.cjdbc.common.sql.UnknownRequest;
import org.objectweb.cjdbc.controller.loadbalancer.AllBackendsFailedException;
import org.objectweb.cjdbc.controller.requestmanager.TransactionMarkerMetaData;
import org.objectweb.cjdbc.controller.requestmanager.distributed.DistributedRequestManager;

/**
 * Execute a distributed release savepoint
 * 
 * @author <a href="mailto:jbvanzuylen@transwide.com">Jean-Bernard van Zuylen
 *         </a>
 * @version 1.0
 */
public class ReleaseSavepoint extends DistributedTransactionMarker
{
  private static final long         serialVersionUID = 3025352266902951038L;

  private String                    savepointName;
  private TransactionMarkerMetaData tm;
  private Long                      tid;
  private int                       numberOfEnabledBackends;

  /**
   * Creates a new <code>ReleaseSavepoint</code> message
   * 
   * @param transactionId the transaction identifier
   * @param savepointName the savepoint name
   */
  public ReleaseSavepoint(long transactionId, String savepointName)
  {
    super(transactionId);
    this.savepointName = savepointName;
  }

  /**
   * @see org.objectweb.cjdbc.controller.virtualdatabase.protocol.DistributedTransactionMarker#scheduleCommand(org.objectweb.cjdbc.controller.requestmanager.distributed.DistributedRequestManager)
   */
  public void scheduleCommand(DistributedRequestManager drm)
      throws SQLException
  {
    try
    {
      // Let's find the transaction marker since it will be used even for
      // logging purposes
      tid = new Long(transactionId);
      tm = drm.getTransactionMarker(tid);

      numberOfEnabledBackends = drm.getLoadBalancer()
          .getNumberOfEnabledBackends();
      if (numberOfEnabledBackends == 0)
        return; // Nothing to do

      // Check that a savepoint with given name has been set
      if (!drm.hasSavepoint(tid, savepointName))
        throw new SQLException(Translate.get("transaction.savepoint.not.found",
            new String[]{savepointName, String.valueOf(transactionId)}));

      // Wait for the scheduler to give us the authorization to execute
      drm.getScheduler().releaseSavepoint(tm, savepointName);
    }
    catch (SQLException e)
    {
      drm
          .getLogger()
          .warn(
              Translate
                  .get("virtualdatabase.distributed.releasesavepoint.sqlexception"),
              e);
      throw e;
    }
    catch (RuntimeException re)
    {
      drm.getLogger().warn(
          Translate
              .get("virtualdatabase.distributed.releasesavepoint.exception"),
          re);
      throw new SQLException(re.getMessage());
    }
  }

  /**
   * @see org.objectweb.cjdbc.controller.virtualdatabase.protocol.DistributedTransactionMarker#executeCommand(org.objectweb.cjdbc.controller.requestmanager.distributed.DistributedRequestManager)
   */
  public Object executeCommand(DistributedRequestManager drm)
      throws SQLException
  {
    try
    {
      if (numberOfEnabledBackends == 0)
        throw new NoMoreBackendException(
            "No backend enabled on this controller");

      if (drm.getLogger().isDebugEnabled())
        drm.getLogger().debug(
            Translate.get("transaction.releasesavepoint", new String[]{
                savepointName, String.valueOf(transactionId)}));

      // Send to load balancer
      drm.getLoadBalancer().releaseSavepoint(tm, savepointName);

      // Notify the recovery log manager
      if (drm.getRecoveryLog() != null)
      {
        drm.getRecoveryLog().logReleaseSavepoint(tm, savepointName);
      }
    }
    catch (NoMoreBackendException e)
    {
      // Log the query in any case for later recovery (if the request really
      // failed, it will be unloged later)
      if (drm.getRecoveryLog() != null)
      {
        if (drm.getLogger().isDebugEnabled())
          drm.getLogger().debug(
              Translate.get(
                  "virtualdatabase.distributed.releasesavepoint.logging.only",
                  new String[]{savepointName, String.valueOf(transactionId)}));
        long logId = drm.getRecoveryLog()
            .logReleaseSavepoint(tm, savepointName);
        e.setRecoveryLogId(logId);
        e.setLogin(tm.getLogin());
      }
      throw e;
    }
    catch (SQLException e)
    {
      drm
          .getLogger()
          .warn(
              Translate
                  .get("virtualdatabase.distributed.releasesavepoint.sqlexception"),
              e);
      return e;
    }
    catch (RuntimeException re)
    {
      drm.getLogger().warn(
          Translate
              .get("virtualdatabase.distributed.releasesavepoint.exception"),
          re);
      throw new SQLException(re.getMessage());
    }
    catch (AllBackendsFailedException e)
    {
      AbstractRequest request = new UnknownRequest("release " + savepointName,
          false, 0, "\n");
      request.setTransactionId(transactionId);
      drm.addFailedOnAllBackends(request);
      if (drm.getLogger().isDebugEnabled())
        drm
            .getLogger()
            .debug(
                Translate
                    .get(
                        "virtualdatabase.distributed.releasesavepoint.all.backends.locally.failed",
                        new String[]{savepointName,
                            String.valueOf(transactionId)}));
      return e;
    }
    finally
    {
      if (numberOfEnabledBackends != 0)
      {
        // Notify scheduler for completion
        drm.getScheduler().savepointCompleted(transactionId);

        // Remove savepoint for the transaction
        drm.removeSavepoint(tid, savepointName);
      }
    }
    return Boolean.TRUE;
  }

  /**
   * Returns the savepointName value.
   * 
   * @return Returns the savepointName.
   */
  public String getSavepointName()
  {
    return savepointName;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
    if (super.equals(obj))
      return savepointName.equals(((ReleaseSavepoint) obj).getSavepointName());
    else
      return false;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return "Release savepoint " + savepointName + " from transaction "
        + transactionId;
  }
}
