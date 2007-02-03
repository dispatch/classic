package $package;

import net.databinder.auth.AuthDataSession;
import wicket.PageParameters;
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
public class SignInPanel extends Panel {

	public SignInPanel(String id) {
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
			add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE)));
		}
		@Override
		protected void onSubmit() {
			if (signIn((String)username.getModelObject(), (String)password.getModelObject(), 
					(Boolean)rememberMe.getModelObject()))
			{
				if (!continueToOriginalDestination())
					setResponsePage(getApplication().getSessionSettings().getPageFactory().newPage(
							getApplication().getHomePage(), (PageParameters)null));
			} else
				error(getLocalizer().getString("signInFailed", this, "Sorry, these credentials are not recognized."));
		}
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
