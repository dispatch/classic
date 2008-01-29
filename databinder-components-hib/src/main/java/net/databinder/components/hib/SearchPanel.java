package net.databinder.components.hib;

import net.databinder.components.AjaxOnKeyPausedUpdater;
import net.databinder.components.StyleLink;
import net.databinder.components.Wrapper;
import net.databinder.models.QueryBinder;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hibernate.Query;

/**
 * Panel for a "live" search field with a clear button. The SearchPanel's model object
 * is linked to the text of the search field inside it. Instances of this class must
 * implement the onUpdate method to register external components for updating.
 * It is possible to override the search button text with the key "searchbutton.text" 
 * @author Nathan Hamblen
 */
public abstract class SearchPanel extends Panel {
	
	/**
	 * @param id Wicket id
	 * @param searchModel model to receive search string
	 */
	public SearchPanel(String id, IModel searchModel) {
		super(id, searchModel);
		add(new SearchForm("searchForm", searchModel));
		add(new StyleLink("searchStylesheet", SearchPanel.class));
	}

	
	/**
	 * Override to add components to be updated (or JavaScript to be executed)
	 * when the search string changes. Remember that added components must
	 * have a markup id; use component.setMarkupId(true) to assign one 
	 * programmatically.
	 * @param target Ajax target to register components for update
	 */
	public abstract void onUpdate(AjaxRequestTarget target);
	
	/**
	 * Binds the search model to a "search" parameter in a query. The value in the 
	 * search field will be bracketed by percent signs (%) for a find-anywhere match.
	 * In the query itself, "search" must be the name of the  one and only parameter 
	 * If your needs differ, bind the model passed in to the SearchPanel constructor 
	 * to your own IQueryBinder instance; this is a convenience method. 
	 * @return binder for a "search" parameter
	 */
	public QueryBinder getQueryBinder() {
		return new QueryBinder() {
			public void bind(Query query) {
				query.setString("search", getModelObject() == null ? 
						null : "%" + getModelObject() + "%");
			}
		};
	}
	
	/** Form with AJAX components and their wrappers. */
	public class SearchForm extends Form {
		public SearchForm(String id, IModel searchModel) {
			super(id);

			final Wrapper searchWrap = new Wrapper("searchWrap");
			add(searchWrap);
			final TextField search = new TextField("search", searchModel);
			search.setOutputMarkupId(true);
			searchWrap.add(search);

			final Wrapper clearWrap = new Wrapper("clearWrap");
			add(clearWrap);
			final AjaxLink clearLink = new AjaxLink("clearLink") {
				/** Clear field and register updates. */
				public void onClick(AjaxRequestTarget target) {
					search.setModelObject(null);
					target.addComponent(searchWrap);
					target.addComponent(clearWrap);
					SearchPanel.this.onUpdate(target);
				}
				/** Hide when search is blank. */
				public boolean isVisible() {
					return search.getModelObject() != null;
				}
			};
			clearLink.setOutputMarkupId(true);
			clearLink.add( new Image("clear", 
					new ResourceReference(this.getClass(), "clear.png")));
			clearWrap.add(clearLink);

			// triggered when user pauses or tabs out
			search.add(new AjaxOnKeyPausedUpdater() {
				protected void onUpdate(AjaxRequestTarget target) {
					target.addComponent(clearWrap);
					SearchPanel.this.onUpdate(target);
				}
			});
		}
	}
}
