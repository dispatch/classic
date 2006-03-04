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
 * Component for a script (JavaScript) link. The stylesheet is expected to be named
 * &lt;ClassName&gt;.js for the class specified in the constructor or via a
 * compound model, and be located in the same package as that class.
 * @author Nathan Hamblen
 */
public class ScriptLink extends WebMarkupContainer {
	
	/** Builds a ScriptLink based on the given class. */
	public ScriptLink(String id, Class pageClass) {
		this(id, new Model(pageClass));
	}
	
	/** Builds a ScriptLink from a compound property model. */
	public ScriptLink(String id) {
		super(id);
	}
	
	/** Builds a ScriptLink based on a class in an IModel. */
	public ScriptLink(String id, IModel model) {
		super(id, model);
	}
	
	/** Ensures that this is a "script" HTML element. */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "script");
		super.onComponentTagBody(markupStream, openTag);
	}
	
	/** Sets appropriate src value for the script. */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		Class pageClass = (Class) getModelObject();
		tag.put("src", getPage().urlFor(new PackageResourceReference(pageClass, 
				pageClass.getSimpleName() + ".js").getPath()));
		super.onComponentTag(tag);
	}
}
