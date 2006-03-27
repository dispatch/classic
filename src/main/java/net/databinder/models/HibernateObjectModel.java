/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.databinder.models;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.persistence.Version;

import org.hibernate.Query;

import net.databinder.DataRequestCycle;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import wicket.model.LoadableDetachableModel;

/**
 * Model persisted by Hibernate.
 * @author Nathan Hamblen
 */

public class HibernateObjectModel extends LoadableDetachableModel {
	/** Used preferentially in loading objects. */
	private String entityName;
	/** Used if entityName unavailable, or for new objects. */
	private Class objectClass;
	private Serializable objectId;
	private String queryString;
	private IQueryBinder queryBinder;
	
	/**
	 * @param objectClass class to be loaded and stored by Hibernate
	 * @param objectId id of the persistent object
	 */
	public HibernateObjectModel(Class objectClass, Serializable objectId) {
		this.objectClass = objectClass;
		this.objectId = objectId;
	}
	
	/**
	 * Constructor for a model with no existing persistent object. The model object
	 * will NOT be persisted in any way until it is replaced by a call to 
	 * setPersistentObject(Object persistent Object). Instead, it is newly constructed and 
	 * empty after any call to detach().  Form components themselves will hold any entered 
	 * data until the object can be saved for the first time, usually after the first successful 
	 * form submittal. 
	 * @param objectClass class to be loaded and stored by Hibernate
	 */
	public HibernateObjectModel(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	/**
	 * Construct with a Hibernate persistent object.
	 * @param persistentObject must already be contained in the Hibernate session
	 */
	public HibernateObjectModel(Object persistentObject) {
		setPersistentObject(persistentObject);
	}

	/**
	 * Construct with a query that returns exactly one result. Use this for fetch
	 * instructions, scalar results, or if the persistent object ID is not available.
	 * Queries that do not return exactly one result will produce exceptions.
	 * @param queryString query returning one result
	 * @param queryBinder bind id or other parameters
	 */
	public HibernateObjectModel(String queryString, IQueryBinder queryBinder) {
		this.queryString = queryString;
		this.queryBinder = queryBinder;
	}
	
	/**
	 * Change the persistent object contained in this model. By using this method instead of
	 * replacing the model itself, you avoid accidentally referencing the old model. 
	 * Because this method establishes a persistent object ID, queries and binders
	 * are removed if present.
	 */
	public void setPersistentObject(Object persistentObject) {
		clearPersistentObject();
		Session sess = DataRequestCycle.getHibernateSession();
		objectId = sess.getIdentifier(persistentObject);
		// the entityName, rather than the objectClass, will be used to load
		entityName = sess.getEntityName(persistentObject);
	}
	
	/**
	 * Disassociates this object from any persitant object, but retains the class
	 * for contructing a blank copy if requested.
	 * @see HibernateObjectModel(Class objectClass)
	 */
	public void clearPersistentObject() {
		Object o = getObject(null);
		if (o != null)
			if (o instanceof HibernateProxy)
				objectClass = ((HibernateProxy)o).getHibernateLazyInitializer()
					.getImplementation().getClass();
			else
				objectClass = o.getClass();
		entityName = null;
		objectId = null;
		queryBinder = null; 
		queryString = null;
		detach();
	}
	
	/**
	 * Load the object through Hibernate, or contruct a new instance if it is not 
	 * bound to an id. This method uses the entityName to load when possible, simply
	 * because that is the recomended method. A correct (unproxied) objectClass
	 * is always available for contructing empty objects.
	 * @throws org.hibernate.HibernateException on load error
	 */
	@Override
	protected Object load() {
		try {
			if (objectId == null && queryString == null)
				return objectClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to instantiate object. Does it have a default constructor?", e);
		}
		Session sess = DataRequestCycle.getHibernateSession();
		if (objectId != null) {
			if (entityName != null)
				return sess.load(entityName, objectId);
			return sess.load(objectClass, objectId);
		}			
		
		Query query = sess.createQuery(queryString);
		// if querybinder was null in constructor, that's weird, but continue
		if (queryBinder != null)
			queryBinder.bind(query);
		return query.uniqueResult();
	}
	
	/**
	 * Uses version annotation to find version for this Model's object. 
	 * @return Persistent storage version number if available, null otherwise
	 */
	public Serializable getVersion() {
		Object o = getObject(null);
		if (o != null)
			// must check superclasses; won't show on subs (incl cglib)
			for (Class c = o.getClass(); c != null; c = c.getSuperclass())
				for (Method m : c.getMethods())
					if (m.isAnnotationPresent(Version.class) 
							&& m.getParameterTypes().length == 0
							&& m.getReturnType() instanceof Serializable)
						try {
							return (Serializable) m.invoke(o, new Object[] {});
						} catch (InvocationTargetException e) {
							throw new RuntimeException(e);
						} catch (IllegalAccessException e) {
							throw new RuntimeException(e);
						}
		return null;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
