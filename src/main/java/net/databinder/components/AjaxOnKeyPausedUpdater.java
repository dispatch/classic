package net.databinder.components;

import wicket.Response;
import wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import wicket.markup.ComponentTag;
import wicket.markup.html.PackageResourceReference;

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
