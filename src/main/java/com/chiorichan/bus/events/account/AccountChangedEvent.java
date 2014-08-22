package com.chiorichan.bus.events.account;

import com.chiorichan.account.bases.Account;

public class AccountChangedEvent extends AccountEvent
{
	public AccountChangedEvent(Account who)
	{
		super( who );
	}
}
