package net.databinder.models.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.databinder.ao.Databinder;
import net.java.ao.RawEntity;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class EntityCreationModel<T extends RawEntity<K>, K> extends AbstractReadOnlyModel {
	private Map<String, Object> properties;
	private Class<T> entityType;
	
	public EntityCreationModel(Class<T> entityType) {
		this.entityType = entityType;
		reset();
	}
	
	@Override
	public Map<String, Object> getObject() {
		return properties;
	}
	
	public T create() {
		try {
			return Databinder.getEntityManager().create(entityType, properties);
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	public void reset() {
		properties = new HashMap<String, Object>();
	}
}
