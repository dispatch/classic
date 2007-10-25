/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2007  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.databinder.web;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.util.string.Strings;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Optional main() class for running embedded Jetty. Client applications may pass
 * this classname to the Java runtime to serve with no other configuration. The webroot 
 * defaults to src/main/webapp, and the server to a root servlet context 
 * on port 8080. <tt>jetty.warPath</tt>, <tt>jetty.contextPath</tt>, and <tt>jetty.port</tt> system properties
 * may be used to override (e.g. <tt>-Djetty.port=80</tt> as a command line parameter).
 * @author Nathan Hamblen
 */
public class DataServer {
	private static final Log log = LogFactory.getLog(DataServer.class);

	public static void main(String[] args) throws Exception
	{
		Server server = new Server();

		WebAppContext web = new WebAppContext();
		
		String contextPath = System.getProperty("jetty.contextPath");
		if (Strings.isEmpty(contextPath)) 
			contextPath = "/" + new File(".").getCanonicalFile().getName();
		web.setContextPath(contextPath);
		
		String warPath = System.getProperty("jetty.warPath");
		if (Strings.isEmpty(warPath)) warPath = "src/main/webapp";
		web.setWar(warPath);
		
		server.addHandler(web);
		
		if (!contextPath.equals("/"))
			server.addHandler(new MovedContextHandler(server, "/", contextPath));

		SelectChannelConnector connector = new SelectChannelConnector();
		try {
			connector.setPort(Integer.valueOf(System.getProperty("jetty.port")));
		} catch (NumberFormatException e) {
			connector.setPort(8080);
		}
		server.setConnectors(new Connector[] { connector });

		server.start();
		log.info("Ready at http://localhost:" + connector.getPort() + contextPath);
		server.join();
	}

}
