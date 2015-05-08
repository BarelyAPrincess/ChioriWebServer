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
import com.chiorichan.event.HandlerList;

/**
 * Called when a User leaves a server
 */
public class AccountLogoutEvent extends AccountEvent
{
	private static final HandlerList handlers = new HandlerList();
	private String quitMessage;
	
	public AccountLogoutEvent( final AccountMeta acct, final String quitMessage )
	{
		super( acct );
		this.quitMessage = quitMessage;
	}
	
	/**
	 * Gets the quit message to send to all online Users
	 * 
	 * @return string quit message
	 */
	public String getLeaveMessage()
	{
		return quitMessage;
	}
	
	/**
	 * Sets the quit message to send to all online Users
	 * 
	 * @param quitMessage
	 *            quit message
	 */
	public void setLeaveMessage( String quitMessage )
	{
		this.quitMessage = quitMessage;
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
