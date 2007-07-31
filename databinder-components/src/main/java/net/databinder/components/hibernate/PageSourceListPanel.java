package net.databinder.components.hibernate;

import net.databinder.components.SourceListPanel;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

public class PageSourceListPanel extends SourceListPanel {
	private Class<? extends Page> pageClass;
	private String idParameter;

	public PageSourceListPanel(String id, Class<? extends Page> page, String bodyProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
	}

	public PageSourceListPanel(String id, Class<? extends Page> page, 
			String bodyProperty, String idParameter, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
		this.idParameter = idParameter;
	}

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
