package net.databinder.components.tree.hib;

import javax.swing.tree.DefaultMutableTreeNode;


import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;


/**
 * Add a new child to the selected tree node.
 * 
 * @author Thomas Kappler
 */
public class DataTreeNewChildLink extends AjaxLink {

	private SingleSelectionDataTree<?> tree;

	public DataTreeNewChildLink(String id, SingleSelectionDataTree<?> tree) {
		super(id);
		this.tree = tree;
	}

	@Override
	public boolean isEnabled() {
		return tree.getSelectedTreeNode() != null;
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		DefaultMutableTreeNode newNode = tree.addNewChildNode(tree.getSelectedTreeNode());
		tree.getTreeState().selectNode(newNode, true);
		tree.repaint(target);
		tree.updateDependentComponents(target, newNode);
	}
}