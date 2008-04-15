package net.databinder.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.databinder.DataApplicationBase;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataApplication extends DataApplicationBase implements ActiveObjectsApplication {
	
	private Logger logger = LoggerFactory.getLogger(DataApplication.class);

	private Map<Object, EntityManager> entityManagers = new HashMap<Object, EntityManager>();

	protected void dataInit() {
		initEntityManager(null);
	}
	
	protected void initEntityManager(Object key) {
		EntityManager entityManager = buildEntityManager(key, buildDatabaseProvider(key));
		setEntityManager(key, entityManager);
		if (isDevelopment()) try {
			generateSchema(entityManager, key);
		} catch (SQLException e) {
			logger.error("Error generating schema", e);
		}
	}
	
	protected abstract DatabaseProvider buildDatabaseProvider(Object key);	

	protected EntityManager buildEntityManager(Object key, DatabaseProvider provider) {
		return new EntityManager(provider);
	}
	
	protected void generateSchema(EntityManager entityManager, Object key) throws SQLException { }
	
	protected void setEntityManager(Object key, EntityManager entityManager) {
		entityManagers.put(key, entityManager);
	}
	
	public EntityManager getEntityManager(Object key) {
		return entityManagers.get(key);
	}
}
