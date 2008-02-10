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

package net.databinder.models;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

/**
 * Adaptation of Wicket's LoadableDetachableModel that does not extend a
 * read-only model and permits changing the underlying object.
 */
public abstract class LoadableWritableModel  implements IModel {

	private transient boolean attached = false;
	private transient Object tempModelObject;

	public LoadableWritableModel() {
	}

	public final void detach() {
		if (attached) {
			attached = false;
			tempModelObject = null;
			onDetach();
		}
	}

	public Object getObject() {
		if (!attached) {
			attached = true;
			tempModelObject = load();

			onAttach();
		}
		return tempModelObject;
	}

	public final boolean isAttached() {
		return attached;
	}

	/**
	 * Called by subclass when the model object is readily available. Saves a later
	 * (possibly expensive) call to load().
	 * @param object
	 */
	protected void setTempModelObject(Object object) {
		attached = true;
		tempModelObject = object;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());
		sb.append(":attached=").append(attached).append(":tempModelObject=[")
				.append(this.tempModelObject).append("]");
		return sb.toString();
	}

	protected abstract Object load();

	/**
	 * Called when attaching, after load().
	 */
	protected void onAttach() {
	}

	/**
	 * Called when detaching.
	 */
	protected void onDetach() {
	}
}
