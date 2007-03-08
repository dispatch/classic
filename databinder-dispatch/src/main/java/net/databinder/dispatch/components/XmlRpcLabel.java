/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
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
package net.databinder.dispatch.components;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import net.databinder.components.CustomLabel;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcLiteHttpTransportFactory;

import wicket.Application;
import wicket.ResourceReference;
import wicket.RestartResponseAtInterceptPageException;
import wicket.model.IModel;
import wicket.util.convert.converters.AbstractConverter;

/**
 * Label that converts its model through an XML-RPC call before display. Results are
 * cached to avoid repetitive network calls.
 * @author Nathan Hamblen
 */
public abstract class XmlRpcLabel extends CustomLabel {
	private static XmlRpcClient client;
	private static XmlRpcClientConfig config;
	/** External script source that responds to XML-RPC requests. */
	public static final ResourceReference scriptFile = new ResourceReference(XmlRpcLabel.class, "databinder-dispatch.rb");

	/**
	 * @param id Wicket id of component
	 * @param converter specific converter to use before display
	 */
	protected XmlRpcLabel(String id, AbstractConverter converter) {
		super(id, converter);
	}
	
	/**
	 * @param id Wicket id of component
	 * @param model model to be passed through converter
	 * @param converter specific converter to use before display
	 */
	protected XmlRpcLabel(String id, IModel model, AbstractConverter converter) {
		super(id, model, converter);
	}

	/** Set custom XML-RPC configuration. Default is endpoint http://localhost:8180/ */
	public static void setXmlRpcConfig(XmlRpcClientConfig xmlRpcConfig) {
		XmlRpcLabel.config = xmlRpcConfig;
	}

	/**
	 * Lazy loads XML-RPC singleton for this class. Uses custom configuration if set previously
	 * in setXmlRpcConfig(), otherwise http://localhost:8180/.
	 * @return lazily initialized XML-RPC client object
	 */
	protected static XmlRpcClient getClient() {
		if (client == null)
			synchronized (XmlRpcLabel.class) {
				if (client == null) { // check again, in case blocked 
					client = new XmlRpcClient();
					if (config == null) {
						config = new XmlRpcClientConfigImpl();
						try {
							((XmlRpcClientConfigImpl)config).setServerURL(new URL("http://localhost:8180/"));
						} catch (MalformedURLException e) {}
					}
					client.setTransportFactory(new XmlRpcLiteHttpTransportFactory(client));
					client.setConfig(config);
				}
			}
		return client;
	}
	
	/**
	 * @param methodName associated name of XML-RPC method
	 * @return lazily instantiated cache, specific to given methodName.
	 */
	protected static Ehcache getCache(String methodName) {
		CacheManager mgr = CacheManager.getInstance();
		String name = XmlRpcLabel.class.getName() + ":" + methodName;
		Ehcache cache = mgr.getEhcache(name);
		if (cache != null)
			return cache;
		mgr.addCache(name);
		return mgr.getEhcache(name);
	}

	/**
	 * Converter calls into XML-RPC method name given by subclass (or finds result in cache).
	 */
	public abstract static class XmlRpcConverter extends AbstractConverter {
		protected abstract  String getMethodName();
		public Object convertToObject(String value, Locale locale) {
			return null;
		}
		@Override
		protected Class getTargetType() {
			return String.class;
		}
		public String convertToString(Object source, Locale locale) {
			String methodName = getMethodName();
			int key = source.hashCode();
			Ehcache cache = getCache(methodName);
			Element elem = cache.get(key);
			if (elem != null) return (String) elem.getValue();
			try{
				 Object out = getClient().execute(methodName, new Object[] {source});
				cache.put(new Element(key, out));
				return (String) out;
			} catch (XmlRpcException e) {
				if (Application.get().getConfigurationType().equals(Application.DEVELOPMENT))
					throw new RestartResponseAtInterceptPageException(new ConnectionErrorPage(e));
				else
					throw new RuntimeException(e);
			}
		}
	}
}

