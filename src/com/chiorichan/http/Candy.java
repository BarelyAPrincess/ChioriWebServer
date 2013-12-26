package com.chiorichan.http;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Candy
{
	private String key, value, path = "/", domain = "";
	private long epoch = 0;
	
	/*
	 * Tells the response writer if it needs to set this candy in the response headers.
	 */
	private Boolean needsUpdating = false;
	
	public Candy(String _key, String _value)
	{
		key = _key;
		value = _value;
	}
	
	public boolean compareTo( Candy var1 )
	{
		return compareTo( var1, false );
	}
	
	public boolean compareTo( Candy var1, boolean compareKey )
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
	 * @param _epoch
	 */
	public void setExpiration( long _epoch )
	{
		needsUpdating = true;
		epoch = _epoch;
	}
	
	/**
	 * Sets an explicit expiration time using the current epoch + timeSpecified
	 * @param defaultLife
	 */
	public void setMaxAge( long defaultLife )
	{
		needsUpdating = true;
		epoch = (System.currentTimeMillis() / 1000) + defaultLife;
	}
	
	public void setDomain( String _domain )
	{
		needsUpdating = true;
		domain = _domain;
	}
	
	public void setPath( String _path )
	{
		needsUpdating = true;
		path = _path;
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
			SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd-MMM-yyyy HH:mm:ss zz");
			additional += "expires=" + dateFormat.format( new Date( epoch * 1000 ) ) + "; ";
		}
		
		if ( !path.isEmpty() )
			additional += "path=" + path + "; ";
		
		if ( !domain.isEmpty() )
			additional += "domain=" + domain + "; ";
		
		return key + "=" + value + "; " + additional;
	}
}
