package net.databinder.components;

import net.databinder.DataStaticService;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public class DataFormBase extends Form {
	public DataFormBase(String id) {
		super(id);
	}
	public DataFormBase(final String id, IModel model)
	{
		super(id, model);
	}
	
	protected void onSubmit() {
		try {
			Session session = DataStaticService.getHibernateSession();
			session.flush(); // needed for conv. sessions, harmless otherwise
			session.getTransaction().commit();
		} catch (StaleObjectStateException e) {
			error(getString("version.mismatch", null)); // report error
		}
	}

}
