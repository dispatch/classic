package net.databinder.components.tree.hib;

import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;

import net.databinder.components.tree.data.DataTreeObject;
import net.databinder.models.hib.HibernateListModel;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.markup.html.tree.ITreeState;



/**
 * A {@link DataTree} in single selection mode (see {@link ITreeState}), with
 * methods to retrieve the selected node or its backing object.
 * 
 * @author Thomas Kappler
 * 
 * @param <T>
 *            see {@link DataTree}
 */
public abstract class SingleSelectionDataTree<T extends DataTreeObject<T>> extends DataTree<T> {

	public SingleSelectionDataTree(String id, HibernateObjectModel rootModel) {
		super(id, rootModel);
		getTreeState().setAllowSelectMultiple(false);
	}
	
	public SingleSelectionDataTree(String id, HibernateListModel childrenModel) {
		super(id, childrenModel);
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
		return selected;
	}
	
	/**
	 * Return the currently selected user object (of type T). 
	 * 
	 * @return the one currently selected T if any, else null
	 */
	public T getSelectedUserObject() {
		DefaultMutableTreeNode selectedNode = getSelectedTreeNode();
		if (selectedNode == null) {
			return null;
		}
		return getDataTreeNode(selectedNode);
	}
}
