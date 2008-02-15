package net.databinder.models.hib.binders;

import org.hibernate.Query;

/**
 * Binds a query's parameters to the properties of an object, as needed.
 * 
 * @author Jonathan
 */
public class PropertyQueryBinder extends AbstractPropertyQueryBinder {

	private static final long serialVersionUID = -5670443203499179555L;

	private final Object object;

	/**
	 * @param object
	 *            The object to bind properties of
	 */
	public PropertyQueryBinder(final Object object) {
		this.object = object;
	}

	/**
	 * @param query
	 *            The query to bind
	 */
	public void bind(final Query query) {
		bind(query, object);
	}
}