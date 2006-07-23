package net.databinder.models;

import org.hibernate.Criteria;

public interface ICriteriaBuilder {
	void build(Criteria criteria);
}
