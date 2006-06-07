package net.databinder.components;

public class DataSignInPage extends DataPage {

	@Override
	protected String getName() {
		return "Please sign in";
	}
	
	public DataSignInPage() {
		add(new DataSignInPanel("signIn"));
	}

}
