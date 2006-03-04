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
import wicket.markup.html.PackageResourceReference;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;
import wicket.model.Model;

/**
 * Component for a stylesheet link. The stylesheet is expected to be named
 * &lt;ClassName&gt;.css for the class specified in the constructor or via a
 * compound model, and be located in the same package as that class.
 * @author Nathan Hamblen
 */
public class StyleLink extends WebMarkupContainer {
	
	/** Builds a StyleLinkbased on the given class. */
	public StyleLink(String id, Class pageClass) {
		this(id, new Model(pageClass));
	}
	
	/** Builds a StyleLink from a compound property model. */
	public StyleLink(String id) {
		super(id);
	}
	
	/** Builds a StyleLink based on a class in an IModel. */
	public StyleLink(String id, IModel model) {
		super(id, model);
	}
	
	/** Ensures that this is a "link" HTML element. */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "link");
		super.onComponentTagBody(markupStream, openTag);
	}
	
	/** Sets appropriate href, type, and rel values for the stylesheet. */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		Class pageClass = (Class) getModelObject();
		tag.put("href", getPage().urlFor(new PackageResourceReference(pageClass, 
				pageClass.getSimpleName() + ".css").getPath()));
		// ensure valid css attributes
		tag.put("type", "text/css");
		tag.put("rel", "stylesheet");
		super.onComponentTag(tag);
	}
	
}
