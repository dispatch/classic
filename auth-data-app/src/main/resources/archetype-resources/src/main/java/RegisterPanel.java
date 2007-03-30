package $package;

import java.util.HashMap;
import java.util.Map;

import net.databinder.DataStaticService;
import net.databinder.auth.AuthDataSession;
import net.databinder.auth.components.RSAPasswordTextField;
import net.databinder.auth.util.EqualPasswordConvertedInputValidator;
import net.databinder.components.DataForm;
import net.databinder.models.HibernateObjectModel;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import wicket.AttributeModifier;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.CheckBox;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Panel;
import wicket.model.AbstractReadOnlyModel;
import wicket.model.Model;
import wicket.util.string.Strings;
import wicket.validation.IValidatable;
import wicket.validation.validator.StringValidator;

/**
 * Registration with username, password, and password confirmation.
 */
public class RegisterPanel extends Panel {

	public RegisterPanel(String id) {
		super(id);
		add(new FeedbackPanel("feedback"));
		HibernateObjectModel userModel = ((AuthDataSession)getSession()).getUserModel();
		if (userModel == null) 
			userModel = new HibernateObjectModel(DataUser.class);
		add(new RegisterForm("registerForm", userModel));
	}
	
	protected class RegisterForm extends DataForm {
		private RSAPasswordTextField password, passwordConfirm;
		private CheckBox rememberMe;
		
		DataUser getUser() {
			return (DataUser) getModelObject();
		}
		
		boolean existing() {
			return getUser().getId() != null;
		}
		
		public RegisterForm(String id, HibernateObjectModel typistModel) {
			super(id, typistModel);
			add(new RequiredTextField("username").add(new StringValidator(){
				@Override
				protected void onValidate(IValidatable validatable) {
					String username = (String) validatable.getValue();
					if (username != null && !Strings.isEqual(username, getUser().getUsername()) && !isAvailable(username)) {
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

			((AuthDataSession)wicket.Session.get()).signIn(getUser(), (Boolean) rememberMe.getModelObject());

			if (!continueToOriginalDestination())
				setResponsePage(getApplication().getHomePage());
		}
	}

	/** @return true if the given username has not been taken */
	protected static boolean isAvailable(String username) {
		Session session = DataStaticService.getHibernateSession();
		Criteria c = session.createCriteria(DataUser.class);
		c.add(Property.forName("username").eq(username));
		c.setProjection(Projections.rowCount());
		return c.uniqueResult().equals(0);
	}
}
