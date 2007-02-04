package $package;

import net.databinder.components.StyleLink;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.WebPage;
import wicket.markup.html.link.Link;
import wicket.markup.html.panel.Panel;

/**
 * Serves as both a sign in and simple regristration page. 
 */
public class SignInPage extends WebPage {
	/** state of page, sign in or registration */
	private boolean register = false;
	
	/** Registration panel whose visibility is controlled from this class. */
	private Panel registerPanel;

	/** Sign-in panel whose visibility is controlled from this class. */
	private Panel signInPanel;

	/**
	 * Displays sign in page. Checks that the page being instantiated is of the type returned
	 * by AuthDataApplication.getSignInPageClass().
	 */
	public SignInPage() {
		add(new StyleLink("signinStylesheet", SignInPage.class));
		
		add(new WebMarkupContainer("gotoRegister"){
			@Override
			public boolean isVisible() {
				return !register;
			}
		}.add(new Link("register") {
			@Override
			public void onClick() {
				setRegister(true);
			}
		}));
		
		add(new WebMarkupContainer("gotoSignIn"){
			@Override
			public boolean isVisible() {
				return register;
			}
		}.add(new Link("signIn") {
			@Override
			public void onClick() {
				setRegister(false);
			}
		}));
		
		add(signInPanel = new SignInPanel("signInPanel"));
		add(registerPanel = new RegisterPanel("registerPanel"));
		setRegister(register);
	}

	/**
	 * @return true if displaying registration page
	 */
	protected boolean isRegister() {
		return register;
	}
	
	/**
	 * @param register true to display the registration version of this page
	 */
	protected void setRegister(boolean register) {
		this.register = register;
		registerPanel.setVisible(register);
		signInPanel.setVisible(!register);
	}
}
