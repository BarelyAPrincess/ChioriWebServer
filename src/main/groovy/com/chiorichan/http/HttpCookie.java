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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.chiorichan.util.CommonFunc;

public class HttpCookie
{
	private String key, value, path = "/", domain = "";
	private long epoch = 0;
	
	/*
	 * Tells the response writer if it needs to set this candy in the response headers.
	 */
	private Boolean needsUpdating = false;
	
	public HttpCookie( String key, String value )
	{
		this.key = key;
		this.value = value;
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
	
	/**
	 * Sets an explicit expiration time using an epoch.
	 * 
	 * @param epoch
	 */
	public void setExpiration( long epoch )
	{
		needsUpdating = true;
		this.epoch = epoch;
	}
	
	/**
	 * Sets an explicit expiration time using the current epoch + timeSpecified
	 * 
	 * @param defaultLife
	 */
	public void setMaxAge( long defaultLife )
	{
		needsUpdating = true;
		epoch = CommonFunc.getEpoch() + defaultLife;
	}
	
	public void setDomain( String domain )
	{
		needsUpdating = true;
		this.domain = domain;
	}
	
	public void setPath( String path )
	{
		needsUpdating = true;
		this.path = path;
	}
	
	protected Boolean needsUpdating()
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
			additional += "expires=" + dateFormat.format( new Date( epoch * 1000 ) ) + "; ";
		}
		
		if ( !path.isEmpty() )
			additional += "path=" + path + "; ";
		
		if ( !domain.isEmpty() )
			additional += "domain=" + domain + "; ";
		
		return key + "=" + value + "; " + additional;
	}
}
