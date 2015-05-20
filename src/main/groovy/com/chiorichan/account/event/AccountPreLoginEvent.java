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
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.Conditional;
import com.chiorichan.event.EventException;
import com.chiorichan.event.HandlerList;
import com.chiorichan.event.RegisteredListener;

/**
 * Stores details for Users attempting to log in
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class AccountPreLoginEvent extends AccountEvent implements Conditional, Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private AccountResult result = AccountResult.DEFAULT;
	private final AccountPermissible via;
	private final Object[] creds;
	
	public AccountPreLoginEvent( AccountMeta meta, AccountPermissible via, String acctId, Object[] creds )
	{
		super( meta, via );
		this.via = via;
		this.creds = creds;
	}
	
	public AccountPermissible getPermissible()
	{
		return via;
	}
	
	public Object[] getCredentials()
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
		// If the result returned is an error then we skip the remaining EventListeners
		return !result.isError();
	}
	
	@Override
	public boolean isCancelled()
	{
		return result == AccountResult.CANCELLED_BY_EVENT;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		result = AccountResult.CANCELLED_BY_EVENT;
	}
}
