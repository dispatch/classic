package net.databinder.conv;

import org.hibernate.HibernateException;

import wicket.IRequestTarget;
import wicket.RequestCycle;
import wicket.WicketRuntimeException;
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
				IRequestTarget target;
				try {
					target = super.resolve(requestCycle, requestParameters);
				} catch (HibernateException e) {
					throw new WicketRuntimeException("Tried to load object before session binding, while determining request target", e);
				}
				RequestCycle cycle = RequestCycle.get();
				if (cycle instanceof DataConversationRequestCycle)
					((DataConversationRequestCycle) cycle).openSessionFor(target);
				return target;
			}
		};
	}
}
