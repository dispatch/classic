package net.databinder.components;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.PropertyResolver;

public class PageSourceListPanel extends SourceListPanel {
	private Class<? extends Page> pageClass;
	private String idProperty;

	public PageSourceListPanel(String id, Class<? extends Page> page, 
			String bodyProperty, String idProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
		this.idProperty = idProperty;
	}

	public PageSourceListPanel(String id, Class<? extends Page> page, String bodyProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.pageClass = page;
	}

	@Override
	protected Link sourceLink(String id, final IModel model) {
		BookmarkablePageLink link = new BookmarkablePageLink(id, pageClass) {
			@Override
			public boolean isEnabled() {
				Object o = getPage().getModelObject();
				return o == null || !o.equals(model.getObject());
			}
		};
		setParameters(link, model);
		return link;
	}
	
	protected void setParameters(BookmarkablePageLink link, IModel model) {
		if (idProperty != null)
			link.setParameter(idProperty, 
					PropertyResolver.getValue(idProperty, model.getObject()).toString());
	}
}
