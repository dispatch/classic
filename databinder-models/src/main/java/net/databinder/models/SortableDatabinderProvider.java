package net.databinder.models;

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

import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.*;

/**
 * <h1>SortableDatabinderProvider</h1>
 * <i>Copyright (C) 2008 The Scripps Research Institute</i>
 * <p>A DatabinderProvider extension that implements ISortableDataProvider so it can be used with a DefaultDataTable or an AjaxFallbackDefaultDataTable
 * The ICriteriaBuilder that handles the sorting should also implement ISortStateLocator (such as CriteriaSorter).</p>
 * 
 * @author Mark Southern (southern at scripps dot edu)
 * @todo To be a complete extension of DatabinderProvider, support for IQueryBinder/IQueryBuilder must be added and all of the DatabinderProvider constructors covered 
 */

public class SortableDatabinderProvider extends DatabinderProvider implements ISortableDataProvider {

    private ISortStateLocator sortStateLocator = null;

    private ISortState sortState;

    public SortableDatabinderProvider(Class<?> objectClass, ICriteriaBuilder criteriaBuilder, ICriteriaBuilder sortCriteriaBuilder) {
        super(objectClass, criteriaBuilder, sortCriteriaBuilder);
        if (sortCriteriaBuilder instanceof ISortStateLocator)
            sortStateLocator = (ISortStateLocator) sortCriteriaBuilder;
    }

    public ISortState getSortState() {
        return (sortStateLocator != null) ? sortStateLocator.getSortState() : sortState;
    }

    public void setSortState(ISortState state) {
        if (sortStateLocator != null) {
            sortStateLocator.setSortState(state);
        }
        else {
            this.sortState = state;
        }
    }
}
