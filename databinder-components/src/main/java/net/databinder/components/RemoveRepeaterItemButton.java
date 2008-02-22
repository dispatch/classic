package net.databinder.components;

import java.util.Collection;

import org.apache.wicket.markup.repeater.Item;

public class RemoveRepeaterItemButton extends RepeaterItemButton {
	
	@Override
	public boolean isEnabled() {
		Collection<?> c = (Collection<?>) getView().getModelObject();
		return c.size() > minElements;
	}

	private Integer minElements = 0;

	public RemoveRepeaterItemButton(String id, Item item) {
		super(id, item, getTrashImage());
	}
	
	public RemoveRepeaterItemButton(String id, Item item, Integer minElements) {
		this(id, item);
		this.minElements = minElements;
	}
   
	@Override
	public void onSubmit() {
		Collection c = (Collection) getView().getModelObject();
		if (c != null) {
			if (c.size() > minElements) {
				getView().modelChanging();
				// depends on correct equals()!
				c.remove(item.getModelObject());
				getView().modelChanged();
			}
		}
	}
}