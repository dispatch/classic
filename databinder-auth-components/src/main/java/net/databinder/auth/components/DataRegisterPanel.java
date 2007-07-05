package net.databinder.auth.components;

import java.util.HashMap;
import java.util.Map;

import net.databinder.DataStaticService;
import net.databinder.auth.IAuthSettings;
import net.databinder.auth.data.IUser;
import net.databinder.auth.valid.EqualPasswordConvertedInputValidator;
import net.databinder.components.hibernate.DataForm;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

/**
 * Registration with username, password, and password confirmation.
 */
public class DataRegisterPanel extends Panel {

	public DataRegisterPanel(String id) {
		super(id);
		add(new FeedbackPanel("feedback"));
		HibernateObjectModel userModel = DataSignInPage.getAuthSession().getUserModel();
		if (userModel == null) 
			userModel = new HibernateObjectModel(((IAuthSettings)getApplication()).getUserClass());
		add(new RegisterForm("registerForm", userModel));
	}
	
	protected class RegisterForm extends DataForm {
		private RSAPasswordTextField password, passwordConfirm;
		private CheckBox rememberMe;
		
		IUser getUser() {
			return (IUser) getPersistentObjectModel().getObject();
		}
		
		boolean existing() {
			return DataStaticService.getHibernateSession().contains(getUser());
		}
		
		public RegisterForm(String id, HibernateObjectModel typistModel) {
			super(id, typistModel);
			add(new RequiredTextField("username").add(new StringValidator(){
				@Override
				protected void onValidate(IValidatable validatable) {
					String username = (String) validatable.getValue();
					if (username != null && !isAvailable(username)) { // TODO is valid if has name already
						Map<String, String> m = new HashMap<String, String>(1);
						m.put("username", username);
						error(validatable,"taken",  m);
					}
				}
			}));
			add(password = new RSAPasswordTextField("password", this) {
				public boolean isRequired() {
					return !existing();
				}
			});
			add(passwordConfirm = new RSAPasswordTextField("passwordConfirm", new Model(), this) {
				public boolean isRequired() {
					return !existing();
				}
			});
			add(new EqualPasswordConvertedInputValidator(password, passwordConfirm));
			
			add(new WebMarkupContainer("rememberMeRow") { 
				public boolean isVisible() {
					return !existing();
				}
			}.add(rememberMe = new CheckBox("rememberMe", new Model(Boolean.FALSE))));
			
			add(new WebMarkupContainer("submit").add(new AttributeModifier("value", new AbstractReadOnlyModel() {
				public Object getObject() {
					return existing() ? "Update Profile" : "Register";
				}
			})));
		}

		@Override
		protected void onSubmit() {
			super.onSubmit();
			
			DataSignInPage.getAuthSession().signIn(getUser(), (Boolean) rememberMe.getModelObject());

			if (!continueToOriginalDestination())
				setResponsePage(getApplication().getHomePage());
		}
	}

	/** @return true if the given username has not been taken */
	public static boolean isAvailable(String username) {
		Session session = DataStaticService.getHibernateSession();
		IAuthSettings authSettings = (IAuthSettings)Application.get();
		Criteria c = session.createCriteria(authSettings.getUserClass());
		authSettings.getUserCriteriaBuilder(username).build(c);
		c.setProjection(Projections.rowCount());
		return c.uniqueResult().equals(0);
	}
}
