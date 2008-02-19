package net.databinder.components.tree.hib;

import javax.swing.tree.DefaultMutableTreeNode;


import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.tree.AbstractTree;


/**
 * Create a new tree node on the top level.
 * 
 * <p>
 * The behaviour of this action depends on whether there is a root node present
 * or not, and on whether the tree is rootless (see {@link AbstractTree}) or
 * not.
 * <table>
 * <tr> <td></td> <td><b>No root present</b></td> <td><b>Root present</b></td> </tr>
 * <tr> <td><b>Rootless</b></td> 
 *      <td>Should not happen, user must manage a root node.  We could clear the 
 *          tree, creating a new root node, and add a new child to the new root 
 *          node, but that assumes a bit much.</td> 
 *      <td>Create a new child of the (invisible) root.</td> </tr>
 * <tr> <td><b>Root shown</b></td> <td>Create a new root node.</td> <td>Not enabled.</td> </tr>
 * </table>
 * </p>
 * 
 * @author Thomas Kappler
 */
public class DataTreeNewToplevelLink extends AjaxLink {

	private DataTree<?> tree;
	
	public DataTreeNewToplevelLink(String id, DataTree<?> tree) {
		super(id);
		this.tree = tree;
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		/* Conditions synchronized with isEnabled() */
		if (tree.isRootLess()) {
			DefaultMutableTreeNode newNode = tree.addNewChildNode(tree.getRootNode());
			tree.getTreeState().selectNode(newNode, true);
			tree.repaint(target);
			tree.updateDependentComponents(target, newNode);
		} else {
			DefaultMutableTreeNode newRootNode = tree.clear(target);
			tree.updateDependentComponents(target, newRootNode);
		}
	}

	/**
	 * For trees where the root is displayed, the link is enabled if no root
	 * node exists yet. For rootless trees, it is always enabled, as these
	 * appear to the user as forests and thus allow several "top level" nodes;
	 * only if the invisible root was somehow deleted, it is not enabled.
	 * 
	 * @see javax.swing.tree.TreeModel
	 * @see org.apache.wicket.Component#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return (tree.isRootLess() && tree.getRootNode() != null)
				|| (!tree.isRootLess() && tree.getRootNode() == null);
	}
}