package net.databinder.components;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;

public class SourceList {
	List<SourceLink> links = new LinkedList<SourceLink>();
	SourceLink current;
	
	public class SourceLink extends Link {
		Component target;
		public SourceLink(String id, Component target) {
			super(id);
			this.target = target;
			links.add(this);
			target.setVisible(false);
		}
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
