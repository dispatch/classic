package net.databinder.auth.components.ao;

import java.util.Map;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.components.DataProfilePanelBase;
import net.databinder.auth.components.DataSignInPageBase.ReturnPage;
import net.databinder.auth.data.ao.UserHelper;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityModel;

import org.apache.wicket.authorization.strategies.role.Roles;
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
	
	private DataForm form;
	
	@Override
	protected Form profileForm(String id, IModel userModel) {
		if (userModel == null) 
			userModel = new EntityModel(((AuthApplication)getApplication()).getUserClass());
		return form = new DataForm(id, (EntityModel) userModel) {
			@SuppressWarnings("unchecked")
			@Override
			protected void onSubmit() {
				if (!getEntityModel().isBound()) {
					Map<String, Object> map = (Map) getModelObject();
					map.put("roleString", Roles.USER);
				}
				super.onSubmit();
			}
			@Override
			protected void afterSubmit() {
				DataProfilePanel.this.afterSubmit();
			}
		};
	}
	
	/**
	 * Uses super implementation if bound, but for new users this method must call
	 * UserHelper.getHash(password) to set the hash in "passwordHash". If application 
	 * uses different hasing implementation.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void setPassword(String password) {
		if (form.getEntityModel().isBound())
			super.setPassword(password);
		else
			((Map)form.getModelObject()).put("passwordHash", UserHelper.getHash(password));
	}

}
