package net.databinder;

import java.util.regex.Pattern;

import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExceptionFilteringRequestCycle extends WebRequestCycle {
	
	private static final Logger log = LoggerFactory.getLogger(ExceptionFilteringRequestCycle.class);
	private static Pattern warnOnlySource = Pattern.compile(".*UrlCodingStrategy");
	
	public ExceptionFilteringRequestCycle(WebApplication application, WebRequest request, Response response) {
		super(application, request, response);
	}
	
	@Override
	protected void logRuntimeException(RuntimeException e) {
		if (warnOnlySource.matcher(e.getStackTrace()[0].getClassName()).matches())
			log.warn(e.getMessage(), e);
		else
			super.logRuntimeException(e);
	}
	
	public static void setWarnOnlySource(Pattern pattern) {
		warnOnlySource = pattern;
	}
}
