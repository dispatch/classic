package net.databinder.components.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;

public class DataForm<T extends RawEntity<K>, K> extends DataFormBase {
	private Class<T> entityType;
	
	public DataForm(String id, Class<T> entityType) {
		super(id, new CompoundPropertyModel(new Model(new HashMap<String, Object>())));
		this.entityType = entityType;
	}
	
	protected void onSubmit(EntityManager entityManager) throws SQLException {
		if (isTransient())
			entityManager.create(entityType, (Map<String, Object>) getModelObject());
		setModelObject(new HashMap());
	}
	
	public boolean isTransient() {
		return getModelObject() instanceof Map;
	}
}
