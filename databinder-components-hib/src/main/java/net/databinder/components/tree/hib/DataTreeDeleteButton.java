package net.databinder.components.tree.hib;

import javax.swing.tree.DefaultMutableTreeNode;

import net.databinder.components.tree.data.DataTreeObject;
import net.databinder.hib.Databinder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.hibernate.Session;


/**
 * Delete the selected node. Works only with {@link SingleSelectionDataTree} to
 * avoid dealing with multiple selected nodes.
 * <p>
 * The root cannot be deleted, it must be handled elsewhere in the application.
 * This follows the Sun <a
 * href="http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html">How
 * to Use Trees</a> tutorial, example DynamicTreeDemo.
 * </p>
 * 
 * @author Thomas Kappler
 * 
 * @param <T>
 *            see {@link DataTree}
 */
public class DataTreeDeleteButton<T extends DataTreeObject<T>> extends AjaxButton {

	private SingleSelectionDataTree<T> tree;
	private boolean deleteOnlyLeafs = true;
	
	public DataTreeDeleteButton(String id, SingleSelectionDataTree<T> tree) {
		super(id);
		this.tree = tree;
		setDefaultFormProcessing(false);
	}

	public DataTreeDeleteButton(String id, SingleSelectionDataTree<T> tree,
			boolean deleteOnlyLeafs) {
		this(id, tree);
		this.deleteOnlyLeafs = deleteOnlyLeafs;
	}

	@Override
	public boolean isEnabled() {
		DefaultMutableTreeNode selected = tree.getSelectedTreeNode(); 
		if (selected == null) {
			return false;
		}
		if (selected.isRoot()) {
			return false;
		}
		if (deleteOnlyLeafs) {
			return selected.isLeaf();
		}
		
		return true;
	}

	@Override
	protected void onSubmit(AjaxRequestTarget target, Form form) {
		DefaultMutableTreeNode selectedNode = tree.getSelectedTreeNode();
		T selected = tree.getSelectedUserObject();
	
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) 
				selectedNode.getParent();
		T parent = tree.getDataTreeNode(parentNode);

		if (parent != null)
			parent.getChildren().remove(selected);
		parentNode.remove(selectedNode);
		
		Session session = Databinder.getHibernateSession();
		if (session.contains(selected)) {
			session.delete(selected);
			session.getTransaction().commit();
		}
		
		tree.getTreeState().selectNode(parentNode, true);
		tree.repaint(target);
		tree.updateDependentComponents(target, parentNode);
	}
}