package com.chiorichan.user;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class UserMetaData
{
	Map<String, Object> metaData = Maps.newLinkedHashMap();
	
	public boolean hasMinimumData()
	{
		return metaData.containsKey( "username" ) && metaData.containsKey( "password" ) && metaData.containsKey( "userId" );
	}
	
	public String getUsername()
	{
		return getString( "username" );
	}
	
	public String getPassword()
	{
		return getString( "password" );
	}
	
	public Object getObject( String key )
	{
		return metaData.get( key );
	}
	
	public void set( String key, Object obj )
	{
		metaData.put( key, obj );
	}
	
	public String getString( String key )
	{
		return (String) metaData.get( key );
	}
	
	public Integer getInteger( String key )
	{
		return (Integer) metaData.get( key );
	}
	
	public Boolean getBoolean( String key )
	{
		return (Boolean) metaData.get( key );
	}
	
	public void setAll( LinkedHashMap<String, Object> data )
	{
		metaData.putAll( data );
	}
	
	public String toString()
	{
		return Joiner.on( "," ).withKeyValueSeparator( "=" ).join( metaData );
	}

	public boolean containsKey( String key )
	{
		return metaData.containsKey( key );
	}
}
