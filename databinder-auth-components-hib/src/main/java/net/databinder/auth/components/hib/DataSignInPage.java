package net.databinder.auth.components.hib;

import net.databinder.auth.components.DataSignInPageBase;

import org.apache.wicket.Component;

public class DataSignInPage extends DataSignInPageBase {
	public DataSignInPage(ReturnPage returnPage) {
		super(returnPage);
	}
	@Override
	protected Component profileSocket(String id, ReturnPage returnPage) {
		return new DataProfilePanel(id, returnPage);
	}
}
