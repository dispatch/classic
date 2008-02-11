package net.databinder.auth.components;

import java.util.Arrays;
import java.util.List;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.AuthSession;
import net.databinder.auth.data.DataUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.DataStyleLink;
import net.databinder.components.ModelSourceListPanel;
import net.databinder.components.NullPlug;
import net.databinder.components.UnbindLink;
import net.databinder.models.BindingModel;

import org.apache.wicket.Component;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * User administration page. Lists all users, allows editing usernames, passwords, and roles.
 * Must have Role.ADMIN to view. Replaceable String resources: <pre>
 * data.auth.user_admin
 * data.auth.user_add
 * data.auth.username
 * data.auth.password
 * data.auth.passwordConfirm
 * data.auth.roles
 * data.auth.save
 * data.auth.delete</pre>
 * @see AuthSession
 */
@AuthorizeInstantiation(Roles.ADMIN)
public abstract class UserAdminPageBase extends WebPage {
	protected Form form;
	public UserAdminPageBase() {
		add(new DataStyleLink("css"));
		add(new Label("title", new ResourceModel("data.auth.user_admin", "User Administration")));
		Class<? extends DataUser> userClass = ((AuthApplication) getApplication()).getUserClass();
		
		add(statusPanel("userStatus"));
		
		form = adminForm("form", userClass);
		add(form);
		
		TextField username = new RequiredTextField("username");
		username.setLabel(new ResourceModel("data.auth.username", "Username"));
		form.add(new SimpleFormComponentLabel("username-label", username));
		form.add(username);

		TextField password = new RSAPasswordTextField("password", new Model(), form) {
			@Override
			public boolean isRequired() {
				return !isBound();
			}
			@Override
			protected void onModelChanged() {
				setPassword((String) getModelObject());
			}
		};
		password.setLabel(new ResourceModel("data.auth.password", "Password"));
		form.add(new SimpleFormComponentLabel("password-label", password));
		form.add(password);
		TextField passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), form) {
			public boolean isRequired() {
				return !isBound();
			}
		};
		form.add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
		passwordConfirm.setLabel(new ResourceModel("data.auth.passwordConfirm", "Retype Password"));
		form.add(new SimpleFormComponentLabel("passwordConfirm-label", passwordConfirm));
		form.add(passwordConfirm);
		
		form.add(new CheckBoxMultipleChoice("roles", rolesModel(), new AbstractReadOnlyModel() {
			public Object getObject() {
				return getRoleChoices();
			}
		}));

		form.add(lowFormSocket("lowForm"));

		form.add(deleteButton("delete"));

		form.add(new FeedbackPanel("feedback"));
		
		add(new UnbindLink("add", getBindingModel()));
				
		add(new ModelSourceListPanel("users", form, "username", userList(userClass)));
	}
	
	protected DataUser getUser() {
		return (DataUser) form.getModelObject();
	}
	
	protected void setPassword(String password) {
		if (password != null)
			getUser().getPassword().change(password);
	}
	
	protected BindingModel getBindingModel() {
		return (BindingModel) ((IChainingModel)form.getModel()).getChainedModel();
	}
	
	protected boolean isBound() {
		return getBindingModel().isBound();
	}
	
	protected abstract Form adminForm(String id, Class<? extends DataUser> userClass);
	
	protected abstract Button deleteButton(String id);
	
	protected abstract DataUserStatusPanelBase statusPanel(String id);

	protected abstract IModel userList(Class<? extends DataUser> userClass);
	
	protected Component lowFormSocket(String id) {
		return new NullPlug(id);
	}
	
	protected IModel rolesModel() {
		return null;
	}
	
	protected List<String> getRoleChoices() {
		return Arrays.asList(new String[] {Roles.USER, Roles.ADMIN});
	}
}
