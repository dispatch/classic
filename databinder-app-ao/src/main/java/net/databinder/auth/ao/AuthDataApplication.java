package net.databinder.auth.ao;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import net.databinder.ao.DataApplication;
import net.databinder.ao.Databinder;
import net.databinder.auth.AuthApplication;
import net.databinder.auth.AuthSession;
import net.databinder.auth.components.ao.DataSignInPage;
import net.databinder.auth.data.DataUser;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.authorization.strategies.role.IRoleCheckingStrategy;
import org.apache.wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;

public abstract class AuthDataApplication extends DataApplication implements IUnauthorizedComponentInstantiationListener, IRoleCheckingStrategy, AuthApplication {
	
	/**
	 * Internal initialization. Client applications should not normally override
	 * or call this method.
	 */
	@Override
	protected void internalInit() {
		super.internalInit();
		authInit();
	}
	
	/**
	 * Sets Wicket's security strategy for role authorization and appoints this 
	 * object as the unauthorized instatiation listener. Called automatically on start-up.
	 */
	protected void authInit() {
		getSecuritySettings().setAuthorizationStrategy(new RoleAuthorizationStrategy(this));
		getSecuritySettings().setUnauthorizedComponentInstantiationListener(this);
	}

	/**
	 * @return new AuthDataSession
	 * @see AuthDataSession
	 */
	@Override
	public Session newSession(Request request, Response response) {
		return new AuthDataSession((WebRequest) request);
	}
	
	/**
	 * Sends to sign in page if not signed in, otherwise throws UnauthorizedInstantiationException.
	 */
	public void onUnauthorizedInstantiation(Component component) {
		if (((AuthSession)Session.get()).isSignedIn()) {
			throw new UnauthorizedInstantiationException(component.getClass());
		}
		else {
			throw new RestartResponseAtInterceptPageException(getSignInPageClass());
		}	
	}
	
	/**
	 * Passes query on to the IUser object if signed in.
	 */
	public final boolean hasAnyRole(Roles roles) {
		DataUser user = ((AuthSession)Session.get()).getUser();
		return user == null ? false : user.hasAnyRole(roles);
	}

	/**
	 * Return user object by matching against a "username" property. Override
	 * if you have a differently named property.
	 * @return IUser for the given username. 
	 */
	@SuppressWarnings("unchecked")
	public DataUser getUser(String username) {
		try {
			Query q = Query.select().where("username = ?", username).limit(1);
			return (DataUser) Databinder.getEntityManager().find(
					(Class<? extends RawEntity>)getUserClass(),q)[0];
		} catch (SQLException e) {
			throw new WicketRuntimeException(e);
		}
	}

	/**
	 * Override if you need to customize the sign-in page.
	 * @return page to sign in users
	 */
	public Class< ? extends WebPage> getSignInPageClass() {
		return DataSignInPage.class;
	}
	
	/**
	 * Get the restricted token for a user, using IP addresses as location parameter. This implementation
	 * combines the "X-Forwarded-For" header with the remote address value so that unique
	 * values result with and without proxying. (The forwarded header is not trusted on its own
	 * because it can be most easily spoofed.)
	 * @param user source of token
	 * @return restricted token
	 */
	public String getToken(DataUser.CookieAuth user) {
		HttpServletRequest req = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest();
		String fwd = req.getHeader("X-Forwarded-For");
		if (fwd == null)
			fwd = "nil";
		return user.getToken(fwd + "-" + req.getRemoteAddr());
	}
}
