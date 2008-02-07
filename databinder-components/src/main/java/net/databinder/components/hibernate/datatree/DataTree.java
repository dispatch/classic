package net.databinder.components.hibernate.datatree;

import java.io.Serializable;
import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.databinder.DataStaticService;
import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.model.Model;
import org.hibernate.Session;


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
	 *            a domain object serving as root; if your tree is rootless,
	 *            create it manually
	 */
	public DataTree(String id, T root) {
		super(id);
		
		Session session = DataStaticService.getHibernateSession();
		session.saveOrUpdate(root);
		
		HibernateObjectModel rootModel = new HibernateObjectModel(root);

		init(root, rootModel);
	}
	
	
	private void init(T root, HibernateObjectModel rootModel) {
		DefaultMutableTreeNode rootNode = 
				new DefaultMutableTreeNode(rootModel);
		populateTree(rootNode, root.getChildren());
		TreeModel treeModel = new DefaultTreeModel(rootNode);
		setModel(new Model((Serializable) treeModel));
	}
	
	public DefaultMutableTreeNode clear(AjaxRequestTarget target) {
		T newObject = createNewUserObject();
		DefaultMutableTreeNode newRootNode = new DefaultMutableTreeNode(
				new HibernateObjectModel(newObject));
		TreeModel treeModel = new DefaultTreeModel(newRootNode);
		setModel(new Model((Serializable) treeModel));
		repaint(target);
		return newRootNode; 
	}
	
	/**
	 * Recursively build the tree nodes according to the structure given by the
	 * beans.
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
		if (treeModel.getRoot() == null) {
			return null;
		}
		return (DefaultMutableTreeNode) treeModel.getRoot();		
	}
	
	/**
	 * Create a new user object using {@link #createNewUserObject()} and add it
	 * to the tree as a child of parentNode.
	 * 
	 * @param parentNode
	 *            to node serving as parent of the new object
	 * @return the newly created tree node
	 */
	public DefaultMutableTreeNode createAndAddNewChildNode(DefaultMutableTreeNode parentNode) {
		T newObject = createNewUserObject();
		T parent = getObjectFromNode(parentNode);
		parent.addChild(newObject);
		
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
				new HibernateObjectModel(newObject)); 
		parentNode.add(newNode);
		return newNode;
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

	/**
	 * Create a new instance of T. Used to create the backing objects of new
	 * tree nodes.
	 * 
	 * @return a new instance of T
	 */
	protected abstract T createNewUserObject();

	/**
	 * Override to update components when another tree node is selected. Does
	 * nothing by default.
	 * 
	 * @param target
	 * @param selectedNode
	 *            the currently selected node
	 */
	public void updateDependentComponents(AjaxRequestTarget target, DefaultMutableTreeNode selectedNode) {
		// Do nothing by default
	}
}
