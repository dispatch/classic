package net.databinder.auth.components.hib;

import org.apache.wicket.Component;

import net.databinder.auth.components.DataProfilePageBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

public class DataProfilePage extends DataProfilePageBase {
	public DataProfilePage(ReturnPage returnPage) {
		super(returnPage);
	}
	@Override
	protected Component profileSocket(String id, ReturnPage returnPage) {
		return new DataProfilePanel(id, returnPage);
	}
}
