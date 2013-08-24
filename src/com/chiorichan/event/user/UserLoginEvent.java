package com.chiorichan.event.user;

import java.net.InetAddress;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Stores details for Users attempting to log in
 */
public class UserLoginEvent extends UserEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final InetAddress address;
	private final String hostname;
	private Result result = Result.ALLOWED;
	private String message = "";
	
	/**
	 * @deprecated Address should be provided in other constructor
	 */
	@Deprecated
	public UserLoginEvent(final User User)
	{
		this( User, "", null );
	}
	
	/**
	 * @deprecated Address should be provided in other constructor
	 */
	@Deprecated
	public UserLoginEvent(final User User, final String hostname)
	{
		this( User, hostname, null );
	}
	
	/**
	 * This constructor defaults message to an empty string, and result to ALLOWED
	 * 
	 * @param User
	 *           The {@link User} for this event
	 * @param hostname
	 *           The hostname that was used to connect to the server
	 * @param address
	 *           The address the User used to connect, provided for timing issues
	 */
	public UserLoginEvent(final User User, final String hostname, final InetAddress address)
	{
		super( User );
		this.hostname = hostname;
		this.address = address;
	}
	
	/**
	 * @deprecated Address and hostname should be provided in other constructor
	 */
	@Deprecated
	public UserLoginEvent(final User User, final Result result, final String message)
	{
		this( User, "", null, result, message );
	}
	
	/**
	 * This constructor pre-configures the event with a result and message
	 * 
	 * @param User
	 *           The {@link User} for this event
	 * @param hostname
	 *           The hostname that was used to connect to the server
	 * @param address
	 *           The address the User used to connect, provided for timing issues
	 * @param result
	 *           The result status for this event
	 * @param message
	 *           The message to be displayed if result denies login
	 */
	public UserLoginEvent(final User User, String hostname, final InetAddress address, final Result result, final String message)
	{
		this( User, hostname, address );
		this.result = result;
		this.message = message;
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
	 * Gets the hostname that the User used to connect to the server, or blank if unknown
	 * 
	 * @return The hostname
	 */
	public String getHostname()
	{
		return hostname;
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
	
	/**
	 * Gets the {@link InetAddress} for the User associated with this event. This method is provided as a workaround
	 * for User.getAddress() returning null during UserLoginEvent.
	 * 
	 * @return The address for this User. For legacy compatibility, this may be null.
	 */
	public InetAddress getAddress()
	{
		return address;
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
		KICK_OTHER
	}
}
