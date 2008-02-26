package net.databinder.components.tree.data;

import java.util.Collection;

/**
 * Classes used as the concrete type of a {@link DataTree}, i.e., the type of
 * objects being represented by the tree nodes, must implement this interface.
 * 
 * @author Thomas Kappler
 * 
 * @param <T>
 *            the concrete type this tree node is representing
 */
public interface DataTreeObject<T> {

	/**
	 * @return the children of this tree node
	 */
	public Collection<T> getChildren();
	
	/** Add new child node */
	public void addChild(T child);
	
	/**
	 * @return the parent of this tree node
	 */
	public T getParent();

}
