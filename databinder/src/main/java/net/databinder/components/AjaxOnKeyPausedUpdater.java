package net.databinder.components;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;

/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * Attaches itself to the onchange event for a TextField or TextArea, and enhances that
 * event to fire not just when focus changes but also when keyboard input pauses. This
 * is effected in JavaScript, with a timer that resets when the onkeyup event fires.
 * @author Nathan Hamblen
 *
 */
public abstract class AjaxOnKeyPausedUpdater extends AjaxFormComponentUpdatingBehavior {

	private static final ResourceReference JAVASCRIPT = new ResourceReference(
			AjaxOnKeyPausedUpdater.class, "AjaxOnKeyPausedUpdater.js");

	/**
	 * Binds to onchange.
	 */
	public AjaxOnKeyPausedUpdater() {
		super("onchange");
	}

	/**
	 * Adds needed JavaScript to header.
	 */
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.renderJavascriptReference(JAVASCRIPT);
	}

	/**
	 * Adds JavaScript listeners for onkeyup and onblur.
	 */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
        tag.put("onkeyup", "AjaxOnKeyPausedTimerReset(this);");
        tag.put("onblur", "AjaxOnKeyPausedTimerCancel();");
	}
}
