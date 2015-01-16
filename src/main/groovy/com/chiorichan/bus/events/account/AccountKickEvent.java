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
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.HandlerList;

/**
 * Called when a User gets kicked from the server
 */
public class AccountKickEvent extends AccountEvent implements Cancellable
{
	private static final HandlerList handlers = new HandlerList();
	private String leaveMessage;
	private String kickReason;
	private Boolean cancel;
	
	public AccountKickEvent(final Account UserKicked, final String kickReason, final String leaveMessage)
	{
		super( UserKicked );
		this.kickReason = kickReason;
		this.leaveMessage = leaveMessage;
		this.cancel = false;
	}
	
	/**
	 * Gets the reason why the User is getting kicked
	 * 
	 * @return string kick reason
	 */
	public String getReason()
	{
		return kickReason;
	}
	
	/**
	 * Gets the leave message send to all online Users
	 * 
	 * @return string kick reason
	 */
	public String getLeaveMessage()
	{
		return leaveMessage;
	}
	
	public boolean isCancelled()
	{
		return cancel;
	}
	
	public void setCancelled( boolean cancel )
	{
		this.cancel = cancel;
	}
	
	/**
	 * Sets the reason why the User is getting kicked
	 * 
	 * @param kickReason
	 *           kick reason
	 */
	public void setReason( String kickReason )
	{
		this.kickReason = kickReason;
	}
	
	/**
	 * Sets the leave message send to all online Users
	 * 
	 * @param leaveMessage
	 *           leave message
	 */
	public void setLeaveMessage( String leaveMessage )
	{
		this.leaveMessage = leaveMessage;
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
