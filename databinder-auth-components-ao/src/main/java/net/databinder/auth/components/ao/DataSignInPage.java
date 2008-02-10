package net.databinder.auth.components.ao;

import net.databinder.auth.components.DataSignInPageBase;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;

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
