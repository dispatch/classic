package net.databinder.conv.components;

import org.hibernate.classic.Session;

import wicket.markup.html.WebPage;

public abstract class DataConversationPage extends WebPage implements IConversationPage {

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
