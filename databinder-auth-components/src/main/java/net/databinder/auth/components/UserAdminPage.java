package net.databinder.auth.components;

import java.util.Arrays;
import java.util.List;

import net.databinder.DataStaticService;
import net.databinder.auth.IAuthSession;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.data.IUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.DataStyleLink;
import net.databinder.components.ModelSourceListPanel;
import net.databinder.components.NullPlug;
import net.databinder.components.hibernate.DataForm;
import net.databinder.models.HibernateListModel;

import org.apache.wicket.Component;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

@AuthorizeInstantiation(Roles.ADMIN)
public class UserAdminPage extends WebPage {
	protected DataForm form;
	public UserAdminPage() {
		add(new DataStyleLink("css"));
		Class<? extends IUser> userClass = ((IAuthSettings) getApplication()).getUserClass();
		
		add(new DataUserStatusPanel("userStatus"));
		
		form = new DataForm("form", userClass);
		add(form);
		
		TextField username = new RequiredTextField("username");
		username.setLabel(new ResourceModel("data.auth.username", "Username"));
		form.add(new SimpleFormComponentLabel("username-label", username));
		form.add(username);

		TextField password = new RSAPasswordTextField("password", form) {
			@Override
			public boolean isRequired() {
				return !form.getPersistentObjectModel().isBound();
			}
		};
		password.setLabel(new ResourceModel("data.auth.password", "Password"));
		form.add(new SimpleFormComponentLabel("password-label", password));
		form.add(password);
		TextField passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), form) {
			public boolean isRequired() {
				return !form.getPersistentObjectModel().isBound();
			}
		};
		form.add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
		passwordConfirm.setLabel(new ResourceModel("data.auth.passwordConfirm", "Retype Password"));
		form.add(new SimpleFormComponentLabel("passwordConfirm-label", passwordConfirm));
		form.add(passwordConfirm);
		
		form.add(new CheckBoxMultipleChoice("roles", new AbstractReadOnlyModel() {
			public Object getObject() {
				return getRoles();
			}
		}));

		form.add(lowFormSocket("lowForm"));

		form.add(new Button("delete") {
			@Override
			public void onSubmit() {
				DataStaticService.getHibernateSession().delete(form.getModelObject());
				DataStaticService.getHibernateSession().getTransaction().commit();
				form.clearPersistentObject();
			}
			@Override
			public boolean isEnabled() {
				return !((IAuthSession)getSession()).getUser().equals(form.getModelObject())
					&& form.getPersistentObjectModel().isBound();
			}
		}.setDefaultFormProcessing(false));
		form.add(new FeedbackPanel("feedback"));
		
		add(new Link("add") {
			public void onClick() {
				form.clearPersistentObject();
			}
			public boolean isEnabled() {
				return form.getPersistentObjectModel().isBound();
			}
		});
		add(new ModelSourceListPanel("users", form, "username", new HibernateListModel(userClass)));
	}
	
	protected Component lowFormSocket(String id) {
		return new NullPlug(id);
	}
	
	protected List<String> getRoles() {
		return Arrays.asList(new String[] {Roles.USER, Roles.ADMIN});
	}
}
