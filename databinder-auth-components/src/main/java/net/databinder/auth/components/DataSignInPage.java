package net.databinder.auth.components;

import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.data.IUser;
import net.databinder.components.DataStyleLink;
import net.databinder.components.SourceList;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Serves as both a sign in and simple registration page. 
 */
public class DataSignInPage extends WebPage {
	/** Registration panel whose visibility is controlled from this class. */
	private Panel registerPanel;

	/** Sign-in panel whose visibility is controlled from this class. */
	private Panel signInPanel;
	
	private SourceList sourceList;
	
	private SourceList.SourceLink registerLink, signinLink;

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
		add(new DataStyleLink("dataStylesheet"));
		
		sourceList = new SourceList();
		
		add(registerPanel = new DataRegisterPanel("registerPanel"));
		registerLink = sourceList.new SourceLink("register", registerPanel);
		add(new WebMarkupContainer("gotoRegister") {
			public boolean isVisible() {
				return registerLink.isEnabled();
			}
		}.add(registerLink));
		
		add(signInPanel = new DataSignInPanel("signInPanel"));
		signinLink = sourceList.new SourceLink("signIn", signInPanel);
		add(new WebMarkupContainer("gotoSignIn") {
			@Override
			public boolean isVisible() {
				return signinLink.isEnabled();
			}
		}.add(signinLink));
		signinLink.onClick();
	}
	protected static  IAuthSession getAuthSession() {
		return (IAuthSession) Session.get();
	}
	public SourceList.SourceLink getRegisterLink() {
		return registerLink;
	}
	public SourceList.SourceLink getSigninLink() {
		return signinLink;
	}
}
