/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2008  Nathan Hamblen nathan@technically.us

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
import org.apache.wicket.model.IModel;

/**
 * Extention of WebMarkupContainer that outputs a markup ID and placeholder tag
 * if invisible by default. Handy for targets of ajax event handlers.
 * @author Nathan Hamblen
 *
 */
public class AjaxCell extends WebMarkupContainer {
	public AjaxCell(String id, IModel model) {
		super(id, model);
		setOutputMarkupId(true);
		setOutputMarkupPlaceholderTag(true);
	}
	public AjaxCell(String id) {
		this(id, null);
	}
}
