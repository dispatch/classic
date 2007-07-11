package net.databinder.components;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class ObjectFilteredPropertyColumn extends ChoiceFilteredPropertyColumn {
	private ChoiceRenderer choiceRenderer;
	private String displayProperty;

	public ObjectFilteredPropertyColumn(IModel displayModel, String sortProperty, String displayProperty, String propertyExpression, String filterLabelProperty, IModel filterChoices) {
		super(displayModel,sortProperty,propertyExpression,filterChoices);
		choiceRenderer = new ChoiceRenderer(filterLabelProperty);
		this.displayProperty = displayProperty;
	}

	protected IChoiceRenderer getChoiceRenderer() {
		return choiceRenderer;
	}

	protected IModel createLabelModel(IModel embeddedModel) {
		return new PropertyModel(embeddedModel, displayProperty);
	}

	public Component getFilter(String componentId, FilterForm form) {
		ChoiceFilter cf = (ChoiceFilter) super.getFilter(componentId, form);
		cf.getChoice().setNullValid(true);
		return cf;
	}
}
