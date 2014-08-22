package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;
import com.chiorichan.bus.events.HandlerList;

/**
 * Stores details for Users attempting to log in
 */
public class PreAccountLoginEvent extends AccountEvent
{
	private static final HandlerList handlers = new HandlerList();
	private Result result = Result.ALLOWED;
	private String message = "";
	
	public PreAccountLoginEvent(Account user)
	{
		super( user );
	}
	
	/**
	 * Gets the current result of the login, as an enum
	 * 
	 * @return Current Result of the login
	 */
	public Result getResult()
	{
		return result;
	}
	
	/**
	 * Sets the new result of the login, as an enum
	 * 
	 * @param result
	 *           New result to set
	 */
	public void setResult( final Result result )
	{
		this.result = result;
	}
	
	/**
	 * Gets the current kick message that will be used if getResult() != Result.ALLOWED
	 * 
	 * @return Current kick message
	 */
	public String getKickMessage()
	{
		return message;
	}
	
	/**
	 * Sets the kick message to display if getResult() != Result.ALLOWED
	 * 
	 * @param message
	 *           New kick message
	 */
	public void setKickMessage( final String message )
	{
		this.message = message;
	}
	
	/**
	 * Allows the User to log in
	 */
	public void allow()
	{
		result = Result.ALLOWED;
		message = "";
	}
	
	/**
	 * Disallows the User from logging in, with the given reason
	 * 
	 * @param result
	 *           New result for disallowing the User
	 * @param message
	 *           Kick message to display to the user
	 */
	public void disallow( final Result result, final String message )
	{
		this.result = result;
		this.message = message;
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
	
	/**
	 * Basic kick reasons for communicating to plugins
	 */
	public enum Result
	{
		/**
		 * The User is allowed to log in
		 */
		ALLOWED,
		/**
		 * The User is not allowed to log in, due to the server being full
		 */
		KICK_FULL,
		/**
		 * The User is not allowed to log in, due to them being banned
		 */
		KICK_BANNED,
		/**
		 * The User is not allowed to log in, due to them not being on the white list
		 */
		KICK_WHITELIST,
		/**
		 * The User is not allowed to log in, for reasons undefined
		 */
		KICK_OTHER,
		/**
		 * The User had incorrect incorrect login
		 */
		DENIED
	}
}
