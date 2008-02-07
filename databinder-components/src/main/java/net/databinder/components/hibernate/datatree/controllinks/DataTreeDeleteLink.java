package net.databinder.components.hibernate.datatree.controllinks;

import javax.swing.tree.DefaultMutableTreeNode;

import net.databinder.components.hibernate.datatree.IDataTreeNode;
import net.databinder.components.hibernate.datatree.SingleSelectionDataTree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;


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
public class DataTreeDeleteLink<T extends IDataTreeNode<T>> extends AjaxLink {

	private SingleSelectionDataTree<T> tree;
	private boolean deleteOnlyLeafs = true;
	
	public DataTreeDeleteLink(String id, SingleSelectionDataTree<T> tree) {
		super(id);
		this.tree = tree;
	}

	public DataTreeDeleteLink(String id, SingleSelectionDataTree<T> tree,
			boolean deleteOnlyLeafs) {
		super(id);
		this.tree = tree;
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
	public void onClick(AjaxRequestTarget target) {
		DefaultMutableTreeNode selectedNode = tree.getSelectedTreeNode();
		T selected = tree.getSelectedUserObject();
	
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) 
				selectedNode.getParent();
		T parent = tree.getObjectFromNode(parentNode);
		parent.removeChild(selected);
		
		selectedNode.removeFromParent();
		
		tree.getTreeState().selectNode(parentNode, true);
		tree.repaint(target);
		tree.updateDependentComponents(target, parentNode);
	}
}