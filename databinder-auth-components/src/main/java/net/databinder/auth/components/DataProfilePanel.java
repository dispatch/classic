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

import net.databinder.DataStaticService;
import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.components.DataSignInPage.ReturnPage;
import net.databinder.auth.data.IUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.NullPlug;
import net.databinder.components.hibernate.DataForm;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.hibernate.Criteria;
import org.hibernate.Session;

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
public class DataProfilePanel extends Panel {
	private ReturnPage returnPage;
	private ProfileForm form;

	public DataProfilePanel(String id, ReturnPage returnPage) {
		super(id);
		this.returnPage = returnPage;
		HibernateObjectModel userModel = DataSignInPage.getAuthSession().getUserModel();
		if (userModel == null) 
			userModel = new HibernateObjectModel(((IAuthSettings)getApplication()).getUserClass());
		add(form = new ProfileForm("registerForm", userModel));
	}
	
	protected class ProfileForm extends DataForm {
		private RequiredTextField username;
		private RSAPasswordTextField password, passwordConfirm;
		private CheckBox rememberMe;
		
		protected RequiredTextField getUsername() { return username; }
		protected RSAPasswordTextField getPassword() { return password; }
		protected RSAPasswordTextField getPasswordConfirm() { return passwordConfirm; }
		protected CheckBox getRememberMe() { return rememberMe; }
		
		IUser getUser() {
			return (IUser) getPersistentObjectModel().getObject();
		}
		
		boolean existing() {
			return DataStaticService.getHibernateSession().contains(getUser());
		}
		
		public ProfileForm(String id, HibernateObjectModel userModel) {
			super(id, userModel);
			add(highFormSocket("highFormSocket"));
			add(feedbackBorder("username-border")
					.add(username = new RequiredTextField("username")));
			username.add(new UsernameValidator());
			username.setLabel(new ResourceModel("data.auth.username", "Username"));
			add(new SimpleFormComponentLabel("username-label", username));
			add(feedbackBorder("password-border")
					.add(password = new RSAPasswordTextField("password", this) {
				public boolean isRequired() {
					return !existing();
				}
			}));
			password.setLabel(new ResourceModel("data.auth.password", "Password"));
			add(new SimpleFormComponentLabel("password-label", password));
			add(feedbackBorder("passwordConfirm-border")
					.add(passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), this) {
				public boolean isRequired() {
					return !existing();
				}
			}));
			add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
			passwordConfirm.setLabel(new ResourceModel("data.auth.passwordConfirm", "Retype Password"));
			add(new SimpleFormComponentLabel("passwordConfirm-label", passwordConfirm));
			
			add(new WebMarkupContainer("rememberMeRow") { 
				public boolean isVisible() {
					return !existing();
				}
			}.add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE))));
			
			add(lowFormSocket("lowFormSocket"));
			
			add(new WebMarkupContainer("submit").add(new AttributeModifier("value", new AbstractReadOnlyModel() {
				public Object getObject() {
					return existing() ? getString("auth.data.update", null, "Update Account") : getString("data.auth.register", null, "Register");
				}
			})));
		}

		@Override
		protected void onSubmit() {
			super.onSubmit();
			
			DataSignInPage.getAuthSession().signIn(getUser(), (Boolean) rememberMe.getModelObject());

			if (returnPage == null) {
				if (!continueToOriginalDestination())
					setResponsePage(getApplication().getHomePage());
			} else
				setResponsePage(returnPage.get());
		}
	}

	public static boolean isAvailable(String username) {
		Session session = DataStaticService.getHibernateSession();
		IAuthSettings authSettings = (IAuthSettings)Application.get();
		Criteria c = session.createCriteria(authSettings.getUserClass());
		authSettings.getUserCriteriaBuilder(username).build(c);
		IUser found = (IUser) c.uniqueResult(), 
			current = ((IAuthSession)WebSession.get()).getUser();
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
		return new FeedbackPanel(id)
			.add(new AttributeModifier("class", true, new Model("feedback")));
	}

	protected Component lowFormSocket(String id) {
		return new NullPlug(id);
	}

	protected Border feedbackBorder(String id) {
		return new FormComponentFeedbackBorder(id);
	}

	public ProfileForm getForm() {
		return form;
	}

}
