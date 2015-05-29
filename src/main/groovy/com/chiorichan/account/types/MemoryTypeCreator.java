/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.types;

import java.util.Arrays;
import java.util.List;

import com.chiorichan.account.AccountContext;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.event.AccountLoadEvent;
import com.chiorichan.account.event.AccountLookupEvent;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventHandler;
import com.chiorichan.tasks.Timings;

/**
 * Handles Memory Accounts, e.g., Root and None
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class MemoryTypeCreator extends AccountTypeCreator
{
	public static final MemoryTypeCreator INSTANCE = new MemoryTypeCreator();
	
	MemoryTypeCreator()
	{
		super();
	}
	
	public AccountType getType()
	{
		return AccountType.MEMORY;
	}
	
	@EventHandler
	public void onAccountLookupEvent( AccountLookupEvent event )
	{
		// Do Nothing
	}
	
	@EventHandler( )
	public void onAccountLoadEvent( AccountLoadEvent event )
	{
		// Do Nothing
	}
	
	@Override
	public void save( AccountContext context )
	{
		// Do Nothing!
	}
	
	@Override
	public void reload( AccountMeta account )
	{
		// Do Nothing
	}
	
	@Override
	public boolean isEnabled()
	{
		return true; // Always
	}
	
	@Override
	public void failedLogin( AccountMeta meta, AccountResult result )
	{
		// Do Nothing
	}
	
	@Override
	public void successLogin( AccountMeta meta )
	{
		// Do Nothing
	}
	
	@Override
	public void preLogin( AccountMeta meta, AccountPermissible via, String acctId, Object... creds )
	{
		// Called before the NONE Account logs in
	}
	
	@Override
	public String getDisplayName( AccountMeta meta )
	{
		return null;
	}
	
	@Override
	public List<String> getLoginKeys()
	{
		return Arrays.asList( new String[] {} );
	}
	
	@Override
	public boolean exists( String acctId )
	{
		return "none".equals( acctId ) || "root".equals( acctId );
	}
	
	@Override
	public AccountContext createAccount( String acctId, String siteId )
	{
		AccountContext context = new AccountContextImpl( this, AccountType.SQL, acctId, siteId );
		
		context.setValue( "date", Timings.epoch() );
		
		return context;
	}
}
