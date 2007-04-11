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
package net.databinder.components;

import java.io.IOException;

import org.hibernate.Hibernate;

import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;

/**
 * Extension of FileUploadField optimized for blob resources. This upload field can be 
 * bound to an object with a compound property model where the property corresponds
 * to the destination blob setter. This allows for file uploads with no specific code for each 
 * upload component.
 * @author Nathan Hamblen
 */
public class BlobUploadField extends FileUploadField {
	/**
	 * Costructor to be used with compound property model.
	 * @param id component id, should resolve to a stream property
	 */
	public BlobUploadField(String id) {
		super(id);
	}

	/**
	 * Constructor to be used with PropertyModel or other model.
	 * @param id component id
	 * @param model should resolve to a stream setter
	 */
	public BlobUploadField(String id, IModel model) {
		super(id, model);
	}

	/**
	 * Converts the upload's inputstream to the resolved blob setter.
	 */
	@Override
	public void updateModel() {
		try {
			if (getFileUpload() != null) {
				setModelObject(Hibernate.createBlob(getFileUpload().getInputStream()));
				onUpdated();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	protected void onUpdated() { }
}