/**
 * C-JDBC: Clustered JDBC.
 * Copyright (C) 2002-2004 French National Institute For Research In Computer
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
 * Initial developer(s): Nicolas Modrzyk.
 * Contributor(s): ______________________.
 */

package org.objectweb.cjdbc.scenario.raidb1.driver;

import java.sql.Connection;

import org.objectweb.cjdbc.scenario.templates.Raidb1Template;
import org.objectweb.cjdbc.scenario.tools.testlet.AbstractTestLet;
import org.objectweb.cjdbc.scenario.tools.testlet.ProcedureTestLet;

/**
 * This class defines a StoredProcedureScenario
 * 
 * @author <a href="mailto:Nicolas.Modrzyk@inrialpes.fr">Nicolas Modrzyk </a>
 * @version 1.0
 */
public class StoredProcedureScenario extends Raidb1Template
{
  /**
   * Test a simple stored procedure with hypersonic backends
   * 
   * @throws Exception if fails
   */
  public void testStoredProcedureOnHypersonic() throws Exception
  {
    Connection con = getCJDBCConnection();
    con.setCatalog("myDB");
    ProcedureTestLet let = new ProcedureTestLet(con);
    let.executeBatch(AbstractTestLet.USE_PREPARED_STATEMENT,
        new Boolean[]{Boolean.FALSE});
  }
}