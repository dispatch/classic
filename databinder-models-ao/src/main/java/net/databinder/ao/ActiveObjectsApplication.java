package net.databinder.ao;

import net.java.ao.EntityManager;

public interface ActiveObjectsApplication {
	EntityManager getEntityManager(Object key);
}
