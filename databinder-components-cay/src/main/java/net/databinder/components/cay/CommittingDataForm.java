package net.databinder.components.cay;

import net.databinder.cay.Databinder;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/** Form that commits the current context in onSubmit() */
public class CommittingDataForm extends Form {
	public CommittingDataForm(String id) {
		super(id);
	}
	
	public CommittingDataForm(String id, IModel model) {
		super(id, model);
	}
	
	/** Base implementation commits current context. */
	@Override
	protected void onSubmit() {
		Databinder.getContext().commitChanges();
	}
}
