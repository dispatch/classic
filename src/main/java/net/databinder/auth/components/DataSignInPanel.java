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

import net.databinder.auth.AuthDataSession;
import wicket.PageParameters;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.CheckBox;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.form.TextField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;

/**
 * Displays username and password fields, along with optional "remember me" checkbox.
 * Queries the AuthDataSession upon a login attempt.
 * @see AuthDataSession
 */
public class DataSignInPanel extends Panel {

	public DataSignInPanel(String id) {
		super(id);
		add(new FeedbackPanel("feedback"));
		add(new SignInForm("signInForm"));
	}
	
	protected class SignInForm extends Form {
		private CheckBox rememberMe;
		private TextField username, password;
		protected SignInForm(String id) {
			super(id);
			add(username = new RequiredTextField("username", new Model(null)));
			add(password = new PasswordTextField("password", new Model(null)));
			add(new WebMarkupContainer("rememberMeRow") {
				@Override
				public boolean isVisible() {
					return includeRememberMe();
				}
			}.add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE))));
		}
		@Override
		protected void onSubmit() {
			if (signIn((String)username.getModelObject(), (String)password.getModelObject(), 
					(Boolean)rememberMe.getModelObject() && includeRememberMe()))
			{
				if (!continueToOriginalDestination())
					setResponsePage(getApplication().getSessionSettings().getPageFactory().newPage(
							getApplication().getHomePage(), (PageParameters)null));
			} else
				error(getLocalizer().getString("signInFailed", this, "Sorry, name and password not recognized."));
		}
	}

	/**
	 * Returns true by default. Override to disable "remember me" functionality.
	 * @return true to include remember me option
	 */
	protected boolean includeRememberMe() {
		return false;
	}
	
	/**
	 * Call sign in method for session. Override to call a different sign in method.
	 * @return true if credentials allowed sign-in
	 * @see AuthDataSession 
	 */
	protected boolean signIn(String username, String password, boolean setCookie) {
		return ((AuthDataSession) getSession()).signIn(username, password, setCookie);
	}
}
