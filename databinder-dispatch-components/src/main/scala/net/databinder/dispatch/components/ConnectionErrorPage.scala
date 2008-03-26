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
package net.databinder.dispatch.components

import java.net.URI

import javax.servlet.http.HttpServletRequest

import org.apache.wicket.ResourceReference
import org.apache.wicket.markup.html.WebPage
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.link.Link
import org.apache.wicket.markup.html.link.ResourceLink
import org.apache.wicket.model.Model
import org.apache.wicket.protocol.http.WebRequest

/**
 * Page describing connection problem and offering source for external script.
 * @author Nathan Hamblen
 */
class ConnectionErrorPage(e: Throwable) extends WebPage {

  add(new Label("error", new Model(e.getMessage())))
  add(new Link("retry") {
    override def onClick() { continueToOriginalDestination() }
  })
  add(new ResourceLink("script", ConnectionErrorPage.scriptFile))

  def req = getRequest.asInstanceOf[WebRequest].getHttpServletRequest
  val full= URI.create(req.getRequestURL().toString())
  val path = new Model(full.resolve(req.getContextPath() + "/" +
    urlFor(ConnectionErrorPage.scriptFile)))

  add(new Label("href", path).setRenderBodyOnly(true))
}

object ConnectionErrorPage {
  val scriptFile = new ResourceReference(classOf[ConnectionErrorPage], "databinder-dispatch.rb")
}
