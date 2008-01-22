package net.databinder.models.ao;

import java.util.HashMap;

import net.databinder.ao.Databinder;
import net.databinder.models.LoadableWritableModel;
import net.java.ao.Common;
import net.java.ao.RawEntity;

public class EntityModel<T extends RawEntity<K>, K> extends LoadableWritableModel {
	private K id;
	private Class<T> entityType;
	
	public EntityModel(Class<T> entityType, K id) {
		this.entityType = entityType;
		this.id = id;
	}
	
	@Override
	protected Object load() {
		return Databinder.getEntityManager().get(entityType, id);
	}
	@SuppressWarnings("unchecked")
	public void setObject(Object object) {
		T entity = (T) object;
		entityType = (Class<T>) entity.getEntityType();
		id = Common.getPrimaryKeyValue(entity);
		setTempModelObject(entity);
	}
}
