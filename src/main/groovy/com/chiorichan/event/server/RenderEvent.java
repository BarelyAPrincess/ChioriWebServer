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

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.framework.Site;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.http.session.SessionProvider;

public class RenderEvent extends ServerEvent
{
	private String pageSource, pageHash;
	private final SessionProvider sess;
	private final Map<String, String> pageData;
	
	public RenderEvent( SessionProvider sess, String source, Map<String, String> pageData )
	{
		pageSource = source;
		pageHash = DigestUtils.md5Hex( source );
		this.pageData = pageData;
		this.sess = sess;
	}
	
	public Map<String, String> getPageData()
	{
		return pageData;
	}
	
	public Site getSite()
	{
		if ( sess == null || sess.getRequest() == null )
			return null;
		
		return sess.getRequest().getSite();
	}
	
	public String getRequestId()
	{
		return sess.getParentSession().getId();
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
	
	public String getSource()
	{
		return pageSource;
	}
	
	public void setSource( String source )
	{
		pageSource = source;
	}
	
	public boolean sourceChanged()
	{
		return !DigestUtils.md5( pageSource ).equals( pageHash );
	}
}
