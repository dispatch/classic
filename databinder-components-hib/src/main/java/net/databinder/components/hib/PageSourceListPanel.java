package net.databinder.components.hib;

import net.databinder.components.SourceListPanel;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

/**
 * Displays a list of PageSourceLinks to the selected page. The panel renders to an
 * unordered list of class <tt>source-list</tt>.
 * @see PageSourceLink
 * @author Nathan Hamblen
 */
public class PageSourceListPanel extends SourceListPanel {
	private Class<? extends Page> pageClass;
	private String idParameter;

	/**
	 * Creates list with identifiers bound to an "id" parameter.
	 * @param id component id
	 * @param page targeted page
	 * @param bodyProperty object property for link body text
	 * @param listModel list of entities to render
	 */
	public PageSourceListPanel(String id, Class<? extends Page> page, String bodyProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
	}

	/**
	 * Creates list with identifiers bound to the chosen parameter
	 * @param id component id
	 * @param page targeted page
	 * @param bodyProperty object property for link body text
	 * @param idParameter identifer passed through this parameter
	 * @param listModel list of entities to render
	 */
	public PageSourceListPanel(String id, Class<? extends Page> page, 
			String bodyProperty, String idParameter, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
		this.idParameter = idParameter;
	}

	/** Called from super-class to construct source links. Note: subclasses my override
	 * to add attribute modifiers to the Link object constructed here, for example. */
	@Override
	protected Link sourceLink(String id, final IModel model) {
		PageSourceLink link = new PageSourceLink(id, pageClass, model, idParameter) {
			@Override
			protected void setParameters() {
				super.setParameters();
				PageSourceListPanel.this.setParameters(this);
			}
		};
		return link;
	}
	/**
	 * Called before rendering links. Override to set custom link parameters (the id parameter will
	 * always be set before calling this method).
	 * @param link one link of the list, with model set correspondingly
	 */
	protected void setParameters(PageSourceLink link) { }
}
