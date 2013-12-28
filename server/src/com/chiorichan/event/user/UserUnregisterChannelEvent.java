package com.chiorichan.event.user;

import com.chiorichan.user.User;


/**
 * This is called immediately after a User unregisters for a plugin channel.
 */
public class UserUnregisterChannelEvent extends UserChannelEvent
{
	
	public UserUnregisterChannelEvent(final User User, final String channel)
	{
		super( User, channel );
	}
}
