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

import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * Extention of WebMarkupContainer that outputs a markup ID by default. This
 * is handy for wrapping ajax targets with minimal fuss.
 * @author Nathan Hamblen
 */
public class Wrapper extends WebMarkupContainer {
	/** 
	 * Creates a wrapping component with a markup ID, normally used as an
	 * ajax target.
	 * @param id Wicket ID
	 */
	public Wrapper(String id) {
		super(id);
		setOutputMarkupId(true);
	}
}
