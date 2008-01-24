package net.databinder.components.ao;

import java.sql.SQLException;

import net.databinder.ao.Databinder;
import net.databinder.models.ao.EntityModel;
import net.java.ao.EntityManager;
import net.java.ao.RawEntity;
import net.java.ao.Transaction;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;

@SuppressWarnings("unchecked")
public class DataForm extends DataFormBase {
	private Class entityType;
	
	public DataForm(String id, Class entityType) {
		super(id, new CompoundPropertyModel(new EntityModel(entityType)));
		this.entityType = entityType;
	}
	
	public DataForm(String id, EntityModel entityModel) {
		super(id, new CompoundPropertyModel(entityModel));
		this.entityType = entityModel.getEntityType();
	}
	
	protected void onSubmit(EntityManager entityManager) throws SQLException {
		if (getEntityModel().isBound())
			((RawEntity)getModelObject()).save();
		else
			setModelObject(entityManager.create(entityType, getEntityModel().getPropertyStore()));
	}
	
	public EntityModel getEntityModel() {
		return (EntityModel) ((CompoundPropertyModel) getModel()).getChainedModel();
	}
	
	public void clear() {
		getEntityModel().clear();
	}
	
	public class DeleteButton extends Button {
		 public DeleteButton(String id) {
			super(id);
			setDefaultFormProcessing(false);
		}
		 @Override
		public boolean isEnabled() {
			return getEntityModel().isBound();
		}
		 @Override
		public void onSubmit() {
				try {
					new Transaction<Object>(Databinder.getEntityManager()) {
						@Override
						protected Object run() throws SQLException {
							Databinder.getEntityManager().delete((RawEntity)DataForm.this.getModelObject());
							return null;
						}
					}.execute();
				} catch (SQLException e) { throw new WicketRuntimeException(e); }
				clear();
		 }
	}
	
	public class ClearLink extends Link {
		public ClearLink(String id) {
			super(id);
		}
		@Override
		public boolean isEnabled() {
			return getEntityModel().isBound();
		}
		@Override
		public void onClick() {
			clear();
		}
	}
	
}
