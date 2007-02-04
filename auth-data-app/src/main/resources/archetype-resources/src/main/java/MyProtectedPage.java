package $package;

import wicket.markup.html.WebPage;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.authorization.strategies.role.Roles;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;

@AuthorizeInstantiation(Roles.USER)
public class MyProtectedPage extends WebPage {
	
	/**
	 * Add components to page.
	 */
	public MyProtectedPage() {
		super();
		add(new FeedbackPanel("feedback"));
	}
}
