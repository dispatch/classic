package net.databinder.components;

import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * Place-holder object for base Panels that can be replaced with a component that will
 * generate markup.  
 * @author Nathan Hamblen
 */
public class NullSocket extends WebMarkupContainer {
	public NullSocket(String id) {
		super(id);
		setRenderBodyOnly(true);
	}
}
