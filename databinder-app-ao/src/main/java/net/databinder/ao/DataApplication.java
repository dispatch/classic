package net.databinder.ao;

import org.apache.wicket.protocol.http.WebApplication;

import net.java.ao.EntityManager;

public abstract class DataApplication extends WebApplication implements ActiveObjectsApplication {

	@Override
	protected void init() {
		// TODO Auto-generated method stub
		super.init();
	}
		
	public EntityManager getEntityManager(Object key) {
		// TODO Auto-generated method stub
		return null;
	}
}
