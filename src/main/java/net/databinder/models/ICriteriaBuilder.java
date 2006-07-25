package net.databinder.models;

import java.io.Serializable;

import org.hibernate.Criteria;

public interface ICriteriaBuilder extends Serializable {
	void build(Criteria criteria);
}
