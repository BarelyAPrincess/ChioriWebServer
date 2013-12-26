package com.chiorichan.http;

public class Candy
{
	private String key, value, path = "/", domain = "";
	private long epoch;
	
	/*
	 * Tells the response writer if it needs to set this candy in the response headers.
	 */
	private Boolean needsUpdating = false;
	
	public Candy(String _key, String _value)
	{
		key = _key;
		value = _value;
		
		epoch = System.currentTimeMillis();
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
		epoch = System.currentTimeMillis() + defaultLife;
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
		// h.add( "Set-Cookie", "lastVisit=99999999; Expires=Wed, 09 Jun 2021 10:18:14 GMT" );
		
		// TODO: Add expires, path, domain
		
		return key + "=" + value + "; ";
	}
}
