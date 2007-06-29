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

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.resources.JavaScriptReference;

/**
 * Component for a script (JavaScript) link. The stylesheet is expected to be named
 * &lt;ClassName&gt;.js for the class specified in the constructor and be located in 
 * the same package as that class.
 * @author Nathan Hamblen
 */
public class ScriptLink extends JavaScriptReference {
	private static String EXT = ".js";
	
	/** Builds a ScriptLink based on the given class. */
	public ScriptLink(String id, Class componentClass) {
		super(id, componentClass, componentClass.getSimpleName() + EXT);
	}
	
	/**
	 * Get a [classname].js header contriubtor that can be added to a component without any reference in
	 * its markup. Useful for subclasses that do not have their own templates.
	 * @param componentClass javascript file should be in same package and have same base name
	 * @return contributor to add to component
	 */
	public static HeaderContributor headerContributor(Class componentClass) {
		return HeaderContributor.forJavaScript(
				componentClass, componentClass.getSimpleName() + EXT);
	}
}
