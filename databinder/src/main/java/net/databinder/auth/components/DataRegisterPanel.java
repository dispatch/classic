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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.databinder.DataStaticService;
import net.databinder.auth.AuthDataSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.data.DataUser;
import net.databinder.auth.data.IUser;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import wicket.PageParameters;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.form.validation.EqualPasswordInputValidator;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Panel;
import wicket.model.CompoundPropertyModel;
import wicket.model.Model;
import wicket.validation.IValidatable;
import wicket.validation.validator.StringValidator;

/**
 * Registration with username, password, and password confirmation.
 * @author Nathan Hamblen
 * @deprecated
 */
public class DataRegisterPanel extends Panel {

	public DataRegisterPanel(String id) {
		super(id);
		add(new FeedbackPanel("feedback"));
		add(new RegisterForm("registerForm"));
	}

	protected class RegisterForm extends Form {
		private PasswordTextField password, passwordConfirm;

		public RegisterForm(String id) {
			super(id, new CompoundPropertyModel(new Credentials()));
			add(new RequiredTextField("username").add(new StringValidator(){
				@Override
				protected void onValidate(IValidatable validatable) {
					String username = (String)validatable.getValue();
					if (username == null || !isAvailable(username)) {
						Map<String, String> m = new HashMap<String, String>(1);
						m.put("username", username);
						error(validatable,"taken",  m);
					}
				}
			}));
			add(password = new PasswordTextField("password"));
			add(passwordConfirm = new PasswordTextField("passwordConfirm", new Model("")));
			add(new EqualPasswordInputValidator(password, passwordConfirm));
		}

		@Override
		protected void onSubmit() {
			Credentials creds = (Credentials) getModelObject();

			IUser user = getNewUser(creds);
			Session session = DataStaticService.getHibernateSession();
			session.save(user);
			session.getTransaction().commit();

			((AuthDataSession)wicket.Session.get()).signIn(creds.getUsername(), creds.getPassword());

			if (!continueToOriginalDestination())
			{
				setResponsePage(getApplication().getSessionSettings().getPageFactory().newPage(
						getApplication().getHomePage(), (PageParameters)null));
			}
		}
	}

	/** @return new user object with given credentials */
	protected IUser getNewUser(Credentials creds) {
		return new DataUser(creds.getUsername(), creds.getPassword());
	}

	/** @return true if the given username has not been taken */
	protected boolean isAvailable(String username) {
		Session session = DataStaticService.getHibernateSession();
		Criteria c = session.createCriteria(((IAuthSettings)getApplication()).getUserClass());
		c.add(Property.forName("username").eq(username));
		c.setProjection(Projections.rowCount());
		return c.uniqueResult().equals(0);
	}

	protected static class Credentials implements Serializable {
		private String username;
		private String password;
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
	}
}
