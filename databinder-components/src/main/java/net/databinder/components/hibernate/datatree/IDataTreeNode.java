package net.databinder.components.hibernate.datatree;

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

	public Collection<T> getChildren();

}
