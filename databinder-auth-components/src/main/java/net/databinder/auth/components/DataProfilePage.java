package net.databinder.auth.components;

import net.databinder.components.DataStyleLink;

import org.apache.wicket.markup.html.WebPage;

public class DataProfilePage extends WebPage {
	public DataProfilePage() {
		add(new DataStyleLink("dataStylesheet"));
		add(new DataProfilePanel("registerPanel"));
	}
}
