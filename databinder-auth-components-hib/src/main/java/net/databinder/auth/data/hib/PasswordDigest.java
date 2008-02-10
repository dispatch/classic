package net.databinder.auth.data.hib;

import java.security.MessageDigest;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import net.databinder.auth.AuthApplication;

import org.apache.wicket.Application;
import org.apache.wicket.util.crypt.Base64;

@Embeddable
public class PasswordDigest {
	private String passwordHash;
	
	private PasswordDigest() { }
	
	public PasswordDigest(String password) {
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

	private void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PasswordDigest))
			return false;
		return passwordHash.equals(((PasswordDigest)obj).getPasswordHash());
	}
	@Override
	public int hashCode() {
		return passwordHash.hashCode();
	}
}
