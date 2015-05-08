/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.types;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.event.AccountLoadEvent;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventHandler;

/**
 * Handles Memory Accounts, e.g., Root and None
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
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
	
	@EventHandler( )
	public void onAccountLoadEvent( AccountLoadEvent event )
	{
		
	}
	
	@Override
	public void save( AccountMeta account )
	{
		// DO NOTHING!
	}
	
	@Override
	public void reload( AccountMeta account )
	{
		// DO NOTHING!
	}
	
	@Override
	public boolean isEnabled()
	{
		return true; // Always
	}
	
	@Override
	public void failedLogin( AccountMeta meta, AccountResult result )
	{
		// Never?
	}
	
	@Override
	public void successLogin( AccountMeta meta, AccountResult result )
	{
		// Never?
	}
	
	@Override
	public void preLogin( AccountMeta meta, AccountPermissible via, AccountCredentials creds )
	{
		// Never?
		throw new AccountException( AccountResult.FEATURE_DISABLED );
	}
	
	@Override
	public String getHumanReadableName( AccountMeta meta )
	{
		return null;
	}
	
	// return new AccountMeta( account, StringFunc.md5( "9834h9fh3497ah4ea3a" + System.currentTimeMillis() ), account );
}
