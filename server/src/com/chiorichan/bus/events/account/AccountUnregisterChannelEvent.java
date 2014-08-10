package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;


/**
 * This is called immediately after a User unregisters for a plugin channel.
 */
public class AccountUnregisterChannelEvent extends AccountChannelEvent
{
	
	public AccountUnregisterChannelEvent(final Account User, final String channel)
	{
		super( User, channel );
	}
}
