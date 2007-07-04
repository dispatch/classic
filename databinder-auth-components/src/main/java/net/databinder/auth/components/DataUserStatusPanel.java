package net.databinder.auth.components;

import net.databinder.auth.AuthDataSession;
import net.databinder.auth.IAuthSettings;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Displays sign in and out links, as well as current user if any.
 * @author Nathan Hamblen
 * @deprecated
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
		}.add(new Label("username", new AbstractReadOnlyModel() {
			public Object getObject() {
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
