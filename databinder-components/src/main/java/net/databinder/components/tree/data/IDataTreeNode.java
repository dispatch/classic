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
public interface IDataTreeNode<T> {

	/**
	 * @return the children of this tree node
	 */
	public Collection<T> getChildren();
	
	/**
	 * @return the parent of this tree node
	 */
	public T getParent();

	/**
	 * @param child
	 *            an object of type T to be added to the children of this node
	 */
	public void addChild(T child);
	/**
	 * @param child
	 *            an object of type T to be removed from the children of this
	 *            node
	 */
	public void removeChild(T child);

}
