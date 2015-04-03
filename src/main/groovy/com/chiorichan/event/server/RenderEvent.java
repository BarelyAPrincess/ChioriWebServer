/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.server;

import io.netty.buffer.ByteBuf;

import java.util.Map;

import com.chiorichan.framework.Site;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.session.SessionProvider;

public class RenderEvent extends ServerEvent
{
	private ByteBuf source;
	private final SessionProvider sess;
	private final Map<String, String> params;
	private String encoding = "UTF-8";
	
	public RenderEvent( SessionProvider sess, ByteBuf source, String encoding, Map<String, String> params )
	{
		this.sess = sess;
		this.source = source;
		this.encoding = encoding;
		this.params = params;
	}
	
	public Map<String, String> getParams()
	{
		return params;
	}
	
	public Site getSite()
	{
		if ( sess == null || sess.getRequest() == null )
			return null;
		
		return sess.getRequest().getSite();
	}
	
	public String getRequestId()
	{
		return sess.getParentSession().getSessId();
	}
	
	public SessionProvider getSession()
	{
		return sess;
	}
	
	public HttpRequestWrapper getRequest()
	{
		return sess.getRequest();
	}
	
	public HttpResponseWrapper getResponse()
	{
		return sess.getResponse();
	}
	
	public ByteBuf getSource()
	{
		return source.copy();
	}
	
	public void setSource( ByteBuf source )
	{
		this.source = source;
	}
	
	public String getEncoding()
	{
		return encoding;
	}
}
