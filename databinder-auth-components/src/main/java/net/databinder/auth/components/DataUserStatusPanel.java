package net.databinder.auth.components;

import java.io.Serializable;

import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Displays sign in and out links, as well as current user if any.
 */
public class DataUserStatusPanel extends Panel {
	/**
	 * Constructs sign in and out links.
	 * @param id Wicket id
	 */
	public DataUserStatusPanel(String id) {
		super(id);

		WebMarkupContainer wrapper = new WebMarkupContainer("signedInWrapper") {
			public boolean isVisible() {
				return getAuthSession().isSignedIn();
			}
		};
		add(wrapper);
		wrapper.add(new Label("username", new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return getAuthSession().getUser().toString();
			}
		}));
		wrapper.add(new Link("profile") {
			@Override
			public void onClick() {
				if (getSession().getMetaData(inDetourKey) == null) {
					getSession().setMetaData(inDetourKey,  new InDetour());
					redirectToInterceptPage(getPageFactory().newPage(((IAuthSettings)getApplication()).getSignInPageClass()));
				} else
					getSession().setMetaData(inDetourKey,  null);
			}
		});

		wrapper.add(new Link("signOut") {
			@Override
			public void onClick() {
				getAuthSession().signOut();
				setResponsePage(getApplication().getHomePage());
			}
		});

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
	/** Session marker for editing profile */
	static class InDetour implements Serializable { }
	static MetaDataKey inDetourKey = new MetaDataKey(InDetour.class) { };

	/** @return casted web session*/
	protected IAuthSession getAuthSession() {
		return (IAuthSession) getSession();
	}
}
