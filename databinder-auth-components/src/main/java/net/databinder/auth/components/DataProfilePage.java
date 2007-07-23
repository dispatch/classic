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
package net.databinder.auth.components;

import net.databinder.components.DataStyleLink;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;

/**
 * Display profile editing panel for logged in user. 
 */
public class DataProfilePage extends WebPage {
	public DataProfilePage(Page returnPage) {
		add(new DataStyleLink("dataStylesheet"));
		add(profileSocket("profileSocket", returnPage));
	}
	protected Component profileSocket(String id, Page returnPage) {
		return new DataProfilePanel(id, returnPage);
	}
}
