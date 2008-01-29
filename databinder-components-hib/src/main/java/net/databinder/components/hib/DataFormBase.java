package net.databinder.components.hib;

import net.databinder.hib.Databinder;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;

/**
 * Base class for forms that commit in onSubmit(). This is extended by DataForm, and may be
 * extended directly by client forms when DataForm is not appropriate. Transactions
 * are committed only when no errors are displayed.
 * @author Nathan Hamblen
 */
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
		return Databinder.getHibernateSession(factoryKey);
	}
	
	/**
	 * Commits transaction if no errors are registered for any form component. 
	 */
	protected void onSubmit() {
		try {
			if (!hasError()) {
				Session session = Databinder.getHibernateSession(factoryKey);
				session.flush(); // needed for conv. sessions, harmless otherwise
				session.getTransaction().commit();
			}
		} catch (StaleObjectStateException e) {
			error(getString("version.mismatch", null)); // report error
		}
	}

}
