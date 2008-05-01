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
import net.databinder.models.PropertyDataProvider;

import org.apache.wicket.model.IModel;
import org.hibernate.Criteria;
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
	private OrderingCriteriaBuilder criteriaBuilder;
	private QueryBuilder queryBuilder, countQueryBuilder;
	
	private Object factoryKey;
	
	/**
	 * Provides all entities of the given class.
	 */
	public HibernateProvider(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	/**
	 * Provides all entities of the given class using a distinct criteria builder for the order query.
	 * @param objectClass
	 * @param criteriaBuilder base criteria builder
	 * @param criteriaOrderer add ordering information ONLY, base criteria will be called first
	 */
	public HibernateProvider(Class objectClass, final CriteriaBuilder criteriaBuilder, final CriteriaBuilder criteriaOrderer) {
		this(objectClass);
		this.criteriaBuilder = new OrderingCriteriaBuilder() {
			public void buildOrdered(Criteria criteria) {
				criteriaBuilder.build(criteria);
				criteriaOrderer.build(criteria);
			}
			public void buildUnordered(Criteria criteria) {
				criteriaBuilder.build(criteria);
			}
		};
	}
	
	/**
	 * Provides all entities of the given class.
	 * @param objectClass
	 * @param criteriaBuider builds different criteria objects for iterator() and size()
	 */
	public HibernateProvider(Class objectClass, OrderingCriteriaBuilder criteriaBuider) {
		this(objectClass);
		this.criteriaBuilder = criteriaBuider;
	}

	/** Provides entities of the given class meeting the supplied criteria. */
	public HibernateProvider(Class objectClass, final CriteriaBuilder criteriaBuilder) {
		this(objectClass, new OrderingCriteriaBuilder() {
			public void buildOrdered(Criteria criteria) {
				criteriaBuilder.build(criteria);
			}
			public void buildUnordered(Criteria criteria) {
				criteriaBuilder.build(criteria);
			}
		});
	}

	/**
	 * Provides entities matching the given query. The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public HibernateProvider(String query) {
		this(query, makeCount(query));
	}
	
	/**
	 * Provides entities matching the given queries.
	 */
	public HibernateProvider(final String query, final String countQuery) {
		this(new QueryBinderBuilder(query), new QueryBinderBuilder(countQuery));
	}
	
	/**
	 * Provides entities matching the given query with bound parameters.  The count query
	 * is derived by prefixing "select count(*)" to the given query; this will fail if 
	 * the supplied query has a select clause.
	 */
	public HibernateProvider(String query, QueryBinder queryBinder) {
		this(query, queryBinder, makeCount(query), queryBinder);
	}

	/**
	 * Provides entities matching the given queries with bound parameters.
	 * @param query query to return entities
	 * @param queryBinder binder for the standard query
	 * @param countQuery query to return count of entities
	 * @param countQueryBinder binder for the count query (may be same as queryBinder)
	 */
	public HibernateProvider(final String query, final QueryBinder queryBinder, final String countQuery, final QueryBinder countQueryBinder) {
		this(new QueryBinderBuilder(query, queryBinder), new QueryBinderBuilder(countQuery, countQueryBinder));
	}
	
	public HibernateProvider(QueryBuilder queryBuilder, QueryBuilder countQueryBuilder) {
		this.queryBuilder = queryBuilder;
		this.countQueryBuilder = countQueryBuilder;
	}

	/** @return query with select count(*) prepended */
	static protected String makeCount(String query) {
		return "select count(*) " + query;
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
	 * It should not normally be necessary to override (or call) this default implementation.
	 */
	public Iterator iterator(int first, int count) {
		Session sess =  Databinder.getHibernateSession(factoryKey);
		
		if(queryBuilder != null) {
			org.hibernate.Query q = queryBuilder.build(sess);
			q.setFirstResult(first);
			q.setMaxResults(count);
			return q.iterate();
		}			
		
		Criteria crit = sess.createCriteria(objectClass);
		if (criteriaBuilder != null)
			criteriaBuilder.buildOrdered(crit);
		
		crit.setFirstResult(first);
		crit.setMaxResults(count);
		return crit.list().iterator();
	}
	
	/**
	 * Only override this method if a single count query or 
	 * criteria projection is not possible.
	 */
	public int size() {
		Session sess =  Databinder.getHibernateSession(factoryKey);

		if(countQueryBuilder != null) {
			org.hibernate.Query q = countQueryBuilder.build(sess);
			Object obj = q.uniqueResult();
			return ((Number) obj).intValue();
		}
		
		Criteria crit = sess.createCriteria(objectClass);
		
		if (criteriaBuilder != null)
			criteriaBuilder.buildUnordered(crit);
		crit.setProjection(Projections.rowCount());
		Integer size = (Integer) crit.uniqueResult();
		return size == null ? 0 : size;
	}


	@Override
	protected IModel dataModel(Object object) {
		return new HibernateObjectModel(object);
	}
	
	/** does nothing */
	public void detach() {
	}
}
