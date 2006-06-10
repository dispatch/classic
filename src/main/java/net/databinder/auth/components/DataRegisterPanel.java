package net.databinder.auth.components;

import java.util.HashMap;
import java.util.Map;

import net.databinder.DataRequestCycle;
import net.databinder.auth.AuthDataSession;
import net.databinder.auth.data.IUser;
import net.databinder.auth.data.User;

import org.hibernate.Query;
import org.hibernate.Session;

import wicket.PageParameters;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.FormComponent;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.form.validation.EqualInputValidator;
import wicket.markup.html.form.validation.StringValidator;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Panel;
import wicket.model.CompoundPropertyModel;
import wicket.model.Model;

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
				public void onValidate(FormComponent formComponent, String username) {
					if (username == null || !isAvailable(username)) {
						Map<String, String> m = new HashMap<String, String>(1);
						m.put("username", username);
						error(formComponent, "username.unavailable", m);
					}
				}
			}));
			add(password = new PasswordTextField("password"));
			add(passwordConfirm = new PasswordTextField("passwordConfirm", new Model("")));
			add(new EqualInputValidator(password, passwordConfirm));
		}
		
		@Override
		protected void onSubmit() {
			Credentials creds = (Credentials) getModelObject();

			IUser user = getNewUser(creds);
			Session session = DataRequestCycle.getHibernateSession();
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
	
	protected IUser getNewUser(Credentials creds) {
		return new User(creds.getUsername(), creds.getPassword());
	}
	
	protected boolean isAvailable(String username) {
		Session session = DataRequestCycle.getHibernateSession();
		Query q = session.createQuery("from User where username = ?")
			.setString(0, username);
		return q.list().isEmpty();
	}
	
	protected static class Credentials {
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
