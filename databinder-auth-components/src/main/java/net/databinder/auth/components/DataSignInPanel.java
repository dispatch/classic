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
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * Displays username and password fields, along with optional "remember me" checkbox.
 * Queries the IAuthSession upon a login attempt. Replaceable String resources: <pre>
 * data.auth.username
 * data.auth.password
 * data.auth.remember
 * data.auth.sign_in</pre>
 * @see AuthSession
 */
public class DataSignInPanel extends Panel {
	private ReturnPage returnPage;
	public DataSignInPanel(String id, ReturnPage returnPage) {
		super(id);
		this.returnPage = returnPage;
		add(new FeedbackPanel("feedback"));
		add(new SignInForm("signInForm"));
	}
	
	protected class SignInForm extends Form {
		private CheckBox rememberMe;
		private TextField username, password;
		protected SignInForm(String id) {
			super(id);
			add(username = new RequiredTextField("username", new Model()));
			username.setLabel(new ResourceModel("data.auth.username", "Username"));
			add(new SimpleFormComponentLabel("username-label", username));
			add(password = new RSAPasswordTextField("password", new Model(), this));
			password.setRequired(true);
			password.setLabel(new ResourceModel("data.auth.password", "Password"));
			add(new SimpleFormComponentLabel("password-label", password));
			add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
		}
		@Override
		protected void onSubmit() {
			if (DataSignInPageBase.getAuthSession().signIn((String)username.getModelObject(), (String)password.getModelObject(), 
					(Boolean)rememberMe.getModelObject()))
			{
				if (returnPage == null) {
					if (!continueToOriginalDestination())
						setResponsePage(getApplication().getHomePage());
				} else
					setResponsePage(returnPage.get());
			} else
				error(getLocalizer().getString("signInFailed", this, "Sorry, these credentials are not recognized."));
		}
	}
}
