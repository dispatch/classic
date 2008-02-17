package net.databinder.ao;

import org.apache.wicket.Application;

import net.java.ao.EntityManager;

public class Databinder {
	public static EntityManager getEntityManager() {
		return getEntityManager(null);
	}
	public static EntityManager getEntityManager(Object managerKey) {
		return ((ActiveObjectsApplication)Application.get()).getEntityManager(managerKey);
	}
}
