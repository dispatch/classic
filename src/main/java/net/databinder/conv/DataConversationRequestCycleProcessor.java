package net.databinder.conv;

import wicket.IRequestTarget;
import wicket.RequestCycle;
import wicket.protocol.http.DefaultWebRequestCycleProcessor;
import wicket.request.RequestParameters;
import wicket.request.compound.DefaultRequestTargetResolverStrategy;
import wicket.request.compound.IRequestTargetResolverStrategy;

public class DataConversationRequestCycleProcessor extends DefaultWebRequestCycleProcessor {
	
	@Override
	protected IRequestTargetResolverStrategy newRequestTargetResolverStrategy() {
		return new DefaultRequestTargetResolverStrategy() {
			// TODO this method is final in Wicket
			@Override
			public IRequestTarget resolve(RequestCycle requestCycle, RequestParameters requestParameters) {
				IRequestTarget target = super.resolve(requestCycle, requestParameters);
				RequestCycle cycle = RequestCycle.get();
				if (cycle instanceof DataConversationRequestCycle)
					((DataConversationRequestCycle) cycle).openSessionFor(target);
				return target;
			}
		};
	}
}
