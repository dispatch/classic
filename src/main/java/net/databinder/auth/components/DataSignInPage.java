package net.databinder.auth.components;

import net.databinder.components.DataPage;
import net.databinder.components.StyleLink;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.WebPage;
import wicket.markup.html.link.Link;

public class DataSignInPage extends WebPage {
	// @bug must be able to completely disable
	private boolean isRegister = false;

	public DataSignInPage() {
		
		add(new StyleLink("dataStylesheet", DataPage.class));
		
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
