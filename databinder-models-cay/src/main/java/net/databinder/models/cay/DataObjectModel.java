package net.databinder.models.cay;

import net.databinder.cay.Databinder;
import net.databinder.models.BindingModel;
import net.databinder.models.LoadableWritableModel;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectId;

public class DataObjectModel extends LoadableWritableModel implements BindingModel {
	
	private ObjectId id;
	private DataObject retainedObject;

	public DataObjectModel(Class<? extends DataObject> objectClass) {
		setObject(Databinder.getContext().newObject(objectClass));
	}
	
	public DataObjectModel(ObjectId id) {
		this.id = id;
	}

	public DataObjectModel(DataObject object) {
		id = object.getObjectId();
	}

	@Override
	protected Object load() {
		if (!isBound()) {
			Databinder.getContext().registerNewObject(retainedObject);
			return retainedObject;
		}
		return DataObjectUtils.objectForPK(Databinder.getContext(), id);
	}

	public void setObject(Object object) {
		DataObject dataObject = (DataObject) object;
		id = dataObject.getObjectId();
		setTempModelObject(dataObject);
		if (!isBound())
			retainedObject = dataObject;
		else
			retainedObject = null;
	}
	
	@Override
	public DataObject getObject() {
		return (DataObject) super.getObject();
	}
	
	@Override
	protected void onDetach() {
		if (retainedObject != null) {
			id = retainedObject.getObjectId();
			if (isBound())
				retainedObject = null;
		}
	}
	
	public boolean isBound() {
		return !id.isTemporary();
	}
	
	public void unbind() {
		setObject(Databinder.getContext().createAndRegisterNewObject(id.getEntityName()));
	}

}
