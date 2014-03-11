package com.chiorichan.event.user;

import com.chiorichan.user.User;

public class UserChangedEvent extends UserEvent
{
	public UserChangedEvent(User who)
	{
		super( who );
	}
}
