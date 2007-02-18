package $package;

import wicket.markup.html.WebPage;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.resources.StyleSheetReference;

public class MyDataPage extends WebPage {
	
	/**
	 * Add components to page.
	 */
	public MyDataPage() {
		super();
		add(new StyleSheetReference("data-app", MyDataPage.class, "data-app.css"));
		add(new FeedbackPanel("feedback"));
	}
}
