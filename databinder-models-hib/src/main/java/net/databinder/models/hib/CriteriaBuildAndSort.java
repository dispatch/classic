package net.databinder.models.hib;

/*---
 Copyright 2008 The Scripps Research Institute
 http://www.scripps.edu
 
* Databinder: a simple bridge from Wicket to Hibernate
*
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
 ---*/

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SingleSortState;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;

/**
 * Abstract base class for building OrderedCriteriaBuilders. It handles the sorting.
 * Subclasses should call super.buildUnordered() when overriding.
 * 
 * Avoids problems with duplicate Aliases by having all the Criteria building code in one location.
 * 
 * @author Mark Southern
 */
public abstract class CriteriaBuildAndSort implements ISortStateLocator, OrderingCriteriaBuilder, Serializable {

    private Set<String> aliases = new HashSet<String>();

    private SingleSortState sortState = new SingleSortState();

    private String defaultSortProperty = null;

    boolean sortAscending, sortCased;

    public CriteriaBuildAndSort(String defaultSortProperty, boolean sortAscending, boolean sortCased) {
        this.defaultSortProperty = defaultSortProperty;
        this.sortAscending = sortAscending;
        this.sortCased = sortCased;
    }

    protected String processProperty(Criteria criteria, String property) {
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
                if (!aliases.contains(path[ii])) {
                    aliases.add(path[ii]);
                    criteria.createAlias(sb.toString(), path[ii], CriteriaSpecification.LEFT_JOIN);
                }
            }
            // when we have a 'dot' property we want to sort by the sub tables field
            // e.g. for the property 'orderbook.order.item.name' we need to sort by 'item.name'
            if (path.length > 1)
                property = String.format("%s.%s", path[path.length - 2], path[path.length - 1]);
            else
                property = path[path.length - 1];
        }
        return property;
    }

    public void buildOrdered(Criteria criteria) {
        buildUnordered(criteria);

        SortParam sort = sortState.getSort();
        String property;
        if (sort != null && sort.getProperty() != null) {
            property = sort.getProperty();
            sortAscending = sort.isAscending();
        }
        else
            property = defaultSortProperty;

        if (property != null) {
            property = processProperty(criteria, property);
            Order order = sortAscending ? Order.asc(property) : Order.desc(property);
            order = sortCased ? order : order.ignoreCase();
            criteria.addOrder(order);
        }
    }

    public void buildUnordered(Criteria criteria) {
        aliases.clear();
    }

    public ISortState getSortState() {
        return sortState;
    }

    public void setSortState(ISortState state) {
        sortState = (SingleSortState) state;
    }
}