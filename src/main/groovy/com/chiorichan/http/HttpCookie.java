/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http;

import com.chiorichan.utils.UtilHttp;
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
		this( cookie.name(), cookie.value() );
		setDomain( cookie.domain() );
		setExpiration( cookie.maxAge() );
		setPath( cookie.path() );
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
	 * @param epoch The Epoch since 1970
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
	 * @param maxLife The about of time in seconds
	 */
	public HttpCookie setMaxAge( long maxLife )
	{
		needsUpdating = true;
		epoch = Timings.epoch() + maxLife;
		return this;
	}

	public boolean isDomainSet()
	{
		return domain != null || !domain.isEmpty();
	}

	public HttpCookie setDomain( String domain )
	{
		needsUpdating = true;
		this.domain = domain == null ? "" : UtilHttp.normalize( domain );
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

		if ( domain != null && !domain.isEmpty() && !".".equals( domain ) )
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
