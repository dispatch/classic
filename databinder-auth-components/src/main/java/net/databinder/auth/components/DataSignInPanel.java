package net.databinder.auth.components;

import net.databinder.auth.IAuthSession;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

/**
 * Displays username and password fields, along with optional "remember me" checkbox.
 * Queries the IAuthSession upon a login attempt.
 * @see IAuthSession
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
			add(username = new RequiredTextField("username", new Model()));
			add(password = new RSAPasswordTextField("password", new Model(), this));
			add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
		}
		@Override
		protected void onSubmit() {
			if (DataSignInPage.getAuthSession().signIn((String)username.getModelObject(), (String)password.getModelObject(), 
					(Boolean)rememberMe.getModelObject()))
			{
				if (!continueToOriginalDestination())
					setResponsePage(getApplication().getHomePage());
			} else
				error(getLocalizer().getString("signInFailed", this, "Sorry, these credentials are not recognized."));
		}
	}
}
