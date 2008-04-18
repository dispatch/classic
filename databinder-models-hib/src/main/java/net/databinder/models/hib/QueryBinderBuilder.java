package net.databinder.models.hib;

import org.hibernate.Query;
import org.hibernate.Session;

public class QueryBinderBuilder implements QueryBuilder {
	private String query;
	private QueryBinder[] binders;
	public QueryBinderBuilder(String query, QueryBinder... binders) {
		this.query = query;
		this.binders = binders;
	}
	public Query build(Session sess) {
		Query q = sess.createQuery(query);
		for (QueryBinder b: binders)
			b.bind(q);
		return q;
	}
}
