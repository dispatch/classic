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

import wicket.AttributeModifier;
import wicket.Component;
import wicket.markup.html.list.ListItem;
import wicket.model.Model;

/**
 * Alternates between class attribute values of "a" and "b", or user selected
 * values, for even and odd ListItem components.
 * @author Nathan Hamblen
 */
public class AlternatingClassModifier extends AttributeModifier {
	/**
	 * Constructs AttributeModifier for "class" with values "a" or "b".
	 * @param item Object whose index determines class value.
	 */
	public AlternatingClassModifier(final ListItem item) {
		this(item, "a", "b");
	}
	
	/**
	 * Constructs AttributeModifier for "class" with the given alternating class values.
	 * @param item Object whose index determines class value.
	 */
	public AlternatingClassModifier(final ListItem item, final String classA, final String classB) {
		super("class", new Model() {
			@Override
			public Object getObject(Component component) {
				return item.getIndex() % 2 == 0 ? classA : classB;
			}
		});
	}
}
