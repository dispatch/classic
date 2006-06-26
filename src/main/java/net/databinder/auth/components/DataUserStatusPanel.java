package net.databinder.auth.components;

import net.databinder.auth.AuthDataApplication;
import net.databinder.auth.AuthDataSession;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.basic.Label;
import wicket.markup.html.link.Link;
import wicket.markup.html.link.PageLink;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;

public class DataUserStatusPanel extends Panel {
	public DataUserStatusPanel(String id) {
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
		
		add(new PageLink("signIn", ((AuthDataApplication)getApplication()).getSignInPageClass()) {
			@Override
			public boolean isVisible() {
				return !getAuthSession().isSignedIn();
			}
		});
	}
	
	protected AuthDataSession getAuthSession() {
		return (AuthDataSession) getSession();
	}
}
