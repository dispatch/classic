package net.databinder.components.hibernate;

import net.databinder.DataStaticService;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;

public class DataFormBase extends Form {
	private Object factoryKey;
	public DataFormBase(String id) {
		super(id);
	}
	public DataFormBase(final String id, IModel model)
	{
		super(id, model);
	}
	
	public Object getFactoryKey() {
		return factoryKey;
	}

	public DataFormBase setFactoryKey(Object key) {
		this.factoryKey = key;
		return this;
	}
	
	protected Session getHibernateSession() {
		return DataStaticService.getHibernateSession(factoryKey);
	}
	
	protected void onSubmit() {
		try {
			Session session = DataStaticService.getHibernateSession(factoryKey);
			session.flush(); // needed for conv. sessions, harmless otherwise
			session.getTransaction().commit();
		} catch (StaleObjectStateException e) {
			error(getString("version.mismatch", null)); // report error
		}
	}

}
