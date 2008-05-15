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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.util.string.Strings;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.ajp.Ajp13SocketConnector;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional starter class for embedded Jetty. Client applications may pass
 * this classname to the Java runtime to serve with no other configuration. The webroot 
 * defaults to src/main/webapp, and the server to a context named after the project directory 
 * with HTTP on port 8080. <tt>jetty.warpath</tt>, <tt>jetty.contextpath</tt>,
 * <tt>jetty.port</tt>, and <tt>jetty.ajp.port</tt> system properties
 * may be used to override (e.g. <tt>-Djetty.port=80</tt> as a command line parameter). AJP
 * is enabled by specifying a port, and HTTP disabled by setting jetty.port to 0.
 * 
 * <p>Some customization can be accomplished by extending this class and overriding
 * the {@link DataServer#configure(Server, WebAppContext)} method. The subclass
 * will need a static main(args) method that constructs an instance of itself, similar
 * to {@link DataServer#main(String[])}  
 * @author Nathan Hamblen
 */
public class DataServer {
	private static final Logger log = LoggerFactory.getLogger(DataServer.class);

	/** Constructs DataServer, kicking off server. */
	public static void main(String[] args)
	{
		new DataServer();
	}
	
	/** Starts web sever using any parameters supplied. Calls 
	 * {DataServer{@link #configure(Server, WebAppContext)} immediately before 
	 * starting server.
	 */
	public DataServer() {
		try {
			Server server = new Server();
	
			WebAppContext web = new WebAppContext();
			
			URL classes = null;
			String projectDir = null;
			try {
				// look for project's classes directory
				URL[] urls = ((URLClassLoader)DataServer.class.getClassLoader()).getURLs();
				for (URL url : urls)
					if (url.getPath().endsWith("classes/")) {
						classes = url;
						break;
					}
			} catch (Exception e) { 
				log.info("unable to find project path by classloader", e);
			}
	
			if (classes == null) {
				projectDir = new File(".").getCanonicalPath();
				log.info("project path fram current directory: " + projectDir);
			} else {
				projectDir = classes.toURI().resolve("../..").getPath();
				log.info("project path as found by classloader: " + projectDir);
			}
	
			String contextPath = System.getProperty("jetty.contextpath", 
					System.getProperty("jetty.contextPath"));
			if (Strings.isEmpty(contextPath)) {
				Matcher m = Pattern.compile("(\\/[^\\/]+)/?$").matcher(projectDir);
				if (!m.find())
					throw new RuntimeException("Project path not as expected: " + projectDir);
				contextPath = m.group(1);
				log.info("context path by project directory: " + contextPath);
			}
			else
				log.info("jetty.contextPath property: " + contextPath);
			web.setContextPath(contextPath);
			
			String warPath = System.getProperty("jetty.warpath", 
					System.getProperty("jetty.warPath"));
			if (Strings.isEmpty(warPath)) warPath = projectDir + "src/main/webapp";
			
			if (!new File(warPath).isDirectory()) {
				log.error("Unable to find webapps path: " + warPath +
					" \nPlease ensure that this project is the first on its " +
					"classpath, or set a valid jetty.warpath JVM property.");
				return;
			}
	
			
			else log.info("jetty.warPath property: " + warPath);
			web.setWar(warPath);
			
			server.addHandler(web);
			
			if (!contextPath.equals("/"))
				server.addHandler(new MovedContextHandler(server, "/", contextPath));
	
			List<Connector> conns = new ArrayList<Connector>(2);
			
			int httpPort = 8080;
			try {
				httpPort = Integer.valueOf(System.getProperty("jetty.port"));
				log.info("jetty.port property: " + httpPort);
			} catch (NumberFormatException e) { }
			
			if (httpPort != 0) {
				SelectChannelConnector httpConn = new SelectChannelConnector();
				httpConn.setPort(httpPort);
				conns.add(httpConn);
			}
	
			int ajpPort = 0;
			try {
				ajpPort = Integer.valueOf(System.getProperty("jetty.ajp.port"));
				log.info("jetty.ajp.port property: " + ajpPort);
			} catch (NumberFormatException e) { }
			
			if (ajpPort != 0) {
				Ajp13SocketConnector ajpConn = new Ajp13SocketConnector();
				ajpConn.setPort(ajpPort);
				conns.add(ajpConn);
			}
			
			server.setConnectors(conns.toArray(new Connector[conns.size()]));
			server.setStopAtShutdown(true);
			
			configure(server, web);
	
			server.start();
			if (httpPort != 0)
				log.info("Ready at http://localhost:" + httpPort + contextPath);
			if (ajpPort != 0)
				log.info("Ready at ajp://localhost:" + ajpPort + contextPath);
			server.join();
			
			stopped(server, web);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Override to customize the server and context objects.
	 * @see DataServer#DataServer()
	 */
	protected void configure(Server server, WebAppContext context) throws Exception { }
	
	/**
	 * Override to perform action after server stops
	 * @see DataServer#DataServer()
	 */
	protected void stopped(Server server, WebAppContext context) throws Exception {  }
}
