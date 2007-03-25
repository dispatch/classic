/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.databinder.auth.components;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import net.databinder.auth.util.EqualPasswordConvertedInputValidator;

import wicket.ResourceReference;
import wicket.WicketRuntimeException;
import wicket.behavior.AttributeAppender;
import wicket.markup.html.IHeaderContributor;
import wicket.markup.html.IHeaderResponse;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.PasswordTextField;
import wicket.markup.html.resources.JavascriptResourceReference;
import wicket.model.AbstractReadOnlyModel;
import wicket.model.IModel;
import wicket.util.convert.ConversionException;
import wicket.util.crypt.Base64;

/**
 * Note: if equal password validation is need, use EqualPasswordConvertedInputValidator.
 * Equal password inputs are not equal until converted (decrypted).
 * 
 * @see EqualPasswordConvertedInputValidator
 */
public class RSAPasswordTextField extends PasswordTextField implements IHeaderContributor {
	private static final ResourceReference RSA_JS = new JavascriptResourceReference(
			RSAPasswordTextField.class, "RSA.js");
	private static final ResourceReference BARRETT_JS = new JavascriptResourceReference(
			RSAPasswordTextField.class, "Barrett.js");
	private static final ResourceReference BIGINT_JS = new JavascriptResourceReference(
			RSAPasswordTextField.class, "BigInt.js");

	private String challenge;
	
	/** 1024 RSA key, generated on first access. */
	private static KeyPair keypair;
	static {
		KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
	        keypair = keyGen.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new WicketRuntimeException("Can't find RSA provider", e);
		}
	}
	public RSAPasswordTextField(String id, Form form) {
		super(id);
		init(form);
	}
	public RSAPasswordTextField(String id, IModel model, Form form) {
		super(id, model);
		init(form);
	}
	protected void init(Form form) {
		setOutputMarkupId(true);

		form.add(new AttributeAppender("onsubmit", new AbstractReadOnlyModel() {
			public Object getObject() {
				StringBuilder eventBuf = new StringBuilder();
				eventBuf
					.append("if (")
					.append(getElementValue())
					.append(" != null && ")
					.append(getElementValue())
					.append(" != '') ")
					.append(getElementValue())
					.append(" = encryptedString(key, ")
					.append(getChallengeVar())
					.append("+ '|' + ")
					.append(getElementValue())
					.append(");");

				return eventBuf.toString();
			}
		}, ""));
		
		challenge = new String(Base64.encodeBase64(
				BigInteger.valueOf(new SecureRandom().nextLong()).toByteArray()));
	}
	
	@Override
	protected Object convertValue(String[] value) throws ConversionException {
		String enc = (String) super.convertValue(value);
		if (enc == null)
			return null;
		try {
			Cipher rsa = Cipher.getInstance("RSA");
			rsa.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
			String dec = new String(rsa.doFinal(hex2data(enc)));
			
			String[] toks = dec.split("\\|", 2);
			if (toks.length != 2 || !toks[0].equals(challenge))
				return null;

			return toks[1];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void renderHead(IHeaderResponse response) {
		response.renderJavascriptReference(BIGINT_JS);
		response.renderJavascriptReference(BARRETT_JS);
		response.renderJavascriptReference(RSA_JS);

        RSAPublicKey pub= (RSAPublicKey)keypair.getPublic();
		StringBuilder keyBuf = new StringBuilder();

		// the key is unique per app instance, send once
		keyBuf
			.append("setMaxDigits(131);\nvar key= new RSAKeyPair('")
			.append(pub.getPublicExponent().toString(16))
			.append("', '', '")
			.append(pub.getModulus().toString(16))
			.append("');");
		response.renderJavascript(keyBuf.toString(), "rsa_key");
		
		// the challeng is unique per component instance, send for every component
		StringBuilder chalBuf = new StringBuilder();
		chalBuf
			.append("var ")
			.append(getChallengeVar())
			.append(" = '")
			.append(challenge)
			.append("';");
		response.renderJavascript(chalBuf.toString(), null);
	}
	
	protected String getChallengeVar() {
		return (getMarkupId() + "_challenge");
	}
	
	protected String getElementValue() {
		return "document.getElementById('" + getMarkupId() + "').value ";
	}
	
	// these two functions LGPL, origin:
	//	 C-JDBC: Clustered JDBC.
	//	 Copyright (C) 2002-2004 French National Institute For Research In Computer
	//	 Science And Control (INRIA).
	//	 Contact: c-jdbc@objectweb.org
	// could be replaced by org.apache.commons.codec.binary.Hex
	private static final byte[] hex2data(String str)
	{
		if (str == null)
			return new byte[0];

		int len = str.length();
		char[] hex = str.toCharArray();
		byte[] buf = new byte[len / 2];

		for (int pos = 0; pos < len / 2; pos++)
			buf[pos] = (byte) (((toDataNibble(hex[2 * pos]) << 4) & 0xF0) | (toDataNibble(hex[2 * pos + 1]) & 0x0F));

		return buf;
	}
	private  static byte toDataNibble(char c)
	{
		if (('0' <= c) && (c <= '9'))
			return (byte) ((byte) c - (byte) '0');
		else if (('a' <= c) && (c <= 'f'))
			return (byte) ((byte) c - (byte) 'a' + 10);
		else if (('A' <= c) && (c <= 'F'))
			return (byte) ((byte) c - (byte) 'A' + 10);
		else
			return -1;
	}
}