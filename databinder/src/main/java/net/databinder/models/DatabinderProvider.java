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

import wicket.markup.repeater.data.IDataProvider;

/**
 * Provides base functionality for DataView IDataProvider. Because Databinder does not link
 * to wicket-extensions, this class does not declare its implementation of IDataProvider.
 * However, it implements all methods of that interface and should make a task-specific
 * IDataProvider implementation considerably easier to build. Internal subclasses
 * are providers for both standard and criteria queries.
 * @see wicket.extensions.markup.html.repeater.data.IDataProvider  
 * @author Nathan Hamblen
 */
public abstract class DatabinderProvider implements IDataProvider  {
	/**
	 * @param object object DataView would like to wrap
	 * @return object wrapped in a HibernateObjectModel
	 */
	public HibernateObjectModel model(Object object) {
		return new HibernateObjectModel(object);
	}
	
	/**
	 * A data provider controlled by the Hibernate Criteria API. Subclass must provide the
	 * implementation for ICriteriaBuilder.
	 */
	public static abstract class CriteriaBuilder extends DatabinderProvider implements ICriteriaBuilder {
		private Class objectClass;
		
		/**
		 * @param objectClass type of objects returned by this provider
		 */
		public CriteriaBuilder(Class objectClass) {
			this.objectClass = objectClass;
		}
		
		/**
		 * It should not be necessary to override (or call) this default implementation.
		 *  See the sort() method for sortable providers.
		 */
		public final Iterator iterator(int first, int count) {
			Session sess =  DataStaticService.getHibernateSession();
			
			Criteria crit = sess.createCriteria(objectClass);
			build(crit);
			sort(crit);
			crit.setFirstResult(first);
			crit.setMaxResults(count);
	
			return crit.list().iterator();
		}
		
		/**
		 * It should not be necessary to override (or call) this default implementation.
		 */
		public final int size() {
			Session sess =  DataStaticService.getHibernateSession();
	
			Criteria crit = sess.createCriteria(objectClass);
			build(crit);
			crit.setProjection(Projections.rowCount());
			return (Integer) crit.uniqueResult();
		}
		
		/**
		 * Override this method if your provider is sortable. It will be called when building 
		 * the actual results (and not the count).
		 * @param crit criteria to receive ordering
		 */
		protected void sort(Criteria crit) {
		}
	}
	
	/**
	 * A data provider controlled by a Hibernate query. Subclass must provide the
	 * implementation for IQueryBinder.
	 */
	public static abstract class QueryBinder extends DatabinderProvider implements IQueryBinder {
		String queryString;
		
		/**
		 * @param queryString Base string, including any parameters, for query.
		 */
		public QueryBinder(String queryString) {
			this.queryString = queryString;
		}
		
		/**
		 * It should not be necessary to override (or call) this default implementation.
		 *  See the sort() method for sortable providers.
		 */
		public final Iterator iterator(int first, int count) {
			Session sess =  DataStaticService.getHibernateSession();
			
			Query query = sess.createQuery(sort(queryString));
			bind(query);
			query.setFirstResult(first);
			query.setMaxResults(count);
	
			return query.list().iterator();
		}
		
		/**
		 * It should not be necessary to override (or call) this default implementation.
		 */
		public final int size() {
			Session sess =  DataStaticService.getHibernateSession();
	
			Query query = sess.createQuery(count(queryString));
			bind(query);
			return ((Long) query.uniqueResult()).intValue();
		}

		/**
		 * Modify query to return row count. This implementation prepends <tt>select count(*)</tt>
		 * to the query, which will work only if the base query string has no select clause. Override
		 * this method if necessary, and note that the base query string may be ignored if 
		 * appropriate. 
		 * @param queryString the base query string for this provider
		 * @return query stirng returning row count only
		 */
		protected String count (String queryString) {
			return "select count(*) " + queryString;
		}
		
		/**
		 * Override this method if your provider is sortable. It will be called when building 
		 * the actual results (and not the count). The base query string is provided for your
		 * convenience.
		 * @param queryString the base query string for this provider
		 * @return query string including any ordering clause
		 */
		protected String sort(String queryString) {
			return queryString;
		}
	}
	/** This class has nothing to detach. */
	public void detach() {
		
	}
}