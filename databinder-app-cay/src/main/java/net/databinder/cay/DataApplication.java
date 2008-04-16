package net.databinder.cay;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebRequest;

import net.databinder.DataApplicationBase;

/**
 * Application base for Cayenne.
 */
public abstract class DataApplication extends DataApplicationBase {

	/** Does nothing, no init required. */
	@Override
	protected void dataInit() { }
	
	/** Returns DataRequestCycle instance for Cayenne. */
	@Override
	public RequestCycle newRequestCycle(Request request, Response response) {
		return new DataRequestCycle(this, (WebRequest) request, response);
	}
}
