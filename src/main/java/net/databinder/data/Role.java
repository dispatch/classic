package net.databinder.data;

import javax.persistence.Embeddable;

@Embeddable
public class Role {
	private String role;

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
}
