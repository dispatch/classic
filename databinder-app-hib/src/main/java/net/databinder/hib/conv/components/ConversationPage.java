package net.databinder.hib.conv.components;


import java.util.HashMap;

import org.apache.wicket.markup.html.WebPage;
import org.hibernate.classic.Session;

public class ConversationPage extends WebPage implements IConversationPage {
	private HashMap<Object, Session> conversationSessions = new HashMap<Object, Session>();
	
	public ConversationPage() {
	}
	
	public ConversationPage(Session conversationSession) {
		setConversationSession(null);
	}

	public ConversationPage(Object key, Session conversationSession) {
		setConversationSession(key, conversationSession);
	}
	
	public Session getConversationSession(Object key) {
		return conversationSessions.get(key);
	}
	
	public void setConversationSession(Object key, Session conversationSession) {
		conversationSessions.put(key, conversationSession);
	}

	public Session getConversationSession() {
		return getConversationSession(null);
	}

	public void setConversationSession(Session conversationSession) {
		setConversationSession(null, conversationSession);
	}
}
