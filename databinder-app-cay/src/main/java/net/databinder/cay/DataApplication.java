package net.databinder.cay;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebRequest;

import net.databinder.DataApplicationBase;

public abstract class DataApplication extends DataApplicationBase {

	@Override
	protected void dataInit() { }
	
	@Override
	public RequestCycle newRequestCycle(Request request, Response response) {
		return new DataRequestCycle(this, (WebRequest) request, response);
	}
}
