package net.databinder.conv.components;


import org.hibernate.classic.Session;

import wicket.markup.html.WebPage;

public abstract class ConversationPage extends WebPage implements IConversationPage {
	private Session conversationSession;
	
	public ConversationPage() {
	}
	
	public ConversationPage(Session conversationSession) {
		this.conversationSession = conversationSession;
	}

	public Session getConversationSession() {
		return conversationSession;
	}

	public void setConversationSession(Session conversationSession) {
		this.conversationSession = conversationSession;
	}

}
