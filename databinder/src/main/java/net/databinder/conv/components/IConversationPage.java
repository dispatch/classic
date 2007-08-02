package net.databinder.conv.components;

import org.hibernate.classic.Session;

public interface IConversationPage {
	public Session getConversationSession(Object key);
	public void setConversationSession(Object key, Session conversationSession);
}
