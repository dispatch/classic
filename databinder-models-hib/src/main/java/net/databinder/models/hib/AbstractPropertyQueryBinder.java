package net.databinder.models.hib;


import org.apache.wicket.util.lang.PropertyResolver;
import org.hibernate.Query;

/**
 * Base class for classes that bind queries using object properties.
 * 
 * @author Jonathan
 */
public abstract class AbstractPropertyQueryBinder implements QueryBinder {

	private static final long serialVersionUID = 145077736634107819L;

	/**
	 * @param query
	 *            The query to bind
	 * @param object
	 *            The object to pull properties from
	 */
	protected void bind(final Query query, final Object object) {
		for (final String parameter : query.getNamedParameters()) {
			query.setParameter(parameter, PropertyResolver.getValue(parameter,
					object));
		}
	}
}
