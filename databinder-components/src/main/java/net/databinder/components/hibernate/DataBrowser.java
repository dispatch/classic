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

package net.databinder.components.hibernate;

import java.util.ArrayList;

import net.databinder.DataStaticService;
import net.databinder.IDataApplication;
import net.databinder.components.DataStyleLink;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.pages.AccessDeniedPage;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Page containing a QueryPanel for browsing data and testing Hibernate queries. DataApplication
 * mounts this page to /dbrowse by default for development (when IDataApplication.isDataBrowserAllowed() 
 * returns true).  For security, this page can not be constructed when isDataBrowserAllowed()
 * returns false.
 * @see IDataApplication
 * @author Nathan Hamblen
 */
public class DataBrowser extends WebPage {
	public DataBrowser() {
		Application app = getApplication();
		if (app instanceof IDataApplication && ((IDataApplication)app).isDataBrowserAllowed()) {
			add(new DataStyleLink("css"));
			add(new QueryPanel("queryPanel"));
			add(new ListView("entities", new LoadableDetachableModel() {
				@SuppressWarnings("unchecked")
				@Override
				protected Object load() {
					return new ArrayList(
							DataStaticService.getHibernateSessionFactory().getAllClassMetadata().keySet());
				}
			}) {
				@Override
				protected void populateItem(ListItem item) {
					item.add(new Label("name", item.getModel()));
				}
			});
					
		} else setResponsePage(AccessDeniedPage.class);
	}
}