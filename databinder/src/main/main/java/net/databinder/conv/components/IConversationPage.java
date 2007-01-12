package net.databinder.conv.components;

import org.hibernate.classic.Session;

public interface IConversationPage {
	public Session getConversationSession();

	public void setConversationSession(Session conversationSession);

}
