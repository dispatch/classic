package net.databinder.components;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;

/**
 * Generator for links to components (usually panels) that are only visible one at a time.
 * Similar to TabbedPanel, but the links and target component can be placed anywhere in 
 * the associated markup template. Ex:
 * <pre>
 * SourceList sl = new SourceList();
 * add(sl.new SourceLink("link-a", new MyPanel("panel-a")));
 * add(sl.new SourceLink("link-b", new MyOtherPanel("panel-b")));
 * </pre>
 * @author Nathan Hamblen
 */
public class SourceList implements Serializable {
	List<SourceLink> links = new LinkedList<SourceLink>();
	SourceLink current;
	
	/** 
	 * Link that that causes the paired component to be visible when clicked, and all other
	 * components in the SourceList to be invisible. Paired components are set to invisible
	 * on initialization; to make select a linked component programmatically, call onClick().  
	 */
	public class SourceLink extends Link {
		Component target;
		public SourceLink(String id, Component target) {
			super(id);
			this.target = target;
			links.add(this);
			target.setVisible(false);
		}
		/** return false when paired component is selected */
		@Override
		public boolean isEnabled() {
			return current != this;
		}
		@Override
		public void onClick() {
			if (current != null)
				current.getTarget().setVisible(false);
			current = this;
			target.setVisible(true);
		}
		Component getTarget() { return target; } 
	}
}
