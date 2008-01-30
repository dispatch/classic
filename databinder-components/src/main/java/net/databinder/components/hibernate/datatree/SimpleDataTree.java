package deafel.helpers.datatree;

import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * An implementation of {@link DataTree} offering additional convenience methods
 * at the price of fixing some assumptions: a {@link SimpleDataTree} is rootless
 * and disallows multiple selection.
 * 
 * @author Thomas Kappler
 * 
 * @param <T> the type of the objects being represented by the tree nodes
 */
public abstract class SimpleDataTree<T extends IDataTreeNode<T>> extends DataTree<T> {

	public SimpleDataTree(String id, T fakeRoot,
			Collection<T> firstLevelChildren) {
		super(id, fakeRoot, firstLevelChildren);

		setRootLess(true);
		getTreeState().setAllowSelectMultiple(false);
	}
	
	/**
	 * Depends on the tree disallowing multiple selection, which we
	 * configured in the constructor.
	 * 
	 * @return the currently selected tree node if any, else null
	 */
	public DefaultMutableTreeNode getSelectedTreeNode() {
		@SuppressWarnings("unchecked")
		Collection<DefaultMutableTreeNode> selectedNodes = 
				getTreeState().getSelectedNodes();
		if (selectedNodes.isEmpty()) {
			return null;
		}
		DefaultMutableTreeNode selected = (DefaultMutableTreeNode) 
				selectedNodes.iterator().next();
		if (selected.equals(getRootNode())) {
			return null;
		}
		return selected;
	}
	
	/**
	 * Return the currently selected user object (of type T). 
	 * 
	 * @return the one currently selected T if any, else null
	 */
	public T getSelectedUserObject() {
		DefaultMutableTreeNode selectedNode = getSelectedTreeNode();
		if (selectedNode == null || selectedNode.equals(getRootNode())) {
			return null;
		} else {
			return getObjectFromNode(selectedNode);
		}
	}

	/**
	 * Repaint the tree when something has changed. It possibly does too much,
	 * but you're safe that changes do show after you call it.
	 * 
	 * @see DataTree#repaint(AjaxRequestTarget)
	 * 
	 * @param target
	 * @param nodeToSelect a node to be selected
	 */
	public void repaint(AjaxRequestTarget target,
			DefaultMutableTreeNode nodeToSelect) {
		invalidateAll();
		getTreeState().selectNode(nodeToSelect, true);
		updateTree(target);
	}
}
