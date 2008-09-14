package net.databinder.models.hib;

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

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.util.lang.PropertyResolver;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;

/**
 * <h1>CriteriaColumnFilter</h1>
 * <i>Copyright (C) 2008 The Scripps Research Institute</i>
 * <p>An implementor of CriteriaBuilder and IFilterStateLocator to wire up a HibernateProvider based DataTable with a FilterToolbar<p>
 * 
 * <pre>
 * //...
 * CriteriaColumnFilter filter = new CriteriaColumnFilter(objectClass.getClass().newInstance(), columns);
 * FilterForm form = new FilterForm("form", filter);
 * //...
 * IDataProvider provider = new DatabinderProvider(objectClass, filter, new DataSorter() );
 * DataTable table = new DataTable("table", columns, provider, 25);
 * //...
 * table.addTopToolbar(new FilterToolbar(table, form, filter));
 * </pre>
 * 
 * @author Mark Southern (southern at scripps dot edu)
 * @deprecated Use CriteriaFilterAndSort instead.
 */
@Deprecated
public class CriteriaColumnFilter implements CriteriaBuilder, IFilterStateLocator {

    private IColumn[] columns;

    private Object bean;

    public CriteriaColumnFilter(Object bean, IColumn[] columns) {
        this.bean = bean;
        this.columns = columns;
    }

    public void build(Criteria criteria) {
        for (IColumn col : columns) {
            if (col instanceof PropertyColumn) {
                PropertyColumn propCol = (PropertyColumn) col;
                String property = propCol.getPropertyExpression();
                Object value = PropertyResolver.getValue(property, bean);
                if (value != null) {
                    if (value instanceof String)
                        criteria.add(Restrictions.ilike(property, (String) value, MatchMode.ANYWHERE));
                    else
                        criteria.add(Restrictions.eq(property, value));
                }

            }
        }
    }

    public Object getFilterState() {
        return bean;
    }

    public void setFilterState(Object bean) {
        this.bean = bean;
    }
}