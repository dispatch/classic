package net.databinder.auth.components.hib;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.components.hib.DataForm;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

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
