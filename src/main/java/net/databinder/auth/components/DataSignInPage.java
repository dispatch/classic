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

import net.databinder.auth.AuthDataApplication;
import net.databinder.components.DataPage;
import net.databinder.components.StyleLink;
import wicket.Application;
import wicket.WicketRuntimeException;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.WebPage;
import wicket.markup.html.link.Link;

/**
 * Serves as both a sign in and simple regristration page. To use a differnt sign in page,
 * override AuthDataApplication's getSignInPageClass() method.
 * @author Nathan Hamblen
 */
public class DataSignInPage extends WebPage {
	/** state of page, sign in or registration */
	private boolean isRegister = false;

	/**
	 * Displays sign in page. Checks that the page being instantiated is of the type returned
	 * by AuthDataApplication.getSignInPageClass().
	 */
	public DataSignInPage() {
		// make sure nothing funny is going on, since this page has a default
		// constructor and is bookmarkable
		if (!((AuthDataApplication)Application.get()).getSignInPageClass().equals(getClass()))
			throw new WicketRuntimeException("The sign in page requested does not match that defined in the AuthDataApplication subclass.");
		
		add(new StyleLink("dataStylesheet", DataPage.class));
		add(new StyleLink("signinStylesheet", DataSignInPage.class));
		
		add(new WebMarkupContainer("gotoRegister"){
			@Override
			public boolean isVisible() {
				return !isRegister;
			}
		}.add(new Link("register") {
			@Override
			public void onClick() {
				isRegister = true;
			}
		}));
		
		add(new WebMarkupContainer("gotoSignIn"){
			@Override
			public boolean isVisible() {
				return isRegister;
			}
		}.add(new Link("signIn") {
			@Override
			public void onClick() {
				isRegister = false;
			}
		}));
		
		add(new DataSignInPanel("signInPanel") {
			@Override
			public boolean isVisible() {
				return !isRegister;
			}
		});
		
		// TODO: allow subclass here ?
		add(new DataRegisterPanel("registerPanel") {
			@Override
			public boolean isVisible() {
				return isRegister;
			}
		});
	}

}