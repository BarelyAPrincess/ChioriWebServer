package com.chiorichan.event.user;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Called when a User joins a server
 */
public class UserJoinEvent extends UserEvent
{
	private static final HandlerList handlers = new HandlerList();
	private String joinMessage;
	
	public UserJoinEvent(final User UserJoined, final String joinMessage)
	{
		super( UserJoined );
		this.joinMessage = joinMessage;
	}
	
	/**
	 * Gets the join message to send to all online Users
	 * 
	 * @return string join message
	 */
	public String getJoinMessage()
	{
		return joinMessage;
	}
	
	/**
	 * Sets the join message to send to all online Users
	 * 
	 * @param joinMessage
	 *           join message
	 */
	public void setJoinMessage( String joinMessage )
	{
		this.joinMessage = joinMessage;
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
