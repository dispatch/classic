package net.databinder.web;

import org.apache.wicket.util.string.Strings;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Optional main() class for running embedded Jetty. Client applications may pass
 * this classname to the Java runtime to serve with no other configuration. The webroot 
 * is expected to be in src/main/webapp, and the server defaults to a root servlet context 
 * on port 8080. <tt>jetty.contextPath</tt> and <tt>jetty.port</tt> system properties
 * may be used to override (e.g. <tt>-Djetty.port=80</tt> as a command line parameter).
 * @author Nathan Hamblen
 */
public class DataServer {

	public static void main(String[] args) throws Exception
	{
		Server server = new Server();

		WebAppContext web = new WebAppContext();
		
		String contextPath = System.getProperty("jetty.contextPath");
		if (Strings.isEmpty(contextPath)) contextPath = "/";
		web.setContextPath(contextPath);
		
		web.setWar("src/main/webapp");
		
		server.addHandler(web);

		SelectChannelConnector connector = new SelectChannelConnector();
		try {
			connector.setPort(Integer.valueOf(System.getProperty("jetty.port")));
		} catch (NumberFormatException e) {
			connector.setPort(8080);
		}
		server.setConnectors(new Connector[] { connector });

		server.start();
		server.join();
	}

}
