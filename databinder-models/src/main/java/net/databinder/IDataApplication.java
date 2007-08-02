package net.databinder;

import org.hibernate.SessionFactory;

public interface IDataApplication {
	SessionFactory getHibernateSessionFactory(Object key);
}
