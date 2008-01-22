package net.databinder.models.ao;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import net.databinder.ao.Databinder;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.LoadableDetachableModel;

public class EntityListModel<T extends RawEntity<K>, K> extends LoadableDetachableModel {

	private Class<T> entityType;
	private Query query;
	
	public EntityListModel(Class<T>entityType) {
		this (entityType, Query.select());
	}
	public EntityListModel(Class<T>entityType, Query query) {
		this.entityType = entityType;
		this.query = query;
	}
	
	@Override
	protected List<T> load() {
		try {
			return Arrays.asList(Databinder.getEntityManager().find(entityType, query));
		} catch (SQLException e) {
			throw new WicketRuntimeException("", e);
		}
	}
}
