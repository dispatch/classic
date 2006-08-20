package net.databinder.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Used to send browsers from the context root to the Wicket servlet.This should be configured
 * to match only requests to the context root, "/", as it will redirect anything landing on the
 * doFilter method.
 * 
 * @author Nathan Hamblen
 */
public class RedirectFilter implements Filter {
    private String redirectUrl;
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
		((HttpServletResponse)response).sendRedirect(redirectUrl);
    }
    
    public void destroy()
    {
        redirectUrl = null;
    }
    
    public void init(FilterConfig config) throws ServletException
    {
        redirectUrl = config.getInitParameter("redirectUrl");
    }
}
