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
 * Initial developer(s): Marc Wick.
 * Contributor(s): ______________________.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import javax.security.auth.Subject;

import org.objectweb.cjdbc.common.jmx.JmxConstants;
import org.objectweb.cjdbc.common.jmx.mbeans.VirtualDatabaseMBean;
import org.objectweb.cjdbc.common.users.VirtualDatabaseUser;
import org.objectweb.cjdbc.controller.authentication.PasswordAuthenticator;

/**
 * This class defines a SimpleJmxClient
 * 
 * @author <a href="mailto:marc.wick@monte-bre.ch">Marc Wick </a>
 * @version 1.0
 */
public class SimpleJmxClient
{

  /**
   * a simple jmx client exmaple to show how to access the c-jdbc controller via
   * jmx
   * 
   * @param args - no args
   * @throws Exception - an exception
   */
  public static void main(String[] args) throws Exception
  {
    String port = "8091";
    String host = "localhost";
    String vdbName = "rubis";
    JMXServiceURL address = new JMXServiceURL("rmi", host, 0, "/jndi/jrmp");

    Map environment = new HashMap();
    environment.put(Context.INITIAL_CONTEXT_FACTORY,
        "com.sun.jndi.rmi.registry.RegistryContextFactory");
    environment.put(Context.PROVIDER_URL, "rmi://" + host + ":" + port);

    // use username and password for authentication of connections
    // with the controller, the values are compared to the ones
    // specified in the controller.xml config file.
    // this line is not required if no username/password has been configered
    environment.put(JMXConnector.CREDENTIALS, PasswordAuthenticator
        .createCredentials("jmxuser", "jmxpassword"));

    JMXConnector connector = JMXConnectorFactory.connect(address, environment);

    ObjectName db = JmxConstants.getVirtualDbObjectName(vdbName);

    // we build a subject for authentication
    VirtualDatabaseUser dbUser = new VirtualDatabaseUser("admin", "c-jdbc");
    Set principals = new HashSet();
    principals.add(dbUser);
    Subject subj = new Subject(true, principals, new HashSet(), new HashSet());

    // we open a connection for this subject, all susequent calls with this
    // connection will be executed on the behalf of our subject.
    MBeanServerConnection delegateConnection = connector
        .getMBeanServerConnection(subj);

    // we create a proxy to the virtual database
    VirtualDatabaseMBean proxy = (VirtualDatabaseMBean) MBeanServerInvocationHandler
        .newProxyInstance(delegateConnection, db, VirtualDatabaseMBean.class,
            false);

    // we call a method on the virtual database
    System.out.println(proxy.getAllBackendNames());

 
  }
}
