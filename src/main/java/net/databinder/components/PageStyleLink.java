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

import wicket.model.IModel;

/**
 * Please switch to StyleLink. The name is changing since the class is just as useful
 * for Panels, etc.
 * @see StyleLink
 * @author Nathan Hamblen
 */
@Deprecated
public class PageStyleLink extends StyleLink {
	
	public PageStyleLink(String id, Class pageClass) {
		super(id, pageClass);
	}
	
	public PageStyleLink(String id) {
		super(id);
	}
	
	public PageStyleLink(String id, IModel model) {
		super(id, model);
	}
}
