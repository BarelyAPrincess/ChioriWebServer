/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.util.ObjectUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class AccountMetaData
{
	Map<String, Object> metaData = Maps.newTreeMap( String.CASE_INSENSITIVE_ORDER );
	
	public AccountMetaData()
	{
		
	}
	
	public AccountMetaData( String username, String password, String acctId )
	{
		metaData.put( "username", username );
		metaData.put( "password", password );
		metaData.put( "acctId", acctId );
	}
	
	public boolean hasMinimumData()
	{
		return metaData.containsKey( "username" ) && metaData.containsKey( "password" ) && metaData.containsKey( "acctId" );
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
			if ( ( ( String ) obj ).isEmpty() )
				return def;
			else
				return Integer.parseInt( ( String ) obj );
		else
			return ( Integer ) obj;
	}
	
	public Boolean getBoolean( String key )
	{
		Object obj = metaData.get( key );
		
		if ( obj instanceof String )
			return Boolean.parseBoolean( ( String ) obj );
		else
			return ( Boolean ) obj;
	}
	
	public void setAll( Map<String, Object> data )
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
	
	public String getAcctId()
	{
		String uid = getString( "acctId" );
		
		if ( uid == null )
			uid = getString( "accountId" );
		
		/** TEMP START - MAYBE **/
		if ( uid == null )
			uid = getString( "userId" );
		
		if ( uid == null )
			uid = getString( "userID" );
		
		if ( uid == null )
			uid = getString( "id" );
		/** TEMP END **/
		
		return uid;
	}
	
	public Set<String> getKeys()
	{
		return metaData.keySet();
	}
	
	public void mergeData( AccountMetaData data )
	{
		for ( Entry<String, Object> entry : metaData.entrySet() )
			if ( !data.containsKey( entry.getKey() ) )
				metaData.remove( entry.getKey() );
		
		metaData.putAll( data.metaData );
	}
}
