package $package;

import net.databinder.auth.AuthDataApplication;

import org.hibernate.cfg.AnnotationConfiguration;

public class MyApplication extends AuthDataApplication {

	/**
	 * @return Page to display when no specific page is requested
	 */
	@Override
	public Class getHomePage() {
		return MyDataPage.class;
	}

	
	@Override
	public Class<DataUser> getUserClass() {
		return DataUser.class;
	}
	
	@Override
	public Class<SignInPage> getSignInPageClass() {
		return SignInPage.class;
	}
	
	/**
	 * Add annotated classes to config, leaving the call to super-implementation in most cases.
	 * @param config Hibernate configuration
	 */
	@Override
	protected void configureHibernate(AnnotationConfiguration config) {
		super.configureHibernate(config);
		//config.addAnnotatedClass(MyItem.class);
	}

	@Override
	public byte[] getSalt() {
		return "${archetypeId}".getBytes(); // TODO: change to something more random
	}

}
