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

import net.databinder.auth.AuthSession;
import net.databinder.auth.AuthApplication;
import net.databinder.auth.data.DataUser;
import net.databinder.components.DataStyleLink;
import net.databinder.components.SourceList;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

/**
 * Sign in and registration page.
 * Replaceable String resources: <pre>
 * data.auth.title.sign_in
 * data.auth.pre_register_link
 * data.auth.register_link
 * data.auth.pre_sign_in_link
 * data.auth.sign_in_link
 * or a subclass of this panel.
 */
public abstract class DataSignInPageBase extends WebPage {
	private SourceList sourceList;
	
	private Component profileSocket, signinSocket;
	
	private SourceList.SourceLink profileLink, signinLink;
	
	public interface ReturnPage extends IClusterable {
		Page get();
	}
	/**
	 * Displays sign in page.
	 */
	public DataSignInPageBase(PageParameters params) {
		this(params, null);
	}
	
	public DataSignInPageBase(ReturnPage returnPage) {
		this(null, returnPage);
	}
	
	public DataSignInPageBase(PageParameters params, ReturnPage returnPage) {
		AuthApplication app = null;
		try { app = ((AuthApplication)Application.get()); } catch (ClassCastException e) { }
		// make sure the user is not trying to sign in or register with the wrong page
		if (app == null || !app.getSignInPageClass().isInstance(this))
			throw new UnauthorizedInstantiationException(DataSignInPageBase.class);

		if (params != null) {
			String username = params.getString("username");
			String token = params.getString("token");
			// e-mail auth, for example
			if (username != null && token != null) {
				DataUser.CookieAuth user = (DataUser.CookieAuth) app.getUser(username);
				
				if (user != null && app.getToken(user).equals(token))
					getAuthSession().signIn(user, true);
				setResponsePage(((Application)app).getHomePage());
				setRedirect(true);
				return;
			}
		}
		
		add(new Label("title", new ResourceModel("data.auth.title.sign_in", "Please sign in")));

		add(new DataStyleLink("dataStylesheet"));
		
		sourceList = new SourceList();
		
		add(profileSocket = profileSocket("profileSocket", returnPage));
		add(new WebMarkupContainer("profileLinkWrapper") {
			public boolean isVisible() {
				return profileLink.isEnabled();
			}
		}.add(profileLink = sourceList.new SourceLink("profileLink", profileSocket)));
		
		add(signinSocket = signinSocket("signinSocket", returnPage));
		add(new WebMarkupContainer("signinLinkWrapper") {
			@Override
			public boolean isVisible() {
				return signinLink.isEnabled();
			}
		}.add(signinLink = sourceList.new SourceLink("signinLink", signinSocket)));
		signinLink.onClick();	// show sign in first
	}
	
	/**
	 * Default returns DataSignInPanel.
	 * @return component (usually panel) to display for sign in
	 * @see DataSignInPanel
	 */
	protected Component signinSocket(String id, ReturnPage returnPage) {
		return new DataSignInPanel(id, returnPage);
	}

	/**
	 * Default returns DataProfilePanel.
	 * @return component to display for profile / registration
	 */
	protected abstract Component profileSocket(String id, ReturnPage returnPage);
	
	protected static  AuthSession getAuthSession() {
		return (AuthSession) Session.get();
	}
}
