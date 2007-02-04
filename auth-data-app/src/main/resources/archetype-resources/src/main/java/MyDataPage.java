package $package;

import wicket.markup.html.WebPage;
import wicket.markup.html.panel.FeedbackPanel;

public class MyDataPage extends WebPage {
	
	/**
	 * Add components to page.
	 */
	public MyDataPage() {
		super();
		add(new FeedbackPanel("feedback"));
	}
}
