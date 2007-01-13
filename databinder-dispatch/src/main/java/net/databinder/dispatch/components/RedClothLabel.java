package net.databinder.dispatch.components;

import wicket.model.IModel;


public class RedClothLabel extends XmlRpcLabel {
	public RedClothLabel(String id) {
		super(id, new RedClothConverter());
	}
	public RedClothLabel(String id, IModel model) {
		super(id, model, new RedClothConverter());
	}
	protected static class RedClothConverter extends XmlRpcConverter {
		@Override
		protected String getMethodName() {
			return "redcloth.to_html";
		}
	}
}
