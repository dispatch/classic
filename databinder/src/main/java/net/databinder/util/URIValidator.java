package net.databinder.util;

import java.net.URI;
import java.util.regex.Pattern;

import wicket.markup.html.form.FormComponent;
import wicket.markup.html.form.validation.AbstractValidator;

/**
 * Validate that the input is a particular kind of URI.
 * @author Nathan Hamblen
 */
public abstract class URIValidator extends AbstractValidator {

	public void validate(final FormComponent formComponent)
	{
		onValidate(formComponent, (URI)formComponent.getConvertedInput());
	}

	public abstract void onValidate(FormComponent formComponent, URI uri);

	/**
	 * Accepts only URIs having an http or https scheme.
	 * @return validator for http and https URIs.
	 */
	public static URIValidator HttpScheme() {
		return new SchemeValidator("http(s)?", "http");
	}

	/**
	 * Accepts only URIs having an ftp scheme.
	 * @return validator for ftp URIs.
	 */
	public static URIValidator FtpScheme() {
		return new SchemeValidator("ftp", "ftp");
	}

	private static class SchemeValidator extends URIValidator {
		Pattern pattern;
		String resourceKeySuffix;
		
		public SchemeValidator(String pattern, String resourceKeySuffix) {
			this.pattern = Pattern.compile(pattern);
			this.resourceKeySuffix = resourceKeySuffix;
		}
		
		@Override
		public void onValidate(FormComponent formComponent, URI uri) {
			{
				if (uri.getScheme() == null || !pattern.matcher(uri.getScheme()).matches())
					error(formComponent);
			}
		}
		@Override
		protected String resourceKey(FormComponent formComponent) {
			return "URIValidator." + resourceKeySuffix;
		}
	}

}
