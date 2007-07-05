package net.databinder.auth.components;

import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.data.IUser;
import net.databinder.components.DataStyleLink;
import net.databinder.components.StyleLink;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Serves as both a sign in and simple registration page. 
 */
public class DataSignInPage extends WebPage {
	/** state of page, sign in or registration */
	private boolean register = false;
	
	/** Registration panel whose visibility is controlled from this class. */
	private Panel registerPanel;

	/** Sign-in panel whose visibility is controlled from this class. */
	private Panel signInPanel;

	/**
	 * Displays sign in page.
	 */
	public DataSignInPage(PageParameters params) {
		String username = params.getString("username");
		String token = params.getString("token");
		if (username != null && token != null) {
			IAuthSettings settings = ((IAuthSettings)Application.get());
			HibernateObjectModel userModel = new HibernateObjectModel(settings.getUserClass(),
					settings.getUserCriteriaBuilder(username));  
			IUser.CookieAuth user = (IUser.CookieAuth) userModel.getObject();
			if (user != null && user.getToken().equals(token))
				getAuthSession().signIn(user, true);
			setResponsePage(((Application)settings).getHomePage());
			setRedirect(true);
			return;
		}
		
		init();
	}
	public DataSignInPage(boolean register) {
		this.register = register;
		init();
	}
	void init() {
		add(new DataStyleLink("dataStylesheet"));
		add(new StyleLink("signinStylesheet", DataSignInPage.class));
		
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
				return register && !getAuthSession().isSignedIn();
			}
		}.add(new Link("signIn") {
			@Override
			public void onClick() {
				setRegister(false);
			}
		}));
		
		add(signInPanel = new DataSignInPanel("signInPanel"));
		add(registerPanel = new DataRegisterPanel("registerPanel"));
		setRegister(register);
	}

	/**
	 * @return true if displaying registration page
	 */
	protected boolean isRegister() {
		return register;
	}
	
	protected static  IAuthSession getAuthSession() {
		return (IAuthSession) Session.get();
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
