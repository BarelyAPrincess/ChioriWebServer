package com.chiorichan.account.helpers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.chiorichan.util.ObjectUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class AccountMetaData
{
	Map<String, Object> metaData = Maps.newLinkedHashMap();
	
	public AccountMetaData()
	{
		
	}
	
	public AccountMetaData(String username, String password, String acctId)
	{
		metaData.put( "username", username );
		metaData.put( "password", password );
		metaData.put( "acctId", acctId );
	}
	
	public boolean hasMinimumData()
	{
		return metaData.containsKey( "username" ) && metaData.containsKey( "password" ) && metaData.containsKey( "acctId" );
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
		return ObjectUtil.castToString( metaData.get( key ) );
	}
	
	public Integer getInteger( String key )
	{
		return getInteger( key, 0 );
	}
	
	public Integer getInteger( String key, int def )
	{
		Object obj = metaData.get( key );
		
		if ( obj instanceof String )
			if ( ( (String) obj ).isEmpty() )
				return def;
			else
				return Integer.parseInt( (String) obj );
		else
			return (Integer) obj;
	}
	
	public Boolean getBoolean( String key )
	{
		Object obj = metaData.get( key );
		
		if ( obj instanceof String )
			return Boolean.parseBoolean( (String) obj );
		else
			return (Boolean) obj;
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
	
	public String getAccountId()
	{
		return getString( "acctId" );
	}
	
	public Set<String> getKeys()
	{
		return metaData.keySet();
	}
}
