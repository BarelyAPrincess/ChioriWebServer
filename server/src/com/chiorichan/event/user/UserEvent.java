package com.chiorichan.event.user;

import com.chiorichan.event.Event;
import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

/**
 * Represents a User related event
 */
public abstract class UserEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	protected User User;
	
	public UserEvent(final User who)
	{
		User = who;
	}
	
	UserEvent(final User who, boolean async)
	{
		super( async );
		User = who;
		
	}
	
	/**
	 * Returns the User involved in this event
	 * 
	 * @return User who is involved in this event
	 */
	public final User getUser()
	{
		return User;
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
