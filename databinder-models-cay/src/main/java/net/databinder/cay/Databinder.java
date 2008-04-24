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
package net.databinder.cay;

import org.apache.cayenne.access.DataContext;
import org.apache.wicket.RequestCycle;

/** Provides access to Cayenne context for this thread. */
public class Databinder {
	/** @return Cayenne context (possbly new) for this thread. */
	public static DataContext getContext() {
		RequestCycle cycle = RequestCycle.get();
		if (cycle instanceof CayenneRequestCycle)
			((CayenneRequestCycle)cycle).contextRequested();
		return DataContext.getThreadDataContext();
	}
}
