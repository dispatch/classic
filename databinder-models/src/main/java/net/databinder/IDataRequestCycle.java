package net.databinder;

/**
 * Request cycle that should be notified on the first use of a data session.
 */
public interface IDataRequestCycle {
	public void dataSessionRequested();
}
