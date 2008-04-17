package net.databinder.auth.components.ao;

import org.apache.wicket.markup.html.WebPage;

import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

/**
 * Displays sign in and out links, as well as current user if any.
 * Replaceable String resources: <pre>
 * data.auth.status.account
 * data.auth.status.admin
 * data.auth.status.sign_out
 * data.auth.status.sign_in</pre>
 */
public class DataUserStatusPanel extends DataUserStatusPanelBase {

	public DataUserStatusPanel(String id) {
		super(id);
	}
	
	@Override
	protected Class<? extends WebPage> adminPageClass() {
		return UserAdminPage.class;
	}

	@Override
	protected WebPage profilePage(ReturnPage returnPage) {
		return new DataProfilePage(returnPage);
	}

}
