package net.databinder.auth.components.hib;

import org.apache.wicket.Component;

import net.databinder.auth.components.DataProfilePageBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;

/**
 * Display profile editing page for logged in user. 
 * Replaceable String resources: <pre>
 * data.auth.update
 * data.auth.title.update
 */
public class DataProfilePage extends DataProfilePageBase {
	public DataProfilePage(ReturnPage returnPage) {
		super(returnPage);
	}
	@Override
	protected Component profileSocket(String id, ReturnPage returnPage) {
		return new DataProfilePanel(id, returnPage);
	}
}
