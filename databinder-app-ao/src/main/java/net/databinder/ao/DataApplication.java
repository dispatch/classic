package net.databinder.ao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.databinder.DataApplicationBase;
import net.java.ao.DatabaseProvider;
import net.java.ao.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional application base for ActiveObjects. */
public abstract class DataApplication extends DataApplicationBase implements ActiveObjectsApplication {
	
	private Logger logger = LoggerFactory.getLogger(DataApplication.class);

	/* Entity manager available to this application, default has key of null. */
	private Map<Object, EntityManager> entityManagers = new HashMap<Object, EntityManager>();

	/** 
	 * Initializes a default ActiveObjects entity manager. This is called 
	 * automatically during start-up. Applications with one entity manager
	 * will not normally need to override.
	 */
	protected void dataInit() {
		initEntityManager(null);
	}
	
	/**
	 * Initializes entity manager and generates schema if in development mode.
	 */
	protected void initEntityManager(Object key) {
		EntityManager entityManager = buildEntityManager(key, buildDatabaseProvider(key));
		setEntityManager(key, entityManager);
		if (isDevelopment()) try {
			generateSchema(entityManager, key);
		} catch (SQLException e) {
			logger.error("Error generating schema", e);
		}
	}

	/**
	 * @param provider provider returned by buildDatabaseProvider(key)
	 * @return instantiated EntityManager in default implementation, override for subclass
	 */
	protected EntityManager buildEntityManager(Object key, DatabaseProvider provider) {
		return new EntityManager(provider);
	}

	/**
	 * @return database provider for key, ignore key if app needs only one provider
	 */
	protected abstract DatabaseProvider buildDatabaseProvider(Object key);	

	/** Generate schema if desired, called only in development mode. */
	protected void generateSchema(EntityManager entityManager, Object key) throws SQLException { }
	
	/** Sets entity manager in map. */
	protected void setEntityManager(Object key, EntityManager entityManager) {
		entityManagers.put(key, entityManager);
	}
	
	/** @return entity manager for given key */
	public EntityManager getEntityManager(Object key) {
		return entityManagers.get(key);
	}
}
