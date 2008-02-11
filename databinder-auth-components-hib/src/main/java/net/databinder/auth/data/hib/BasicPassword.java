package net.databinder.auth.data.hib;

import java.io.Serializable;
import java.security.MessageDigest;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.databinder.auth.AuthApplication;
import net.databinder.auth.data.DataPassword;

import org.apache.wicket.Application;
import org.apache.wicket.util.crypt.Base64;

/**
 * Simple, optional implementation of {@link DataPassword}.
 * @author Nathan Hamblen
 */
@Embeddable
public class BasicPassword implements DataPassword, Serializable {
	private String passwordHash;
	
	public BasicPassword() { }
	
	public BasicPassword(String password) {
		change(password);
	}
	
	public void change(String password) {
		MessageDigest md = ((AuthApplication)Application.get()).getDigest();
		byte[] hash = md.digest(password.getBytes());
		passwordHash = new String(Base64.encodeBase64(hash));
	}
	
	public void update(MessageDigest md) {
		md.update(passwordHash.getBytes());
	}
	
	@Column(length = 28, nullable = false)
	private String getPasswordHash() {
		return passwordHash;
	}

	@SuppressWarnings("unused")
	private void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	public boolean matches(String password) {
		return passwordHash != null &&
			passwordHash.equals(new BasicPassword(password).getPasswordHash());
	}
}
