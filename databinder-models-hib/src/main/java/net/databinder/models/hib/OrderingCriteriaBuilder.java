package net.databinder.models.hib;

import java.io.Serializable;

import org.hibernate.Criteria;

/**
 * Builds criteria objects with or without order. Only one of the build methods
 * should be called in building a criteria object.
 */
public interface OrderingCriteriaBuilder extends Serializable {
	/** Build the criteria without setting an order */
	public void buildUnordered(Criteria criteria);
	/** Build the (entire) criteria, including an order */
	public void buildOrdered(Criteria criteria);
}
