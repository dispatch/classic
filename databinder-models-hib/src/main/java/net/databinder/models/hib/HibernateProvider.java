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
package net.databinder.models.hib;

import java.util.Iterator;

import net.databinder.hib.Databinder;
import net.databinder.models.Models;
import net.databinder.models.PropertyDataProvider;

import org.apache.wicket.model.IModel;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

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
public class HibernateProvider extends PropertyDataProvider  {
	private Class objectClass;
	private CriteriaBuilder criteriaBuilder, sortCriteriaBuilder;
	private String queryString, countQueryString;
	private QueryBinder queryBinder, countQueryBinder;
	private QueryBuilder queryBuilder, countQueryBuilder;
	
	private Object factoryKey;
	
	/**
	 * Provides all entities of the given class.
	 */
	public HibernateProvider(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	/**
	 * Provides entities of the given class meeting the supplied criteria.
	 */
	public HibernateProvider(Class objectClass, CriteriaBuilder criteriaBuilder) {
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
	public HibernateProvider(Class objectClass, CriteriaBuilder criteriaBuilder, CriteriaBuilder sortCriteriaBuilder) {
		this(objectClass, criteriaBuilder);
		this.sortCriteriaBuilder = sortCriteriaBuilder;
	}
	
	/**
	 * Provides entities matching the given query. The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public HibernateProvider(String query) {
		this.queryString = query;
	}
	
	/**
	 * Provides entities matching the given queries.
	 */
	public HibernateProvider(String query, String countQuery) {
		this.queryString = query;
		this.countQueryString = countQuery;
	}
	
	/**
	 * Provides entities matching the given query with bound parameters.  The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public HibernateProvider(String query, QueryBinder queryBinder) {
		this(query);
		this.queryBinder = queryBinder;
	}

	/**
	 * Provides entities matching the given queries with bound parameters.
	 * @param query query to return entities
	 * @param queryBinder binder for the standard query
	 * @param countQuery query to return count of entities
	 * @param countQueryBinder binder for the count query (may be same as queryBinder)
	 */
	public HibernateProvider(String query, QueryBinder queryBinder, String countQuery, QueryBinder countQueryBinder) {
		this(query, queryBinder);
		this.countQueryString = countQuery;
		this.countQueryBinder = countQueryBinder;
	}
	
	public HibernateProvider(QueryBuilder queryBuilder, QueryBuilder countQueryBuilder) {
		this.queryBuilder = queryBuilder;
		this.countQueryBuilder = countQueryBuilder;
	}

	/** @return session factory key, or null for the default factory */
	public Object getFactoryKey() {
		return factoryKey;
	}

	/**
	 * Set a factory key other than the default (null).
	 * @param key session factory key
	 * @return this, for chaining
	 */
	public HibernateProvider setFactoryKey(Object key) {
		this.factoryKey = key;
		return this;
	}
	
	/**
	 * It should not be necessary to override (or call) this default implementation.
	 */
	public final Iterator iterator(int first, int count) {
		Session sess =  Databinder.getHibernateSession(factoryKey);
		
		if(queryBuilder != null) {
			org.hibernate.Query q = queryBuilder.build(sess);
			q.setFirstResult(first);
			q.setMaxResults(count);
			return q.iterate();
		}			
		
		if (queryString != null) {
			org.hibernate.Query q = sess.createQuery(queryString);
			if (queryBinder != null)
				queryBinder.bind(q);
			q.setFirstResult(first);
			q.setMaxResults(count);
			return q.iterate();
		}
		
		Criteria crit = sess.createCriteria(objectClass);
		if (criteriaBuilder != null)
			criteriaBuilder.build(crit);
		
		if (sortCriteriaBuilder != null)
			sortCriteriaBuilder.build(crit);
		
		crit.setFirstResult(first);
		crit.setMaxResults(count);
		return crit.list().iterator();
	}
	
	/**
	 * It should not be necessary to override (or call) this default implementation.
	 */
	public final int size() {
		Session sess =  Databinder.getHibernateSession(factoryKey);

		if(countQueryBuilder != null) {
			org.hibernate.Query q = countQueryBuilder.build(sess);
			Object obj = q.uniqueResult();
			return ((Number) obj).intValue();
		}
		
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


	@Override
	protected IModel dataModel(Object object) {
		return new HibernateObjectModel(object);
	}
	
	/** Detaches query binder if needed. */
	public void detach() {
		Models.checkDetach(queryBinder, countQueryBinder);
	}
}
