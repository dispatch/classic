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

import net.databinder.DataRequestCycle;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;

import wicket.Component;

/**
 * Model persisted by Hibernate.
 * @author Nathan Hamblen
 */
public class HibernateObjectModel extends LoadableWritableModel {
	/** Used preferentially in loading objects. */
	private String entityName;
	/** Used if entityName unavailable, or for new objects. */
	private Class objectClass;
	private Serializable objectId;
	private String queryString;
	private IQueryBinder queryBinder;
	private IQueryBuilder queryBuilder;
	private ICriteriaBuilder criteriaBuilder;
	/** May store unsaved objects between requests. */
	private Serializable retainedObject;
	/** Enable retaining unsaved objects between requests. */
	private boolean retainUnsaved = false;
	
	/**
	 * @param objectClass class to be loaded and stored by Hibernate
	 * @param objectId id of the persistent object
	 * @throws org.hibernate.ObjectNotFoundException if objectId is not valid
	 */
	public HibernateObjectModel(Class objectClass, Serializable objectId) {
		this.objectClass = objectClass;
		this.objectId = objectId;
		// load object early, provoking load exceptions when they can be easily caught
		getObject(null);
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
	 * Construct with a query and binder that return exactly one result. Use this for fetch
	 * instructions, scalar results, or if the persistent object ID is not available.
	 * Queries that return more than one result will produce exceptions.
	 * @param queryString query returning one result
	 * @param queryBinder bind id or other parameters
	 * @throws org.hibernate.HibernateException on load error
	 */
	public HibernateObjectModel(String queryString, IQueryBinder queryBinder) {
		this.queryString = queryString;
		this.queryBinder = queryBinder;
		getObject(null);	// loads & retains object
	}
	
	/**
	 * Construct with a class and criteria binder that return exactly one result. Use this for fetch
	 * instructions, scalar results, or if the persistent object ID is not available. Criteria that 
	 * return more than one result will produce exceptions.
	 * @param objectClass class of object for root criteria
	 * @param criteriaBuilder builder to apply criteria restrictions
	 * @throws org.hibernate.HibernateException on load error
	 */
	public HibernateObjectModel(Class objectClass, ICriteriaBuilder criteriaBuilder) {
		this.objectClass = objectClass;
		this.criteriaBuilder = criteriaBuilder;
		getObject(null);	// loads & retains object
	}
	
	/**
	 * Construct with a query builder that returns exactly one result, used for custom query
	 * objects. Queries that return more than one result will produce exceptions.
	 * @param queryBuilder builder to create and bind query object
	 * @throws org.hibernate.HibernateException on load error
	 */
	public HibernateObjectModel(IQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
		getObject(null);	// loads & retains object
	}

	/**
	 * Construct with no object. Will return null for getObject().
	 */
	public HibernateObjectModel() {
	}
	
	/**
	 * Change the persistent object contained in this model.
	 * Because this method establishes a persistent object ID, queries and binders
	 * are removed if present.
	 */
	public void setObject(Component component, Object object) {
		clearPersistentObject();	// clear everything but objectClass
		
		if (object == null)
			// clear out completely
			objectClass = null;
		else {
			Session sess = DataRequestCycle.getHibernateSession();
			objectId = sess.getIdentifier(object);
			// the entityName, rather than the objectClass, will be used to load
			entityName = sess.getEntityName(object);
		}
	}
	
	/** Please use setObject(null, object) instead of this method. */
	@Deprecated
	public void setPersistentObject(Object persistentObject) {
		setObject(null, persistentObject);
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
		queryBuilder = null;
		queryString = null;
		criteriaBuilder = null;
		retainedObject = null;
		detach();
	}
	
	/**
	 * Load the object through Hibernate, contruct a new instance if it is not 
	 * bound to an id, or use unsaved retained object. This method uses the entityName 
	 * to load when possible. A correct  (unproxied) objectClass is always available for 
	 * contructing empty objects.
	 * @throws org.hibernate.HibernateException on load error
	 */
	@Override
	protected Object load() {
		if (objectClass == null && entityName == null && queryString == null && queryBuilder == null)
			return null;	// can't load without one of these
		try {
			if (objectId == null && queryString == null && criteriaBuilder == null && queryBuilder == null) {
				if (retainUnsaved && retainedObject != null)
					return retainedObject;
				else if (retainUnsaved)
					return retainedObject = (Serializable) objectClass.newInstance();
				else
					return objectClass.newInstance();
			}
		} catch (ClassCastException e) {
			throw new RuntimeException("Retaining unsaved model objects requires that they be Serializable.", e);
		} catch (Throwable e) {
			throw new RuntimeException("Unable to instantiate object. Does it have a default constructor?", e);
		}
		Session sess = DataRequestCycle.getHibernateSession();
		if (objectId != null) {
			if (entityName != null)
				return sess.load(entityName, objectId);
			return sess.load(objectClass, objectId);
		}
		
		if(criteriaBuilder != null) {
			Criteria criteria = sess.createCriteria(objectClass);
			criteriaBuilder.build(criteria);
			return criteria.uniqueResult();
		}
		
		if (queryBuilder != null)
			return queryBuilder.build(sess).uniqueResult();
		
		Query query = sess.createQuery(queryString);
		// if querybinder was null in constructor, that's weird, but continue
		if (queryBinder != null)
			queryBinder.bind(query);
		Object o = query.uniqueResult();
		if (o == null)
			throw new QueryException("Returned no results", queryString);
		return o;
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

	/**
	 * @return true if unsaved objects should be retained between requests.
	 */
	public boolean getRetainUnsaved() {
		return retainUnsaved;
	}

	/**
	 * Unsaved Serializable objects can be retained between requests.
	 * @param retainUnsaved set to true to retain unsaved objects
	 */
	public void setRetainUnsaved(boolean retainUnsaved) {
		this.retainUnsaved = retainUnsaved;
	}
}
