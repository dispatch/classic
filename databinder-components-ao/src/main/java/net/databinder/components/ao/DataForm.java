package net.databinder.components.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;

public class DataForm extends DataFormBase {
	private Class entityType;
	
	public DataForm(String id, Class entityType) {
		super(id, new CompoundPropertyModel(new Model(new HashMap())));
		this.entityType = entityType;
	}
	
	
	@SuppressWarnings("unchecked")
	protected void onSubmit(EntityManager entityManager) throws SQLException {
		Object obj = getModelObject();
		if (obj instanceof Map)
			entityManager.create(entityType, (Map) getModelObject());
		else
			((RawEntity)getModelObject()).save();
	}
	
}
