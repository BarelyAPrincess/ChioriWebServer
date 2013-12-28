package com.chiorichan.event.user;

import com.chiorichan.user.User;


/**
 * This is called immediately after a User registers for a plugin channel.
 */
public class UserRegisterChannelEvent extends UserChannelEvent
{
	
	public UserRegisterChannelEvent(final User User, final String channel)
	{
		super( User, channel );
	}
}
