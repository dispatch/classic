package net.databinder.models.ao;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;

import net.databinder.ao.Databinder;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Objects;

@SuppressWarnings("unchecked")
public class EntityProvider implements IDataProvider {
	
	private Class entityType;
	private Query query;
	/** Controls wrapping with a compound property model. */
	private boolean wrapWithPropertyModel = true;
	
	public EntityProvider(Class entityType) {
		this (entityType, Query.select());
	}
	
	public EntityProvider(Class entityType, Query query) {
		this.entityType = entityType;
		this.query = query;
	}
	
	public Iterator iterator(int first, int count) {
		try {
			Query q = ((Query) Objects.cloneObject(query)).offset(first).limit(count);
			
			return Arrays.asList(Databinder.getEntityManager().find(entityType, q)).iterator();
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	public int size() {
		try {
			return Databinder.getEntityManager().count(entityType, query);
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	public IModel model(Object object) {
		IModel model = new EntityModel((RawEntity)object);
		if (wrapWithPropertyModel)
			model = new CompoundPropertyModel(model);
		return model;
	}
	
	public EntityProvider setWrapWithPropertyModel(boolean wrapWithPropertyModel) {
		this.wrapWithPropertyModel = wrapWithPropertyModel;
		return this;
	}

	public void detach() { }
}
