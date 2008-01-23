package net.databinder.models.ao;

import java.io.Serializable;

import net.databinder.ao.Databinder;
import net.databinder.models.LoadableWritableModel;
import net.java.ao.Common;
import net.java.ao.RawEntity;

public class EntityModel extends LoadableWritableModel {
	private Serializable id;
	private Class entityType;
	
	public EntityModel(Class entityType, Serializable id) {
		this.entityType = entityType;
		this.id = id;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object load() {
		return Databinder.getEntityManager().get(entityType, id);
	}
	public void setObject(Object object) {
		RawEntity<?> entity = (RawEntity<?>) object;
		entityType = entity.getEntityType();
		id = (Serializable) Common.getPrimaryKeyValue(entity);
		setTempModelObject(entity);
	}
}
