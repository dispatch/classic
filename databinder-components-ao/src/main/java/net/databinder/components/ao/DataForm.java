package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.databinder.models.ao.EntityModel;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

import org.apache.wicket.model.CompoundPropertyModel;

@SuppressWarnings("unchecked")
public class DataForm extends TransactionalForm {
	private Class entityType;
	
	public DataForm(String id, Class entityType) {
		super(id, new CompoundPropertyModel(new EntityModel(entityType)));
		this.entityType = entityType;
	}
	
	public DataForm(String id, EntityModel entityModel) {
		super(id, new CompoundPropertyModel(entityModel));
		this.entityType = entityModel.getEntityType();
	}
	
	@Override
	protected void inSubmitTransaction(EntityManager entityManager) throws SQLException {
		if (getEntityModel().isBound())
			((RawEntity)getModelObject()).save();
		else
			setModelObject(entityManager.create(entityType, getEntityModel().getPropertyStore()));
	}
	
	public EntityModel getEntityModel() {
		return (EntityModel) ((CompoundPropertyModel) getModel()).getChainedModel();
	}
	
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
		}
		@Override
		public boolean isEnabled() {
			return getEntityModel().isBound();
		}
	}
	
}
