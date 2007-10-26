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

import org.apache.wicket.util.string.Strings;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional main() class for running embedded Jetty. Client applications may pass
 * this classname to the Java runtime to serve with no other configuration. The webroot 
 * defaults to src/main/webapp, and the server to a context named after the current directory 
 * on port 8080. <tt>jetty.warPath</tt>, <tt>jetty.contextPath</tt>, and <tt>jetty.port</tt> system properties
 * may be used to override (e.g. <tt>-Djetty.port=80</tt> as a command line parameter).
 * @author Nathan Hamblen
 */
public class DataServer {
	private static final Logger log = LoggerFactory.getLogger(DataServer.class);

	public static void main(String[] args) throws Exception
	{
		Server server = new Server();

		WebAppContext web = new WebAppContext();
		
		String contextPath = System.getProperty("jetty.contextPath");
		if (Strings.isEmpty(contextPath)) {
			contextPath = "/" + new File(".").getCanonicalFile().getName();
			log.info("context path by current directory: " + contextPath);
		}
		else
			log.info("jetty.contextPath property: " + contextPath);
		web.setContextPath(contextPath);
		
		String warPath = System.getProperty("jetty.warPath");
		if (Strings.isEmpty(warPath)) warPath = "src/main/webapp";
		else log.info("jetty.warPath property: " + warPath);
		web.setWar(warPath);
		
		server.addHandler(web);
		
		if (!contextPath.equals("/"))
			server.addHandler(new MovedContextHandler(server, "/", contextPath));

		SelectChannelConnector connector = new SelectChannelConnector();
		try {
			connector.setPort(Integer.valueOf(System.getProperty("jetty.port")));
			log.info("jetty.port property: " + connector.getPort());
		} catch (NumberFormatException e) {
			connector.setPort(8080);
		}
		server.setConnectors(new Connector[] { connector });

		server.start();
		log.info("Ready at http://localhost:" + connector.getPort() + contextPath);
		server.join();
	}

}
