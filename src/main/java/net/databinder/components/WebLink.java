/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

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
package net.databinder.components;

import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;
import wicket.util.string.Strings;

/**
 * Component for an anchor linking outside of Wicket. Similar to 
 * wicket.markup.html.link.ExternaLink, but uses an IModel for the href attribute
 * value and will not replace its body. (Use a span with wicket.markup.html.basic.Label 
 * for variable link body text.)
 * @author Nathan Hamblen
 */
public class WebLink extends WebMarkupContainer {
	
	/**
	 * Initialize with a compound model.
	 */
	public WebLink(String id) {
		super(id);
	}
	
	/**
	 * Initialize with a specific model.
	 */
	public WebLink(String id, IModel model) {
		super(id, model);
	}
	
	/**
	 *  Sets the link's href to this component's model value, changing any ampersands
	 *  to the escaped form.
	 */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		tag.put("href", Strings.replaceAll(getModelObjectAsString(), "&", "&amp;"));
		super.onComponentTag(tag);
	}
	
	/**
	 * Ensures that is component is mapped to an "a" tag.
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "a");
		super.onComponentTagBody(markupStream, openTag);
	}
}
