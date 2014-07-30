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
