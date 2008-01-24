package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public abstract class DataFormBase extends Form {
	
	public DataFormBase(String id, IModel model) {
		super(id, model);
	}

	@Override
	protected final void onSubmit() {
		try {
			new Transaction<Object>(Databinder.getEntityManager()) {
				@Override
				protected Object run() throws SQLException {
					onSubmit(Databinder.getEntityManager());
					return null;
				}
			}.execute();
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}
	
	abstract void onSubmit(EntityManager entityManager) throws SQLException;
}
