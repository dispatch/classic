package net.databinder.dispatch.components;


import java.net.MalformedURLException;
import java.net.URL;

import net.databinder.components.CustomLabel;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcLiteHttpTransportFactory;

import wicket.model.IModel;

public abstract class XmlRpcLabel extends CustomLabel {

	private static XmlRpcClient client = new XmlRpcClient();
	private static Ehcache cache;
	static {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL("http://mini:8180/"));
		} catch (MalformedURLException e) {}
		client.setTransportFactory(new XmlRpcLiteHttpTransportFactory(client));
		client.setConfig(config);

		CacheManager mgr = CacheManager.getInstance();
		String name = XmlRpcLabel.class.getName();
		mgr.addCache(name);
		cache = mgr.getEhcache(name);
	}

	/**
	 * @param id Wicket id of component
	 * @param converter specific converter to use before display
	 */
	protected XmlRpcLabel(String id, CustomConverter converter) {
		super(id, converter);
	}
	
	/**
	 * @param id Wicket id of component
	 * @param model model to be passed through converter
	 * @param converter specific converter to use before display
	 */
	protected XmlRpcLabel(String id, IModel model, CustomConverter converter) {
		super(id, model, converter);
	}

	protected abstract static class XmlRpcConverter extends CustomConverter {
		protected abstract  String getMethodName();
		public Object convert(Object source, Class cl) {
			int key = source.hashCode();
			Element elem = cache.get(key);
			if (elem != null) return elem.getValue();
			try{
				 Object out = client.execute(getMethodName(), new Object[] {source});
				cache.put(new Element(key, out));
				return out;
			} catch (XmlRpcException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

