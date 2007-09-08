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

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Page describing connection problem and offering source for external script.
 * @author Nathan Hamblen
 */
public class ConnectionErrorPage extends WebPage {

	public ConnectionErrorPage(XmlRpcException e) {
		add(new Label("error", new Model(e.getMessage())));
		add(new Link("retry") {
			@Override
			public void onClick() {
				continueToOriginalDestination();
			}
		});
		ResourceLink script = new ResourceLink("script", XmlRpcLabel.scriptFile);
		add(script);

		HttpServletRequest req =  ((WebRequest)getRequest()).getHttpServletRequest();
		URI full= URI.create(req.getRequestURL().toString());
		Model path = new Model(full.resolve(req.getContextPath() +"/" + urlFor(XmlRpcLabel.scriptFile)));

		add(new Label("href", path).setRenderBodyOnly(true));
	}
}
