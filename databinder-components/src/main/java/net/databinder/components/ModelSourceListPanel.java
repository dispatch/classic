package net.databinder.components;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

/**
 * Displays a list of links to set the model of a target component. The panel renders to an
 * unordered list of class <tt>source-list</tt>. 
 * @author Nathan Hamblen
 */
public class ModelSourceListPanel extends SourceListPanel {
	private Component target;
	/**
	 * Creates list panel.
	 * @param id component id
	 * @param target sets model of this component
	 * @param bodyProperty object property for link body text
	 * @param listModel list of entities to render
	 */
	public ModelSourceListPanel(String id, Component target, String bodyProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.target = target;
	}
	/** Called from super-class to construct source links. Note: subclasses my override
	 * to add attribute modifiers to the Link object constructed here, for example. */
	@Override
	protected Link sourceLink(String id, IModel model) {
		return new ModelSourceLink("link", target, model);
	}
}
