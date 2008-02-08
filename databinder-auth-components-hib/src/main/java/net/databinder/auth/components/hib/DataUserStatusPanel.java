package net.databinder.auth.components.hib;

import org.apache.wicket.markup.html.WebPage;

import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

public class DataUserStatusPanel extends DataUserStatusPanelBase {
	public DataUserStatusPanel(String id) {
		super(id);
	}
	@Override
	protected WebPage profilePage(ReturnPage returnPage) {
		return new DataProfilePage(returnPage);
	}
	@Override
	protected Class<? extends WebPage> adminPageClass() {
		return UserAdminPage.class;
	}
}
