package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.databinder.models.ao.EntityModel;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

import org.apache.wicket.model.CompoundPropertyModel;

/** Form to be used with a single object, wraps in a compound property model. */
@SuppressWarnings("unchecked")
public class DataForm extends TransactionalForm {
	
	public DataForm(String id, Class entityType) {
		super(id, new CompoundPropertyModel(new EntityModel(entityType)));
	}
	
	public DataForm(String id, EntityModel entityModel) {
		super(id, new CompoundPropertyModel(entityModel));
	}
	
	/** Default implementation saves object if bound, otherwise creates new object using model's fieldMap. */
	@Override
	protected void inSubmitTransaction(EntityManager entityManager) throws SQLException {
		if (getEntityModel().isBound())
			((RawEntity)getModelObject()).save();
		else
			setModelObject(entityManager.create(getEntityModel().getEntityType(), getEntityModel().getFieldMap()));
	}
	
	public EntityModel getEntityModel() {
		return (EntityModel) ((CompoundPropertyModel) getModel()).getChainedModel();
	}
	
	/** Button to delete this form's model object. */
	public class DeleteButton extends TransactionalButton {
		public DeleteButton(String id) {
			super(id);
			setDefaultFormProcessing(false);
		}
		@Override
		protected void inSubmitTransaction(EntityManager entityManager) throws SQLException {
			Databinder.getEntityManager().delete((RawEntity)DataForm.this.getModelObject());
		}
		@Override
		protected void afterSubmit() {
			getEntityModel().unbind();
			DataForm.this.modelChanged();
		}
		@Override
		public boolean isEnabled() {
			return getEntityModel().isBound();
		}
	}
	
}
