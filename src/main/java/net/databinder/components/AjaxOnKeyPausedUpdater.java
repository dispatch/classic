package net.databinder.components;

import wicket.Response;
import wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import wicket.markup.ComponentTag;
import wicket.markup.html.PackageResourceReference;

/**
 * Attaches itself to the onchange event for a TextField or TextArea, and enhances that
 * event to fire not just when focus changes but also when keyboard input pauses. This
 * is effected in JavaScript, with a timer that resets when the onkeyup event fires.
 * @author nathan
 *
 */
public abstract class AjaxOnKeyPausedUpdater extends AjaxFormComponentUpdatingBehavior {

	private static final PackageResourceReference JAVASCRIPT = new PackageResourceReference(
			AjaxOnKeyPausedUpdater.class, "AjaxOnKeyPausedUpdater.js");

	/**
	 * Binds to onchange.
	 */
	public AjaxOnKeyPausedUpdater() {
		super("onchange");
	}
	
	/**
	 * Adds JavaScript listeners for onkeypress and onblur.
	 */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
        tag.put("onkeypress", "AjaxOnKeyPausedTimerReset(this);");
        tag.put("onblur", "AjaxOnKeyPausedTimerCancel();");
	}

	/**
	 * Adds needed JavaScript to header.
	 */
	@Override
	protected void onRenderHeadInitContribution(Response response) {
		super.onRenderHeadInitContribution(response);
		writeJsReference(response, JAVASCRIPT);
	}
}
