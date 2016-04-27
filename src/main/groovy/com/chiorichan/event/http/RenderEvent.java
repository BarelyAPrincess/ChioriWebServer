/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.http;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.Map;

import com.chiorichan.event.application.ApplicationEvent;
import com.chiorichan.http.HttpHandler;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.session.Session;
import com.chiorichan.site.Site;
import com.google.common.base.Charsets;

public class RenderEvent extends ApplicationEvent
{
	private ByteBuf source;
	private final HttpHandler handler;
	private final Map<String, String> params;
	private Charset encoding = Charsets.UTF_8;

	public RenderEvent( HttpHandler handler, ByteBuf source, Charset encoding, Map<String, String> params )
	{
		this.handler = handler;
		this.source = source;
		this.encoding = encoding;
		this.params = params;
	}

	public Charset getEncoding()
	{
		return encoding;
	}

	public Map<String, String> getParams()
	{
		return params;
	}

	public HttpRequestWrapper getRequest()
	{
		return handler.getRequest();
	}

	public String getRequestId()
	{
		return handler.getSession().getSessId();
	}

	public HttpResponseWrapper getResponse()
	{
		return handler.getResponse();
	}

	public Session getSession()
	{
		return handler.getSession();
	}

	public Site getSite()
	{
		if ( handler == null || handler.getRequest() == null )
			return null;

		return handler.getRequest().getLocation();
	}

	public ByteBuf getSource()
	{
		return source.copy();
	}

	public void setSource( ByteBuf source )
	{
		this.source = source;
	}
}
