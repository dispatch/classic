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
 * Converts its model via the Ruby <a href="http://www.deveiate.org/projects/BlueCloth">BlueCloth</a>
 * <a href="http://daringfireball.net/projects/markdown/">Markdown</a> processing library before 
 * display. This component depends upon a  properly configured XML-RPC listener at the endpoint 
 * configured in XmlRpcLabel.
 * @see XmlRpcLabel 
 * @author Nathan Hamblen
 */
public class BlueClothLabel extends XmlRpcLabel {

	/**
	 * @param Wicket id
	 */
	public BlueClothLabel(String id) {
		super(id, new BlueClothConverter());
		setEscapeModelStrings(false);
	}
/**
 * @param Wicket id
 * @param String model
 */
	public BlueClothLabel(String id, IModel model) {
		super(id, model, new BlueClothConverter());
		setEscapeModelStrings(false);
	}
	/** XML-RPC method name is "bluecloth.to_html". */
	protected static class BlueClothConverter extends XmlRpcConverter {
		@Override
		protected String getMethodName() {
			return "bluecloth.to_html";
		}
	}
	
	/**
	 * Applies SmartyPants conversion after Markdown conversion, for curved quotes,
	 * em dashes, etc.
	 * @see RubyPantsLabel
	 */
	public static class Smarty extends XmlRpcLabel {
		/**
		 * @param Wicket id
		 */
		public Smarty(String id) {
			super(id, new SmartyConverter());
			setEscapeModelStrings(false);
		}
	/**
	 * @param Wicket id
	 * @param String model
	 */
		public Smarty(String id, IModel model) {
			super(id, model, new SmartyConverter());
			setEscapeModelStrings(false);
		}
		/** XML-RPC method name is "bluecloth.rubypants.to_html". */
		protected static class SmartyConverter extends XmlRpcConverter {
			@Override
			protected String getMethodName() {
				return "bluecloth.rubypants.to_html";
			}
		}
	}
}
