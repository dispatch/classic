package net.databinder.components.tree.hib;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.databinder.components.tree.data.DataTreeObject;
import net.databinder.models.hib.CriteriaBuilder;
import net.databinder.models.hib.HibernateListModel;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hibernate.Criteria;
import org.hibernate.criterion.Property;


/**
 * An extension of {@link BaseTree} based on node objects being represented by
 * {@link HibernateObjectModel}s. Additionally, it offers some convenience
 * methods.
 * 
 * @author Thomas Kappler
 * 
 * @param <T> the IDataTreeNode implementation being represented by the tree nodes
 */
public abstract class DataTree<T extends DataTreeObject<T>> extends BaseTree {
	/**
	 * Construct a tree with a root entity.
	 * @param id Wicket id
	 * @param rootModel must contain a root of type T
	 */
	@SuppressWarnings("unchecked")
	public DataTree(String id, HibernateObjectModel rootModel) {
		super(id);
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootModel);
		populateTree(rootNode, ((T)rootModel.getObject()).getChildren());
		setModel(new Model(new DefaultTreeModel(rootNode)));
	}
	
	/**
	 * Construct a rootless tree based on a list of top level nodes. 
	 * @param id
	 * @param topLevelModel must contain a List<T> of top level children
	 */
	@SuppressWarnings("unchecked")
	public DataTree(String id, HibernateListModel topLevelModel) {
		super(id);
		setRootLess(true);
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(topLevelModel);
		populateTree(rootNode, (List<T>)topLevelModel.getObject());
		setModel(new Model(new DefaultTreeModel(rootNode)));
	}
	
	/**
	 * Convenience criteria builder for fetching top-level entities.
	 */
	public static class TopLevelCriteriaBuilder implements CriteriaBuilder {
		/**
		 * build criteria for a null "parent" property
		 */
		public void build(Criteria criteria) {
			criteria.add(Property.forName("parent").isNull());
		}
	}
	
	public DefaultMutableTreeNode clear(AjaxRequestTarget target) {
		T newObject = createNewObject();
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
	 * Get the IDataTreeNode instance behind this node, or null if the node is the
	 * root of a tree with no root entity.
	 * 
	 * @param node
	 *            a tree node
	 * @return the object represented by node
	 */
	@SuppressWarnings("unchecked")
	public T getDataTreeNode(DefaultMutableTreeNode node) {
		Object nodeObject = ((IModel) node.getUserObject()).getObject();
		return (nodeObject instanceof DataTreeObject<?>) ?
			(T) nodeObject : null;
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
	 * Create a new user object using {@link #createNewObject()} and add it
	 * to the tree as a child of parentNode.
	 * 
	 * @param parentNode
	 *            to node serving as parent of the new object
	 * @return the newly created tree node
	 */
	public DefaultMutableTreeNode addNewChildNode(DefaultMutableTreeNode parentNode) {
		T newObject = createNewObject();
		
		T parent = getDataTreeNode(parentNode);
		if (parent != null)
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
	protected abstract T createNewObject();

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
	
	@Override
	public void onDetach() {
		super.onDetach();
		// in a root less tree it's not bound to any component
		((IModel)getRootNode().getUserObject()).detach();
	}
}
