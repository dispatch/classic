package net.databinder.auth.components.hib;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.components.hib.DataForm;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Registration with username, password, and password confirmation.
 * Replaceable String resources: <pre>
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.remember
 * data.auth.register
 * data.auth.update
 * data.auth.username.taken * </pre> * Must be overriden in a containing page
 * or a subclass of this panel.
 */
public class DataProfilePanel extends DataProfilePanelBase {
	public DataProfilePanel(String id, ReturnPage returnPage) {
		super(id, returnPage);
	}

	@Override
	protected Form profileForm(String id, IModel userModel) {
		if (userModel == null) 
			userModel = new HibernateObjectModel(((AuthApplication)getApplication()).getUserClass());

		return new DataForm(id, (HibernateObjectModel) userModel) {
			@Override
			protected void onSubmit() {
				super.onSubmit();
				DataProfilePanel.this.afterSubmit();
			}
		};
	}

}
