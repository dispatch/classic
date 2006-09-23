package net.databinder;

import org.hibernate.SessionFactory;

public class DataStaticService {
	private static SessionFactory hibernateSessionFactory;
	
	public static SessionFactory getHibernateSessionFactory() {
		return hibernateSessionFactory;
	}
	
	// TODO: DataApplication interface
	public static void init(DataApplication app) {
		if (hibernateSessionFactory != null)
			throw new ExceptionInInitializerError("DatabinderHolder.init() called more than once.");
		hibernateSessionFactory = app.createHibernateSessionFactory();
	}

}
