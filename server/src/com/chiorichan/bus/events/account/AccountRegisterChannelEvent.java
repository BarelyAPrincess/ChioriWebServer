package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;


/**
 * This is called immediately after a User registers for a plugin channel.
 */
public class AccountRegisterChannelEvent extends AccountChannelEvent
{
	
	public AccountRegisterChannelEvent(final Account User, final String channel)
	{
		super( User, channel );
	}
}
