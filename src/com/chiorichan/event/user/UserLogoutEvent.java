package com.chiorichan.event.user;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Called when a User leaves a server
 */
public class UserLogoutEvent extends UserEvent
{
	private static final HandlerList handlers = new HandlerList();
	private String quitMessage;
	
	public UserLogoutEvent(final User who, final String quitMessage)
	{
		super( who );
		this.quitMessage = quitMessage;
	}
	
	/**
	 * Gets the quit message to send to all online Users
	 * 
	 * @return string quit message
	 */
	public String getQuitMessage()
	{
		return quitMessage;
	}
	
	/**
	 * Sets the quit message to send to all online Users
	 * 
	 * @param quitMessage
	 *           quit message
	 */
	public void setQuitMessage( String quitMessage )
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
