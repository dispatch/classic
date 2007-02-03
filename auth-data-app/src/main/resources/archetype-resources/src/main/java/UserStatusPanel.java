package $package;

import net.databinder.auth.AuthDataSession;
import net.databinder.auth.IAuthSettings;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.Link;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;

/**
 * Displays sign in and out links, as well as current user if any. 
 */
public class UserStatusPanel extends Panel {
	/**
	 * Constructs sign in and out links.
	 * @param id Wicket id
	 */
	public UserStatusPanel(String id) {
		super(id);
		
		add(new WebMarkupContainer("signedInWrapper") {
			public boolean isVisible() {
				return getAuthSession().isSignedIn();
			}
		}.add(new Label("username", new Model() {
			public Object getObject(wicket.Component component) {
				return getAuthSession().getUser().toString();
			}
		})).add(new Link("signOut") {
			@Override
			public void onClick() {
				getAuthSession().signOut();
			}
		}));
		
		add(getSignInLink("signIn"));
	}
	
	/** 
	 * Returns link to sign-in page from <tt>AuthDataApplication</tt> subclass. Uses redirect
	 * to intercept page so that user will return to current page once signed in. Override
	 * for other behavior.	 
	 */
	protected Link getSignInLink(String id) {
		return new Link(id) {
			@Override
			public void onClick() {
				redirectToInterceptPage(getPageFactory().newPage(
						((IAuthSettings)getApplication()).getSignInPageClass()));
			}
			@Override
			public boolean isVisible() {
				return !getAuthSession().isSignedIn();
			}
		};
	}
	
	/** @return casted web session*/
	protected AuthDataSession getAuthSession() {
		return (AuthDataSession) getSession();
	}
}
