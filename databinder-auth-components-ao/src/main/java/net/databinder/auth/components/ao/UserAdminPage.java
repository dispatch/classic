package net.databinder.auth.components.ao;

import java.util.Map;

import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.UserAdminPageBase;
import net.databinder.auth.data.DataUser;
import net.databinder.auth.data.ao.UserHelper;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityListModel;
import net.databinder.models.ao.EntityModel;

import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public class UserAdminPage extends UserAdminPageBase {
	private DataForm form;
	
	@Override
	protected Form adminForm(String id, Class<? extends DataUser> userClass) {
		return form = new DataForm(id, new EntityModel(userClass) {
			@Override
			protected void putDefaultProperties(
					Map<String, Object> propertyStore) {
				propertyStore.put("roles", new Roles(Roles.USER));
			}			
		}) {
			@SuppressWarnings("unchecked")
			@Override
			protected void onSubmit() {
				if (!getEntityModel().isBound()) {
					Map<String, Object> map = (Map) getModelObject();
					map.put(UserHelper.getRoleStringField(), ((Roles)map.remove("roles")).toString());
				}
				super.onSubmit();
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void setPassword(String password) {
		if (form.getEntityModel().isBound())
			super.setPassword(password);
		else
			((Map)form.getModelObject()).put(UserHelper.getPasswordHashField(), UserHelper.getHash(password));
	}

	@Override
	protected Button deleteButton(String id) {
		return form.new DeleteButton(id);
	}

	@Override
	protected DataUserStatusPanelBase statusPanel(String id) {
		return new DataUserStatusPanel(id);
	}

	@Override
	protected IModel userList(Class<? extends DataUser> userClass) {
		return new EntityListModel(userClass);
	}

}
