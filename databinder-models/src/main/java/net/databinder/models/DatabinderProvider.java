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

import java.util.Iterator;

import net.databinder.DataStaticService;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.BoundCompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Provides query results to DataView and related components. Like the Hibernate model classes,
 * the results of this provider can be altered by query binders and criteria builders. By default
 * this provider wraps items in a compound property model in addition to a Hibernate model.
 * This is convenient for mapping DataView subcomponents as bean properties (as with
 * PropertyListView). However, <b>DataTable will not work with a compound property model.</b>
 * Call setWrapWithPropertyModel(false) when using with DataTable, DataGridView, or any
 * other time you do not want a compound property model.
 * @author Nathan Hamblen
 */
public class DatabinderProvider implements IDataProvider  {
	private Class objectClass;
	private ICriteriaBuilder criteriaBuilder, sortCriteriaBuilder;
	private String queryString, countQueryString;
	private IQueryBinder queryBinder, countQueryBinder;
	/** Controls wrapping with a compound property model. */
	private boolean wrapWithPropertyModel = true;
	
	private Object factoryKey;
	
	/**
	 * Provides all entities of the given class.
	 */
	public DatabinderProvider(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	/**
	 * Provides entities of the given class meeting the supplied criteria.
	 */
	public DatabinderProvider(Class objectClass, ICriteriaBuilder criteriaBuilder) {
		this(objectClass);
		this.criteriaBuilder = criteriaBuilder;
	}

	/**
	 * Provides entities of the given class meeting the supplied critiera and sort criteria. The
	 * sort criteria is not applied to the count query (as it should not affect the count). When returning
	 * results it is applied in addition to the standard criteria, so it is not necessary to duplicate
	 * filter or other criteria in the sort criteria.
	 * @param criteriaBuilder standard criteria applied to both the count and actual results
	 * @param sortCriteriaBuilder sort criteria applied only to the actual results
	 */
	public DatabinderProvider(Class objectClass, ICriteriaBuilder criteriaBuilder, ICriteriaBuilder sortCriteriaBuilder) {
		this(objectClass, criteriaBuilder);
		this.sortCriteriaBuilder = sortCriteriaBuilder;
	}
	
	/**
	 * Provides entities matching the given query. The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public DatabinderProvider(String query) {
		this.queryString = query;
	}
	
	/**
	 * Provides entities matching the given query with bound parameters.  The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public DatabinderProvider(String query, IQueryBinder queryBinder) {
		this(query);
		this.queryBinder = queryBinder;
	}

	/**
	 * Provides entities matching the given queries with bound parameters.
	 * @param query query to return entities
	 * @param queryBinder binder for the standard query
	 * @param countQuery query to return count of entities
	 * @param countQueryBinder binder for the count query
	 */
	public DatabinderProvider(String query, IQueryBinder queryBinder, String countQuery, IQueryBinder countQueryBinder) {
		this(query, queryBinder);
		this.countQueryString = countQuery;
		this.countQueryBinder = countQueryBinder;
	}

	public Object getFactoryKey() {
		return factoryKey;
	}

	DatabinderProvider setFactoryKey(Object key) {
		this.factoryKey = key;
		return this;
	}
	
	/** Used only by deprecated subclasses. Please ignore.  */
	protected void setCriteriaBuilder(ICriteriaBuilder criteriaBuilder) {
		this.criteriaBuilder = criteriaBuilder;
	}

	/** Used only by deprecated subclasses. Please ignore.  */
	protected void setSortCriteriaBuilder(ICriteriaBuilder sortCriteriaBuilder) {
		this.sortCriteriaBuilder = sortCriteriaBuilder;
	}
	
	/** Used only by deprecated subclasses. Please ignore.  */
	protected void setQueryBinder(IQueryBinder queryBinder) {
		this.queryBinder = queryBinder;
	}

	/**
	 * It should not be necessary to override (or call) this default implementation.
	 */
	public final Iterator iterator(int first, int count) {
		Session sess =  DataStaticService.getHibernateSession(factoryKey);
		
		if (queryString != null) {
			org.hibernate.Query q = sess.createQuery(queryString);
			if (queryBinder != null)
				queryBinder.bind(q);
			q.setFirstResult(first);
			q.setMaxResults(count);
			return q.iterate();
		}
		
		Criteria crit = sess.createCriteria(objectClass);
		if (criteriaBuilder != null) {
			criteriaBuilder.build(crit);
			if (sortCriteriaBuilder != null)
				sortCriteriaBuilder.build(crit);
		}
		crit.setFirstResult(first);
		crit.setMaxResults(count);
		return crit.list().iterator();
	}
	
	/**
	 * It should not be necessary to override (or call) this default implementation.
	 */
	public final int size() {
		Session sess =  DataStaticService.getHibernateSession(factoryKey);

		if (countQueryString != null) {
			Query query = sess.createQuery(countQueryString);
			if (countQueryBinder != null)
				countQueryBinder.bind(query);
			return ((Long) query.uniqueResult()).intValue();
		}
		if (queryString != null) {
			String synthCountQuery = "select count(*) " + queryString;
			Query query = sess.createQuery(synthCountQuery);
			if (queryBinder != null)
				queryBinder.bind(query);
			return ((Long) query.uniqueResult()).intValue();
		}
		
		Criteria crit = sess.createCriteria(objectClass);
		
		if (criteriaBuilder != null)
			criteriaBuilder.build(crit);
		crit.setProjection(Projections.rowCount());
		Integer size = (Integer) crit.uniqueResult();
		return size == null ? 0 : size;
	}

	/**
	 * @return true if wrapping items with a compound property model (the default)
	 */
	public boolean getWrapWithPropertyModel() {
		return wrapWithPropertyModel;
	}

	/**
	 * @param wrapInCompoundModel false to not wrap items in a compound property model (true is default)
	 */
	public void setWrapWithPropertyModel(boolean wrapInCompoundModel) {
		this.wrapWithPropertyModel = wrapInCompoundModel;
	}

	/**
	 * Wraps object in HiberanteObjectModel, and also BoundCompoundPropertyModel if 
	 * wrapInCompoundModel is true.
	 * @param object object DataView would like to wrap
	 * @return object wrapped in a HibernateObjectModel and possibly BoundCompoundPropertyModel
	 */
	public IModel model(Object object) {
		HibernateObjectModel model = new HibernateObjectModel(object);
		return  wrapWithPropertyModel ? new BoundCompoundPropertyModel(model) : model;
	}
	
	/**
	 * Please use the base DatabinderProvider with independent criteria builders. This subclass
	 * will be removed in a future release.
	 * @deprecated
	 */
	public static abstract class CriteriaBuilder extends DatabinderProvider implements ICriteriaBuilder {
		public CriteriaBuilder(Class objectClass) {
			super(objectClass);
			setCriteriaBuilder(this);
			setSortCriteriaBuilder(new ICriteriaBuilder() {
				public void build(Criteria criteria) {
					sort(criteria);
				}
			});
			setWrapWithPropertyModel(false);
		}
		protected void sort(Criteria crit) {
		}
	}
	/**
	 * Please use the base DatabinderProvider with independent query binders. This subclass
	 * will be removed from a future release.
	 * @deprecated
	 */
	public static abstract class QueryBinder extends DatabinderProvider implements IQueryBinder {
		String queryString;
		
		public QueryBinder(String queryString) {
			super(queryString);
			setQueryBinder(this);
			setWrapWithPropertyModel(false);
		}
		
		/**
		 * This method will not be called. Please use the base DatabinderProvider.
		 */
		protected final String count (String queryString) {
			return null;
		}
		
		/**
		 * This method will not be called. Please use the base DatabinderProvider.
		 */
		protected final String sort(String queryString) {
			return null;
		}
	}
	
	/** This provider has nothing to detach. */
	public void detach() {
		
	}
}