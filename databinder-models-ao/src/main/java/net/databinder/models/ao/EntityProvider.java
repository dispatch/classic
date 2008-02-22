package net.databinder.models.ao;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;

import net.databinder.ao.Databinder;
import net.databinder.models.PropertyDataProvider;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Objects;

@SuppressWarnings("unchecked")
public class EntityProvider extends PropertyDataProvider {
	
	private Class entityType;
	private Query query;
	private Object managerKey;
	
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
			
			return Arrays.asList(Databinder.getEntityManager(managerKey).find(entityType, q)).iterator();
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	public int size() {
		try {
			return Databinder.getEntityManager(managerKey).count(entityType, query);
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	@Override
	protected IModel dataModel(Object object) {
		return new EntityModel((RawEntity)object);
	}

	public void detach() { }

	public Object getManagerKey() {
		return managerKey;
	}

	public void setManagerKey(Object managerKey) {
		this.managerKey = managerKey;
	}
}
