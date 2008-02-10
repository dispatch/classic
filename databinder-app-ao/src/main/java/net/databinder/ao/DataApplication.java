package net.databinder.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.databinder.DataApplicationBase;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

public abstract class DataApplication extends DataApplicationBase implements ActiveObjectsApplication {
	
	private Logger logger = LoggerFactory.getLogger(DataApplication.class);

	private Map<Object, EntityManager> entityManagers = new HashMap<Object, EntityManager>();

	protected void dataInit() {
		buildEntityManager(null);
	}
	
	protected void buildEntityManager(Object key) {
		EntityManager entityManager = new EntityManager(buildDatabaseProvider(key));
		configureEntityManager(entityManager, key);
		setEntityManager(key, entityManager);
		if (isDevelopment()) try {
			generateSchema(entityManager, key);
		} catch (SQLException e) {
			logger.error("Error generating schema", e);
		}
	}
	
	protected void generateSchema(EntityManager entityManager, Object key) throws SQLException { }
	
	protected void configureEntityManager(EntityManager entityManager, Object key) { }
	
	protected void setEntityManager(Object key, EntityManager entityManager) {
		entityManagers.put(key, entityManager);
	}
	
	protected abstract DatabaseProvider buildDatabaseProvider(Object key);
	
	public EntityManager getEntityManager(Object key) {
		return entityManagers.get(key);
	}
}
