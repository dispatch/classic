package net.databinder.components.hibernate.datatree.controllinks;

import net.databinder.components.hibernate.datatree.DataTree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;


/**
 * Collapse all tree nodes.
 * 
 * @author Thomas Kappler
 */
public class DataTreeCollapseAllLink extends AjaxLink {

	private DataTree<?> tree;
	
	public DataTreeCollapseAllLink(String id, DataTree<?> tree) {
		super(id);
		this.tree = tree;
	}

	@Override
	public void onClick(AjaxRequestTarget target) {
		tree.getTreeState().collapseAll();
		tree.updateTree(target);
	}
}