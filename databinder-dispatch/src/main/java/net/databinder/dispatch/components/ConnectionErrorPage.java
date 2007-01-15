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

import javax.servlet.http.HttpServletRequest;

import net.databinder.components.DataPage;

import org.apache.xmlrpc.XmlRpcException;

import wicket.Component;
import wicket.WicketRuntimeException;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.Link;
import wicket.model.AbstractReadOnlyModel;
import wicket.model.Model;
import wicket.protocol.http.WebRequest;
import wicket.util.io.Streams;
import wicket.util.resource.IResourceStream;

/**
 * Page describing connection problem and offering source for external script.
 * @author Nathan Hamblen
 */
public class ConnectionErrorPage extends DataPage {
	@Override
	protected String getName() {
		return "XML-RPC Connection Error";
	}
	
	public ConnectionErrorPage(XmlRpcException e) {
		add(new Label("error", new Model(e.getMessage())));
		add(new Link("retry") {
			@Override
			public void onClick() {
				continueToOriginalDestination();
			}
		});
		add(new Label("script", new AbstractReadOnlyModel() {
			@Override
			public Object getObject(Component component) {
				try {
					IResourceStream stream = XmlRpcLabel.scriptFile.getResource().getResourceStream();
					String script = Streams.readString(stream.getInputStream());
					stream.close();
					return script;
				} catch (Exception e) {
					throw new WicketRuntimeException(e);
				}
			}
		}));
		
		HttpServletRequest req =  ((WebRequest)getRequest()).getHttpServletRequest();
		String base = "http://" + req.getServerName() + ":" + req.getServerPort();
		
		add(new Label("href", new Model(base + urlFor(XmlRpcLabel.scriptFile))).setRenderBodyOnly(true));
	}
}
