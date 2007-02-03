package $package;

import net.databinder.DataApplication;

import org.hibernate.cfg.AnnotationConfiguration;

public class MyApplication extends DataApplication {

	/**
	 * @return Page to display when no specific page is requested
	 */
	@Override
	public Class getHomePage() {
		return MyDataPage.class;
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

}
