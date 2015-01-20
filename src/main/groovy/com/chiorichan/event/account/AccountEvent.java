/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.account;

import com.chiorichan.account.Account;
import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;

/**
 * Represents a User related event
 */
public abstract class AccountEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	protected Account acct;
	
	public AccountEvent(final Account who)
	{
		acct = who;
	}
	
	AccountEvent(final Account who, boolean async)
	{
		super( async );
		acct = who;
		
	}
	
	/**
	 * Returns the User involved in this event
	 * 
	 * @return User who is involved in this event
	 */
	public final Account getAccount()
	{
		return acct;
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
}
