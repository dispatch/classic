package net.databinder.auth.components;

import net.databinder.auth.AuthDataSession;
import net.databinder.auth.IAuthSettings;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.Link;
import wicket.markup.html.link.PageLink;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;

/**
 * Displays sign in and out links, as well as current user if any. 
 * @author Nathan Hamblen
 */
public class DataUserStatusPanel extends Panel {
	/**
	 * Constructs sign in and out links.
	 * @param id Wicket id
	 */
	public DataUserStatusPanel(String id) {
		super(id);
		
		add(new WebMarkupContainer("signedInWrapper") {
			public boolean isVisible() {
				return getAuthSession().isSignedIn();
			}
		}.add(new Label("username", new Model() {
			public Object getObject(wicket.Component component) {
				return getUsername();
			}
		})).add(new Link("signOut") {
			@Override
			public void onClick() {
				signOut();
			}
		}));
		
		add(getSignInLink("signIn"));
	}
	
	/** Signs out from session; override for other behavior.	 */
	protected void signOut() {
		getAuthSession().signOut();
	}
	
	/** 
	 * Returns link to sign-in page from <tt>AuthDataApplication</tt> subclass. Override
	 * for other behavior.	 
	 */
	protected Link getSignInLink(String id) {
		return new PageLink(id, ((IAuthSettings)getApplication()).getSignInPageClass()) {
			@Override
			public boolean isVisible() {
				return !getAuthSession().isSignedIn();
			}
		};
	}
	
	/**
	 * Returns user.toString() on IUser from the session. Override for other behavior.
	 * @return text (name) to display adjacent to sing out link.
	 */
	protected String getUsername() {
		return getAuthSession().getUser().toString();
	}
	
	/** @return casted web session*/
	protected AuthDataSession getAuthSession() {
		return (AuthDataSession) getSession();
	}
}
