package net.databinder.conv.components;

import net.databinder.components.DataPage;

import org.hibernate.classic.Session;

public abstract class DataConversationPage extends DataPage implements IConversationPage {

	private Session conversationSession;
	
	public DataConversationPage() {
	}
	
	public DataConversationPage(Session conversationSession) {
		this.conversationSession = conversationSession;
	}

	public Session getConversationSession() {
		return conversationSession;
	}

	public void setConversationSession(Session conversationSession) {
		this.conversationSession = conversationSession;
	}
}
