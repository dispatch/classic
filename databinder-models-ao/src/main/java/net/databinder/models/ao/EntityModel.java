package net.databinder.models.ao;

import net.databinder.ao.Databinder;
import net.databinder.models.LoadableWritableModel;
import net.java.ao.Common;
import net.java.ao.RawEntity;

public class EntityModel<T extends RawEntity<K>, K> extends LoadableWritableModel {
	private K id;
	private Class<T> entityClass;
	
	@Override
	protected Object load() {
		return Databinder.getEntityManager().get(entityClass, id);
	}
	public void setObject(Object object) {
		T entity = (T) object; // necessary cast
		entityClass =(Class<T>)  entity.getEntityType(); // should be unnecessary ?
		id = Common.getPrimaryKeyValue(entity);
		setTempModelObject(entity);
	}
}
