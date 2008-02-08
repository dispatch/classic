package net.databinder.models.ao;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.databinder.ao.Databinder;
import net.databinder.models.BindingModel;
import net.databinder.models.LoadableWritableModel;
import net.java.ao.Common;
import net.java.ao.RawEntity;

@SuppressWarnings("unchecked")
public class EntityModel extends LoadableWritableModel implements BindingModel {
	private Serializable id;
	private Class entityType;
	private Map<String, Object> propertyStore;
	
	public EntityModel(Class entityType, Serializable id) {
		this(entityType);
		this.id = id;
	}
	
	public EntityModel(Class entityType) {
		this.entityType = entityType;
	}
	
	public boolean isBound() {
		return id != null;
	}
	
	@Override
	protected Object load() {
		if (isBound())
			return Databinder.getEntityManager().get(entityType, id);
		return getPropertyStore();
	}
	
	public void setObject(Object object) {
		unbind();
		RawEntity entity = (RawEntity) object;
		entityType = entity.getEntityType();
		id = (Serializable) Common.getPrimaryKeyValue(entity);
		setTempModelObject(entity);
	}
	
	protected void putDefaultProperties(Map<String, Object> propertyStore) { }
	
	public void unbind() {
		id = null;
		propertyStore = null; 
		detach();
	}

	public Map<String, Object> getPropertyStore() {
		if (propertyStore == null) {
			propertyStore = new HashMap<String, Object>();
			putDefaultProperties(propertyStore);
		}
		return propertyStore;
	}

	public Class getEntityType() {
		return entityType;
	}
	
}
