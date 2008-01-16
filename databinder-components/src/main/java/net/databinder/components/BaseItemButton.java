package net.databinder.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.form.ImageButton;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Base class for item buttons, whether ListItem or repeater Item.
 */
public abstract class BaseItemButton extends ImageButton {

	public BaseItemButton(String id, ResourceReference image) {
		super(id, image);
		add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
			@Override
			public Object getObject() {
				return isEnabled() ? null : "disabled-image";
			}
		}));
	}
	
	protected static ResourceReference getTrashImage() {
		return new ResourceReference(BaseItemButton.class, "image/trash.png");
	}
}
