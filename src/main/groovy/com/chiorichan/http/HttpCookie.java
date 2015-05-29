/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.Cookie;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.chiorichan.tasks.Timings;

public class HttpCookie
{
	private String key, value, path = "/", domain = "";
	private long epoch = 0;
	private boolean secure, httpOnly = true;
	
	/*
	 * Tells the response writer if it needs to set this candy in the response headers.
	 */
	private boolean needsUpdating = false;
	
	public HttpCookie( String key, String value )
	{
		this.key = key;
		this.value = value;
	}
	
	public HttpCookie( Cookie cookie )
	{
		this( cookie.getName(), cookie.getValue() );
		setDomain( cookie.getDomain() );
		setExpiration( cookie.getMaxAge() );
		setPath( cookie.getPath() );
		needsUpdating = false;
	}
	
	public boolean compareTo( HttpCookie var1 )
	{
		return compareTo( var1, false );
	}
	
	public boolean compareTo( HttpCookie var1, boolean compareKey )
	{
		if ( var1 == null )
			return false;
		
		if ( var1.getValue().equals( getValue() ) )
		{
			if ( compareKey )
			{
				return var1.getKey().equals( getKey() );
			}
			else
			{
				return true;
			}
		}
		
		return false;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public String getKey()
	{
		return key;
	}
	
	public void setKey( String key )
	{
		needsUpdating = true;
		this.key = key;
	}
	
	/**
	 * Sets an explicit expiration time using an epoch.
	 * 
	 * @param epoch
	 */
	public HttpCookie setExpiration( long epoch )
	{
		needsUpdating = true;
		this.epoch = epoch;
		return this;
	}
	
	/**
	 * Sets an explicit expiration time using the current epoch + seconds specified
	 * 
	 * @param defaultLife
	 */
	public HttpCookie setMaxAge( long defaultLife )
	{
		needsUpdating = true;
		epoch = Timings.epoch() + defaultLife;
		return this;
	}
	
	public boolean isDomainSet()
	{
		return domain != null || !domain.isEmpty();
	}
	
	public HttpCookie setDomain( String domain )
	{
		needsUpdating = true;
		this.domain = domain == null ? "" : domain.toLowerCase();
		return this;
	}
	
	public boolean isPathSet()
	{
		return path != null || !path.isEmpty();
	}
	
	public HttpCookie setPath( String path )
	{
		needsUpdating = true;
		this.path = path == null ? "" : path;
		return this;
	}
	
	protected boolean needsUpdating()
	{
		return needsUpdating;
	}
	
	protected String toHeaderValue()
	{
		String additional = "";
		
		if ( epoch > 0 )
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat( "EE, dd-MMM-yyyy HH:mm:ss zz" );
			dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
			additional += "; expires=" + dateFormat.format( new Date( epoch * 1000 ) );
		}
		
		if ( path != null && !path.isEmpty() )
			additional += "; path=" + path;
		else
			additional += "; path=/";
		
		if ( domain != null && !domain.isEmpty() )
			additional += "; domain=" + domain;
		
		if ( secure )
			additional += "; secure";
		
		if ( httpOnly )
			additional += "; HttpOnly";
		
		return key + "=" + value + additional;
	}
	
	public boolean isSecure()
	{
		return secure;
	}
	
	public HttpCookie setSecure( boolean secure )
	{
		this.secure = secure;
		return this;
	}
	
	public HttpCookie setHttpOnly( boolean httpOnly )
	{
		this.httpOnly = httpOnly;
		return this;
	}
	
	public String getDomain()
	{
		return domain;
	}
}
