package $package;

import wicket.Component;
import wicket.markup.html.WebPage;
import wicket.markup.html.basic.Label;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.AbstractReadOnlyModel;

public class MyDataPage extends WebPage {
	
	/**
	 * Add components to page.
	 */
	public MyDataPage() {
		super();
		add(new FeedbackPanel("feedback"));
	}
}
