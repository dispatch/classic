package net.databinder.hib;

/**
 * Request cycle that should be notified on the first use of a data session.
 */
public interface HibernateRequestCycle {
	public void dataSessionRequested(Object key);
}
