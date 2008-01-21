package net.databinder.ao;

import org.apache.wicket.Application;

import net.java.ao.EntityManager;

public class Databinder {
	public static EntityManager getEntityManager() {
		return ((ActiveObjectsApplication)Application.get()).getEntityManager(null);
	}
}
