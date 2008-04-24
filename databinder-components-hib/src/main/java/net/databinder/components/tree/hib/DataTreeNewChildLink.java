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

	private DataTree<?> tree;
	private DefaultMutableTreeNode parentNode;

	public DataTreeNewChildLink(String id, DataTree tree, DefaultMutableTreeNode node) {
		super(id);
		this.tree = tree;
		this.parentNode = node;
	}
	
	protected DefaultMutableTreeNode getParentNode() {
		return parentNode;
	}

	@Override
	public boolean isEnabled() {
		return getParentNode() != null;
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		DefaultMutableTreeNode newNode = tree.addNewChildNode(getParentNode());
		tree.getTreeState().selectNode(newNode, true);
		tree.repaint(target);
		tree.updateDependentComponents(target, newNode);
	}
	
	public static class SingleSelection extends DataTreeNewChildLink {
		private SingleSelectionDataTree<?> tree;

		public SingleSelection(String id, SingleSelectionDataTree<?> tree) {
			super(id, tree, null);
			this.tree = tree;
		}
		@Override
		protected DefaultMutableTreeNode getParentNode() {
			return tree.getSelectedTreeNode();
		}
	}
}