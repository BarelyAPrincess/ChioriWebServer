package com.chiorichan.event.user;

import com.chiorichan.event.Event;
import com.chiorichan.user.User;

/**
 * Represents a User related event
 */
public abstract class UserEvent extends Event
{
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
}
