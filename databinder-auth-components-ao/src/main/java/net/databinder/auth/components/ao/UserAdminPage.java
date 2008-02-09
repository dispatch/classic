package net.databinder.auth.components.ao;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import net.databinder.auth.components.DataUserStatusPanelBase;
import net.databinder.auth.components.UserAdminPageBase;
import net.databinder.auth.data.DataUser;
import net.databinder.components.ao.DataForm;
import net.databinder.models.ao.EntityListModel;

public class UserAdminPage extends UserAdminPageBase {
	private DataForm form;
	
	@Override
	protected Form adminForm(String id, Class<? extends DataUser> userClass) {
		return form = new DataForm(id, userClass);
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
