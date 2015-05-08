/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.event;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.auth.AccountCredentials;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.Conditional;
import com.chiorichan.event.EventException;
import com.chiorichan.event.HandlerList;
import com.chiorichan.event.RegisteredListener;

/**
 * Stores details for Users attempting to log in
 */
public class AccountPreLoginEvent extends AccountEvent implements Conditional
{
	private static final HandlerList handlers = new HandlerList();
	private AccountResult result = AccountResult.LOGIN_SUCCESS;
	private final AccountPermissible perm;
	private final AccountCredentials creds;
	
	public AccountPreLoginEvent( AccountMeta meta, AccountPermissible perm, AccountCredentials creds )
	{
		super( meta, perm );
		this.perm = perm;
		this.creds = creds;
	}
	
	public AccountPermissible getPermissible()
	{
		return perm;
	}
	
	public AccountCredentials getCredentials()
	{
		return creds;
	}
	
	/**
	 * Gets the current result of the login, as an enum
	 * 
	 * @return Current AccountResult of the login
	 */
	public AccountResult getAccountResult()
	{
		return result;
	}
	
	/**
	 * Sets the new result of the login, as an enum
	 * 
	 * @param result
	 *            New result to set
	 */
	public void setAccountResult( final AccountResult result )
	{
		this.result = result;
	}
	
	/**
	 * Allows the User to log in
	 */
	public void success()
	{
		result = AccountResult.DEFAULT;
	}
	
	/**
	 * Disallows the User from logging in, with the given reason
	 * 
	 * @param result
	 *            New result for disallowing the User
	 * @param message
	 *            Kick message to display to the user
	 */
	public void fail( final AccountResult result )
	{
		this.result = result;
	}
	
	@Override
	public HandlerList getHandlers()
	{
		return handlers;
	}
	
	public static HandlerList getHandlerList()
	{
		return handlers;
	}
	
	@Override
	public boolean conditional( RegisteredListener context ) throws EventException
	{
		return result == AccountResult.DEFAULT;
	}
}
