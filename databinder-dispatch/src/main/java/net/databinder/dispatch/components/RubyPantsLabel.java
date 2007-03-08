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
 * Converts its model via the <a href="http://chneukirchen.org/blog/static/projects/rubypants.html">RubyPants</a>
 * processing library before display. Based on 
 * <a href="http://daringfireball.net/projects/smartypants/">SmartyPants</a>, RubyPants 
 * converts standard HTML with straight quotes and double-hyphens to standard HTML 
 * with curved quotes, em dashes, etc. This component depends upon a  properly 
 * configured XML-RPC listener at the endpoint configured in XmlRpcLabel.
 * @see XmlRpcLabel 
 * @author Nathan Hamblen
 */
public class RubyPantsLabel extends XmlRpcLabel {

	/**
	 * @param Wicket id
	 */
	public RubyPantsLabel(String id) {
		super(id, new RubyPantsConverter());
		setEscapeModelStrings(false);
	}
/**
 * @param Wicket id
 * @param String model
 */
	public RubyPantsLabel(String id, IModel model) {
		super(id, model, new RubyPantsConverter());
		setEscapeModelStrings(false);
	}
	/** XML-RPC method name is "rubypants.to_html". */
	public static class RubyPantsConverter extends XmlRpcConverter {
		@Override
		protected String getMethodName() {
			return "rubypants.to_html";
		}
	}
}
