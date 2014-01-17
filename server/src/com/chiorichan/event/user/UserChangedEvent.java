package com.chiorichan.event.user;

import com.chiorichan.event.HandlerList;
import com.chiorichan.user.User;

public class UserChangedEvent extends UserEvent
{
	public UserChangedEvent(User who)
	{
		super( who );
	}
	
	private static final HandlerList handlers = new HandlerList();
}
