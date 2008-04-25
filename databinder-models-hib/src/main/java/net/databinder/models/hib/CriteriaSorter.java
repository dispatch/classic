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

import java.io.Serializable;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;

/**
 * <h1>CriteriaSorter</h1>
 * <i>Copyright (C) 2008 The Scripps Research Institute</i>
 * <p>A Criteria based sorter suitable for adding to a HibernateProvider</p> 
 *  * <pre>
 * // a default sort by name, ascending and case insensitive: 
 * CriteriaSorter sorter = new CriteriaSorter("name",true,false); 
 * IDataProvider provider = new DatabinderProvider(objectClass, criteriaBuilder, sorter);
 * </pre>
 *  * @author Mark Southern (southern at scripps dot edu)
 */
public class CriteriaSorter implements ISortStateLocator, CriteriaBuilder, Serializable {

    private SingleSortState sortState;

    private String defaultProperty = null;

    boolean asc, cased;

    public CriteriaSorter() {
        this(null, true, true);
    }

    public CriteriaSorter(String defaultProperty) {
        this(defaultProperty, true, true);
    }

    public CriteriaSorter(String defaultProperty, boolean asc) {
        this(defaultProperty, asc, true);
    }

    /**
     * @param defaultProperty - property for a default sort before any is set
     * @param asc - sort ascending/descending
     * @param cased - sort cased/case insensitive
     */
    public CriteriaSorter(String defaultProperty, boolean asc, boolean cased) {
        sortState = new SingleSortState();
        this.defaultProperty = defaultProperty;
        this.asc = asc;
        this.cased = cased;
    }

    public void build(Criteria criteria) {
        SortParam sort = sortState.getSort();
        String property;
        if (sort != null && sort.getProperty() != null) {
            property = sort.getProperty();
            asc = sort.isAscending();
        }
        else {
            property = defaultProperty;
        }
        if (property != null) {
            if (property.contains(".")) {
                // for 'dot' properties we need to add aliases
                // e.g. for the property 'orderbook.order.item.name' we need to add an aliases for 'order' and 'order.item'
                String path[] = property.split("\\.");
                for (int ii = 0; ii < path.length - 1; ii++) {
                    StringBuffer sb = new StringBuffer();
                    for (int jj = 0; jj <= ii; jj++) {
                        if (sb.length() > 0)
                            sb.append(".");
                        sb.append(path[jj]);
                    }
                    criteria.createAlias(sb.toString(), path[ii], CriteriaSpecification.LEFT_JOIN);
                }
                // when we have a 'dot' property we want to sort by the sub tables field
                // e.g. for the property 'orderbook.order.item.name' we need to sort by 'item.name'
                if (path.length > 1)
                    property = String.format("%s.%s", path[path.length - 2], path[path.length - 1]);
                else
                    property = path[path.length - 1];
            }
            Order order = asc ? Order.asc(property) : Order.desc(property);
            order = cased ? order : order.ignoreCase();
            criteria.addOrder(order);
        }
    }

    public ISortState getSortState() {
        return sortState;
    }

    public void setSortState(ISortState state) {
        sortState = (SingleSortState) state;
    }
}