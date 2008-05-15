package net.databinder.web;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.wadi.WadiCluster;
import org.mortbay.jetty.servlet.wadi.WadiSessionHandler;
import org.mortbay.jetty.servlet.wadi.WadiSessionManager;
import org.mortbay.jetty.webapp.WebAppContext;

public class ClusterServer extends DataServer {
	public static void main(String[] args)
	{
		new ClusterServer();
	}
	private WadiCluster cluster;

	@Override
	protected void configure(Server server, WebAppContext context) throws Exception {
		cluster = new WadiCluster("Databinder", 
			System.getProperty("jetty.node", "one"), // does not seem to matter if not unique
			"http://localhost:" + server.getConnectors()[0].getPort() // "not used", but seemed to need to be unique
		);
		cluster.start();
		context.setSessionHandler(new WadiSessionHandler(new WadiSessionManager(cluster, 10, 24, 360)));
	}

	@Override
	protected void stopped(Server server, WebAppContext context) throws Exception {
		cluster.stop();
	}
}
