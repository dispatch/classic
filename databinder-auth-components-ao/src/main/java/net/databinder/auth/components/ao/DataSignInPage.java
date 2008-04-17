package net.databinder.auth.components.ao;

import net.databinder.auth.components.DataSignInPageBase;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;

/**
 * Sign in and registration page.
 * Replaceable String resources: <pre>
 * data.auth.title.sign_in
 * data.auth.pre_register_link
 * data.auth.register_link
 * data.auth.pre_sign_in_link
 * data.auth.sign_in_link
 * or a subclass of this panel.
 */
public class DataSignInPage extends DataSignInPageBase {
	
	public DataSignInPage(ReturnPage returnPage) {
		super(returnPage);
	}
	public DataSignInPage(PageParameters params) {
		super(params);
	}
	
	@Override
	protected Component profileSocket(String id, ReturnPage returnPage) {
		return new DataProfilePanel(id, returnPage);
	}
}
