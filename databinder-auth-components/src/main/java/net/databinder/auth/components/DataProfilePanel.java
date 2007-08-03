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
import net.databinder.auth.components.DataSignInPage.LazyPage;
import net.databinder.auth.data.IUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.hibernate.DataForm;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.hibernate.Criteria;
import org.hibernate.Session;

/**
 * Registration with username, password, and password confirmation.
 */
public class DataProfilePanel extends Panel {
	private LazyPage returnPage;

	public DataProfilePanel(String id, LazyPage returnPage) {
		this(id);
		this.returnPage = returnPage;
	}
	public DataProfilePanel(String id) {
		super(id);
		add(new FeedbackPanel("feedback"));
		HibernateObjectModel userModel = DataSignInPage.getAuthSession().getUserModel();
		if (userModel == null) 
			userModel = new HibernateObjectModel(((IAuthSettings)getApplication()).getUserClass());
		add(new RegisterForm("registerForm", userModel));
	}
	
	protected class RegisterForm extends DataForm {
		private RSAPasswordTextField password, passwordConfirm;
		private CheckBox rememberMe;
		
		IUser getUser() {
			return (IUser) getPersistentObjectModel().getObject();
		}
		
		boolean existing() {
			return DataStaticService.getHibernateSession().contains(getUser());
		}
		
		public RegisterForm(String id, HibernateObjectModel userModel) {
			super(id, userModel);
			add(new RequiredTextField("username").add(new UsernameValidator()));
			add(password = new RSAPasswordTextField("password", this) {
				public boolean isRequired() {
					return !existing();
				}
			});
			add(passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), this) {
				public boolean isRequired() {
					return !existing();
				}
			});
			add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
			
			add(new WebMarkupContainer("rememberMeRow") { 
				public boolean isVisible() {
					return !existing();
				}
			}.add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE))));
			
			add(new WebMarkupContainer("submit").add(new AttributeModifier("value", new AbstractReadOnlyModel() {
				public Object getObject() {
					return existing() ? "Update Profile" : "Register";
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
				setResponsePage(returnPage.getPage());
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
				error(validatable,"taken",  m);
			}
		}
	}

}
