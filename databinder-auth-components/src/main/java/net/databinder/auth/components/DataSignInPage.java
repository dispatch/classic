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

import net.databinder.auth.IAuthSettings;
import net.databinder.auth.components.DataRegisterPanel.Credentials;
import net.databinder.auth.data.IUser;
import net.databinder.components.StyleLink;

import org.apache.wicket.Application;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Serves as both a sign in and simple regristration page. Please use a differnt sign in page;
 * override AuthDataApplication's getSignInPageClass() method. An updated version of
 * this page and its panels are available in the auth-data-app Maven archetype.
 * @author Nathan Hamblen
 */
public abstract class DataSignInPage extends WebPage {
	/** state of page, sign in or registration */
	private boolean register = false;
	
	/** Registration panel whose visibility is controlled from this class. */
	private Panel registerPanel;

	/** Sign-in panel whose visibility is controlled from this class. */
	private Panel signInPanel;

	/**
	 * Displays sign in page. Checks that the page being instantiated is of the type returned
	 * by AuthDataApplication.getSignInPageClass().
	 */
	public DataSignInPage() {
		init();
	}
	public DataSignInPage(boolean register) {
		this.register = register;
		init();
	}
	void init() {
		// make sure nothing funny is going on, since this page has a default
		// constructor and is bookmarkable
		if (!((IAuthSettings)Application.get()).getSignInPageClass().equals(getClass()))
			throw new WicketRuntimeException("The sign in page requested does not match that defined in the AuthDataApplication subclass.");
		
		add(new StyleLink("signinStylesheet", DataSignInPage.class));
		
		add(new WebMarkupContainer("gotoRegister"){
			@Override
			public boolean isVisible() {
				return !register;
			}
		}.add(new Link("register") {
			@Override
			public void onClick() {
				setRegister(true);
			}
		}));
		
		add(new WebMarkupContainer("gotoSignIn"){
			@Override
			public boolean isVisible() {
				return register;
			}
		}.add(new Link("signIn") {
			@Override
			public void onClick() {
				setRegister(false);
			}
		}));
		
		add(signInPanel = getSignInPanel("signInPanel"));
		add(registerPanel = getRegisterPanel("registerPanel"));
		setRegister(register);
	}
	
	/**
	 * Override to use subclass of DataSignInPanel or some other panel.
	 * @param id use this id for your registration panel
	 * @return panel that appears for signing in 
	 */
	protected Panel getSignInPanel(String id) {
		return new DataSignInPanel(id);
	}
	
	/**
	 * Override to use subclass of DataRegisterPanel or some other panel.
	 * @param id use this id for your registration panel
	 * @return panel that appears after user clicks registration link 
	 */
	protected Panel getRegisterPanel(String id) {
		return new DataRegisterPanel(id) {
			@Override
			protected IUser getNewUser(Credentials creds) {
				return DataSignInPage.this.getNewUser(creds);
			}
		};
	}

	protected abstract IUser getNewUser(Credentials creds);
	
	/**
	 * @return true if displaying registration page
	 */
	protected boolean isRegister() {
		return register;
	}
	
	/**
	 * @param register true to display the registration version of this page
	 */
	protected void setRegister(boolean register) {
		this.register = register;
		registerPanel.setVisible(register);
		signInPanel.setVisible(!register);
	}
}
