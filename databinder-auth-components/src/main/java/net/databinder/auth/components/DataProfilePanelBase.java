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

import java.util.HashMap;
import java.util.Map;

import net.databinder.auth.AuthSession;
import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.auth.data.DataUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.NullPlug;
import net.databinder.models.BindingModel;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;

/**
 * Registration with username, password, and password confirmation.
 * Replaceable String resources: <pre>
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.remember
 * data.auth.register
 * data.auth.update
 * data.auth.username.taken * </pre> * Must be overriden in a containing page
 * or a subclass of this panel.
 */
public abstract class DataProfilePanelBase extends Panel {
	private ReturnPage returnPage;
	private Form form;
	private RequiredTextField username;
	private RSAPasswordTextField password, passwordConfirm;
	private CheckBox rememberMe;

	public DataProfilePanelBase(String id, ReturnPage returnPage) {
		super(id);
		this.returnPage = returnPage;
		add(new FeedbackPanel("feedback"));
		add(form = getProfileForm("registerForm", DataSignInPageBase.getAuthSession().getUserModel()));
		form.add(new Profile("profile"));
	}
	
	protected abstract Form getProfileForm(String id, IModel userModel);
	
	DataUser getUser() {
		return (DataUser) form.getModelObject();
	}

	protected boolean existing() {
		BindingModel model = ((BindingModel)((IChainingModel)form.getModel()).getChainedModel());
		return model != null && model.isBound();
	}

	protected class Profile extends WebMarkupContainer {
		
		public Profile(String id) {
			super(id);
			add(highFormSocket("highFormSocket"));
			add(username = new RequiredTextField("username"));
			username.add(new UsernameValidator());
			username.setLabel(new ResourceModel("data.auth.username", "Username"));
			add(new SimpleFormComponentLabel("username-label", username));
			add(password = new RSAPasswordTextField("password", form) {
				public boolean isRequired() {
					return !existing();
				}
			});
			password.setLabel(new ResourceModel("data.auth.password", "Password"));
			add(new SimpleFormComponentLabel("password-label", password));
			add(passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), form) {
				public boolean isRequired() {
					return !existing();
				}
			});
			form.add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
			passwordConfirm.setLabel(new ResourceModel("data.auth.passwordConfirm", "Retype Password"));
			add(new SimpleFormComponentLabel("passwordConfirm-label", passwordConfirm));
			
			add(new WebMarkupContainer("rememberMeRow") { 
				public boolean isVisible() {
					return !existing();
				}
			}.add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE))));
			
			add(lowFormSocket("lowFormSocket"));
			
			add(new Button("submit").add(new AttributeModifier("value", new AbstractReadOnlyModel() {
				public Object getObject() {
					return existing() ? getString("auth.data.update", null, "Update Account") : 
						getString("data.auth.register", null, "Register");
				}
			})));
		}
	}
	
	protected void afterSubmit() {
		DataSignInPageBase.getAuthSession().signIn(getUser(), (Boolean) rememberMe.getModelObject());

		if (returnPage == null) {
			if (!continueToOriginalDestination())
				setResponsePage(getApplication().getHomePage());
		} else
			setResponsePage(returnPage.get());
	}

	public static boolean isAvailable(String username) {
		AuthApplication authSettings = (AuthApplication)Application.get();
		
		DataUser found = (DataUser) authSettings.getUser(username), 
			current = ((AuthSession)WebSession.get()).getUser();
		return found == null || found.equals(current);
	}
	
	public static class UsernameValidator extends StringValidator {
		@Override
		protected void onValidate(IValidatable validatable) {
			String username = (String) validatable.getValue();
			if (username != null && !isAvailable(username)) {
				Map<String, String> m = new HashMap<String, String>(1);
				m.put("username", username);
				error(validatable,"data.auth.username.taken",  m);
			}
		}
	}
	
	protected Component highFormSocket(String id) {
		return new NullPlug(id);
	}

	protected Component lowFormSocket(String id) {
		return new NullPlug(id);
	}

	public Form form() {
		return form;
	}

}
