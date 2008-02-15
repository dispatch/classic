package net.databinder.models.hib.binders;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hibernate.Query;

/**
 * A query binder that sets query parameters to corresponding properties taken
 * from the given Wicket model object.
 * 
 * @author Jonathan
 */
public class ModelPropertyQueryBinder extends AbstractPropertyQueryBinder
		implements IDetachable {

	private static final long serialVersionUID = -6544558086991812867L;

	protected final IModel model;

	public ModelPropertyQueryBinder(final IModel model) {
		this.model = model;
	}

	public void detach() {
		model.detach();
	}

	public void bind(final Query query) {
		bind(query, model.getObject());
	}
}