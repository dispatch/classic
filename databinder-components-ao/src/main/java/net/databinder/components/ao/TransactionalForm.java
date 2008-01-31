package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.java.ao.EntityManager;
import net.java.ao.Transaction;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public abstract class TransactionalForm extends Form {
	
	public TransactionalForm(String id, IModel model) {
		super(id, model);
	}

	@Override
	protected void onSubmit() {
		try {
			new Transaction<Object>(Databinder.getEntityManager()) {
				@Override
				protected Object run() throws SQLException {
					inSubmitTransaction(Databinder.getEntityManager());
					return null;
				}
			}.execute();
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
		afterSubmit();
	}
	
	protected abstract void inSubmitTransaction(EntityManager entityManager) throws SQLException;
	protected void afterSubmit() { };
}
