package net.databinder.cay;

import net.databinder.CookieRequestCycle;

import org.apache.cayenne.access.DataContext;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;

public class DataRequestCycle extends CookieRequestCycle implements CayenneRequestCycle {
	private DataContext context;
	public DataRequestCycle(WebApplication app, WebRequest request, Response response) {
		super(app, request, response);
	}
	public void contextRequested() {
		if (context == null) {
			context = DataContext.createDataContext();
			DataContext.bindThreadDataContext(context);
		}
	}
	@Override
	protected void onEndRequest() {
		if (context != null) {
			if (context.hasChanges())
				context.rollbackChanges();
			context = null;
			DataContext.bindThreadDataContext(null);
		}
	}
}
