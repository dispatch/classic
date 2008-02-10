package net.databinder.auth.components.ao;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityModel;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public class DataProfilePanel extends DataProfilePanelBase {
	
	public DataProfilePanel(String id, ReturnPage returnPage) {
		super(id, returnPage);
	}
	
	@Override
	protected Form getProfileForm(String id, IModel userModel) {
		if (userModel == null) 
			userModel = new EntityModel(((AuthApplication)getApplication()).getUserClass());
		return new DataForm(id, (EntityModel) userModel) {
			@Override
			protected void afterSubmit() {
				DataProfilePanel.this.afterSubmit();
			}
		};
	}

}
