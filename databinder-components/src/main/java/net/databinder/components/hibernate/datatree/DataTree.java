package net.databinder.components.hibernate.datatree;

import java.io.Serializable;
import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.model.Model;


/**
 * An extension of {@link BaseTree} based on node objects being represented by
 * {@link HibernateObjectModel}s. Additionally, it offers some convenience
 * methods.
 * 
 * @author Thomas Kappler
 * 
 * @param <T> the type of the objects being represented by the tree nodes
 */
public abstract class DataTree<T extends IDataTreeNode<T>> extends BaseTree {
	
	/**
	 * @param id
	 *            as usual
	 * @param root
	 *            a domain object serving as root - if your tree is rootless,
	 *            create a dummy one (or consider using {@link SimpleDataTree})
	 * @param firstLevelChildren
	 *            The first level in the tree, below the root; the further
	 *            levels are included recursively from there. If your tree is
	 *            not rootless, this will be <code>root.getChildren()</code>.
	 */
	public DataTree(String id, T root, Collection<T> firstLevelChildren) {
		super(id);
		
		DefaultMutableTreeNode fakeRoot = 
				new DefaultMutableTreeNode(root);
		populateTree(fakeRoot, firstLevelChildren);
		TreeModel treeModel = new DefaultTreeModel(fakeRoot);
		setModel(new Model((Serializable) treeModel));
	}
	
	/**
	 * Recursively build the tree nodes according to the structure given by the
	 * beans.
	 * <p>
	 * The parent and its children are separate parameters instead of calling
	 * <code>parent.getChildren()</code>, because for rootless trees, the
	 * parent is a "fake" one for the first level, i.e., it is not a domain
	 * object and thus must not be persisted. Therefore, it cannot be connected
	 * to the top-level set of elements via the parent/child relationship,
	 * because Hibernate would pick this up when cascading.
	 * </p>
	 * 
	 * @param parent
	 *            a tree node serving as parent to the newly created nodes for
	 *            the elements in children
	 * @param children
	 *            objects to be inserted into the tree below parent
	 */
	private void populateTree(DefaultMutableTreeNode parent, 
			Collection<T> children) {		
		for (T t : children) {
			HibernateObjectModel m = new HibernateObjectModel(t);
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(m);
			parent.add(node);
			populateTree(node, t.getChildren());
		}
	}

	/**
	 * Get the object of type T represented by the given node.
	 * 
	 * @param node
	 *            a tree node
	 * @return the object represented by node
	 */
	@SuppressWarnings("unchecked")
	public T getObjectFromNode(DefaultMutableTreeNode node) {
		return (T) getModelFromNode(node).getObject();
	}
	
	/**
	 * Get the {@link HibernateObjectModel} behind a tree node.
	 * 
	 * @param node
	 *            a tree node
	 * @return the model
	 */
	public HibernateObjectModel getModelFromNode(DefaultMutableTreeNode node) {
		return (HibernateObjectModel) node.getUserObject();
	}

	/**
	 * @return the root node of the tree
	 */
	public DefaultMutableTreeNode getRootNode() {
		DefaultTreeModel treeModel = (DefaultTreeModel) getModelObject();
		return (DefaultMutableTreeNode) treeModel.getRoot();
	}
	
	/**
	 * Repaint the tree when something has changed. It possibly does too much,
	 * but you're safe that changes do show after you call it.
	 * 
	 * @param target
	 */
	public void repaint(AjaxRequestTarget target) {
		invalidateAll();
		updateTree(target);
	}
}
