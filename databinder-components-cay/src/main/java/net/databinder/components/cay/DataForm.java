package net.databinder.components.cay;

import net.databinder.models.cay.DataObjectModel;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IChainingModel;

/** Form to be used with a single object, wraps in a compound property model. */
public class DataForm extends CommittingDataForm {
	public DataForm(String id, Class<? extends DataObject> cl) {
		super(id, new CompoundPropertyModel(new DataObjectModel(cl)));
	}
	public DataForm(String id, DataObject object) {
		super(id, new CompoundPropertyModel(new DataObjectModel(object)));
	}
	public DataForm(String id, ObjectId objectId) {
		super(id, new CompoundPropertyModel(new DataObjectModel(objectId)));
	}
	public DataObjectModel getPersistentObjectModel() {
		return (DataObjectModel) ((IChainingModel)getModel()).getChainedModel();
	}
}
