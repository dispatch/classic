package net.databinder.components;

import wicket.markup.ComponentTag;
import wicket.markup.html.WebPage;
import wicket.markup.html.form.TextField;
import wicket.model.IModel;

/**
 * TextField that can be told to focus itself on the next request.  Works in cojunction with 
 * the onload handler. 
 */
public class FocusableTextField extends TextField {
	private boolean wantsFocus = false;
	/**
	 * @param id Wicket id
	 * @param model text field model
	 * @param page page that will receive component
	 */
	public FocusableTextField(String id, IModel model, WebPage page) {
		super (id, model);
		add(ScriptLink.headerContributor(FocusableTextField.class));
		page.getBodyContainer().addOnLoadModifier("initFocusableTextField();", this);
	}
	/**
	 * @param id Wicket id
	 * @param page page that will receive component
	 */
	public FocusableTextField(String id, WebPage page) {
		super (id);
		add(ScriptLink.headerContributor(FocusableTextField.class));
		page.getBodyContainer().addOnLoadModifier("initFocusableTextField();", this);
	}
	/**
	 * Request focus on next rendering.
	 */
	public void requestFocus() {
		wantsFocus = true;
	}
	/** Adds flagging id attribute if focus has been requested. */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		if (wantsFocus) {
			tag.put("id", "focusMe");
			wantsFocus = false;
		}
		super.onComponentTag(tag);
	}
}