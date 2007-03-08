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

import wicket.model.IModel;

/**
 * Converts its model via the Ruby <a href="http://maruku.rubyforge.org/">Maruku</a> library,
 * superset of the <a href="http://daringfireball.net/projects/markdown/">Markdown</a> 
 * processing library. This component depends upon a  properly configured XML-RPC listener at the endpoint 
 * configured in XmlRpcLabel.
 * @see XmlRpcLabel 
 * @author Nathan Hamblen
 */
public class MarukuLabel extends XmlRpcLabel {

	/**
	 * @param Wicket id
	 */
	public MarukuLabel(String id) {
		super(id, new MarukuConverter());
		setEscapeModelStrings(false);
	}
/**
 * @param Wicket id
 * @param String model
 */
	public MarukuLabel(String id, IModel model) {
		super(id, model, new MarukuConverter());
		setEscapeModelStrings(false);
	}
	/** XML-RPC method name is "maruku.to_html". */
	public static class MarukuConverter extends XmlRpcConverter {
		@Override
		protected String getMethodName() {
			return "maruku.to_html";
		}
	}
}
