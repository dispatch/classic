package net.databinder.auth.components;

import net.databinder.auth.AuthDataApplication;
import net.databinder.components.DataPage;
import net.databinder.components.StyleLink;
import wicket.Application;
import wicket.WicketRuntimeException;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.WebPage;
import wicket.markup.html.link.Link;

public class DataSignInPage extends WebPage {
	private boolean isRegister = false;

	public DataSignInPage() {
		// make sure nothing funny is going on, since this page has a default
		// constructor and is bookmarkable
		if (!((AuthDataApplication)Application.get()).getSignInPageClass().equals(getClass()))
			throw new WicketRuntimeException("The sign in page requested does not match that defined in the AuthDataApplication subclass.");
		
		add(new StyleLink("dataStylesheet", DataPage.class));
		add(new StyleLink("signinStylesheet", DataSignInPage.class));
		
		add(new WebMarkupContainer("gotoRegister"){
			@Override
			public boolean isVisible() {
				return !isRegister;
			}
		}.add(new Link("register") {
			@Override
			public void onClick() {
				isRegister = true;
			}
		}));
		
		add(new WebMarkupContainer("gotoSignIn"){
			@Override
			public boolean isVisible() {
				return isRegister;
			}
		}.add(new Link("signIn") {
			@Override
			public void onClick() {
				isRegister = false;
			}
		}));
		
		add(new DataSignInPanel("signInPanel") {
			@Override
			public boolean isVisible() {
				return !isRegister;
			}
		});
		
		add(new DataRegisterPanel("registerPanel") {
			@Override
			public boolean isVisible() {
				return isRegister;
			}
		});
	}

}
