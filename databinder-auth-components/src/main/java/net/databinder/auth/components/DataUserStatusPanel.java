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

import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.components.DataSignInPage.LazyPage;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Displays sign in and out links, as well as current user if any.
 */
public class DataUserStatusPanel extends Panel {
	/**
	 * Constructs sign in and out links.
	 * @param id Wicket id
	 */
	public DataUserStatusPanel(String id) {
		super(id);

		WebMarkupContainer wrapper = new WebMarkupContainer("signedInWrapper") {
			public boolean isVisible() {
				return getAuthSession().isSignedIn();
			}
		};
		add(wrapper);
		wrapper.add(new Label("username", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return getAuthSession().getUser().toString();
			}
		}));
		wrapper.add(new Link("profile") {
			@Override
			public void onClick() {
				setResponsePage(getProfilePage());
			}
		});

		wrapper.add(new Link("signOut") {
			@Override
			public void onClick() {
				getAuthSession().signOut();
				setResponsePage(getApplication().getHomePage());
			}
		});

		add(getSignInLink("signIn"));
	}
	
	WebPage getProfilePage() {
		return new DataProfilePage(new LazyPage() {
			public Page getPage() {
				return DataUserStatusPanel.this.getPage();
			}
		});
	}

	/**
	 * Returns link to sign-in page from <tt>AuthDataApplication</tt> subclass. Uses redirect
	 * to intercept page so that user will return to current page once signed in. Override
	 * for other behavior.
	 */
	protected Link getSignInLink(String id) {
		return new Link(id) {
			@Override
			public void onClick() {
				redirectToInterceptPage(getPageFactory().newPage(
						((IAuthSettings)getApplication()).getSignInPageClass()));
			}
			@Override
			public boolean isVisible() {
				return !getAuthSession().isSignedIn();
			}
		};
	}

	/** @return casted web session*/
	protected IAuthSession getAuthSession() {
		return (IAuthSession) getSession();
	}
}
