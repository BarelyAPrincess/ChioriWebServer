/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter.memory;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;

public class MemoryAccount extends Account
{
	protected final MemoryAdapter lookupAdapter;
	
	public MemoryAccount( AccountMetaData meta, MemoryAdapter adapter ) throws LoginException
	{
		super( meta );
		
		lookupAdapter = adapter;
		adapter.cacheAcct( meta );
	}
	
	public MemoryAccount( String userId, MemoryAdapter adapter ) throws LoginException
	{
		super( userId, adapter );
		
		lookupAdapter = adapter;
		adapter.cacheAcct( metaData );
	}
	
	@Override
	public void preLoginCheck() throws LoginException
	{
		
	}
	
	@Override
	public void postLoginCheck() throws LoginException
	{
		
	}
	
	@Override
	public boolean isYou( String id )
	{
		if ( metaData.getString( "username" ).equals( id ) )
			return true;
		
		if ( metaData.getString( "acctId" ).equals( id ) )
			return true;
		
		return false;
	}
	
	@Override
	public String getPassword()
	{
		return getString( "password" );
	}
	
	@Override
	public String getDisplayName()
	{
		return getString( "username" );
	}
	
	@Override
	public String getUsername()
	{
		return getString( "username" );
	}
	
	@Override
	public boolean isValid()
	{
		return metaData.hasMinimumData();
	}
	
	@Override
	public MemoryAdapter getLookupAdapter()
	{
		return lookupAdapter;
	}
}
