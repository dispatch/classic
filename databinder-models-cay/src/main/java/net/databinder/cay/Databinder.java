package net.databinder.cay;

import org.apache.cayenne.access.DataContext;
import org.apache.wicket.RequestCycle;

public class Databinder {
	public static DataContext getContext() {
		RequestCycle cycle = RequestCycle.get();
		if (cycle instanceof CayenneRequestCycle)
			((CayenneRequestCycle)cycle).contextRequested();
		return DataContext.getThreadDataContext();
	}
}
