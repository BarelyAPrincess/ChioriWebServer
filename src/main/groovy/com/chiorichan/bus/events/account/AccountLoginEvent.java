/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.HandlerList;

/**
 * Stores details for Users attempting to log in
 */
public class AccountLoginEvent extends AccountEvent
{
	private static final HandlerList handlers = new HandlerList();
	private String message;
	
	public AccountLoginEvent(Account user, final String _message)
	{
		super( user );
		message = _message;
	}
	
	/**
	 * Gets the join message to send to all online Accounts
	 * 
	 * @return string join message
	 */
	public String getLoginMessage()
	{
		return message;
	}
	
	/**
	 * Sets the join message to send to all online Accounts
	 * 
	 * @param joinMessage
	 *           join message
	 */
	public void setJoinMessage( String _message )
	{
		message = _message;
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
