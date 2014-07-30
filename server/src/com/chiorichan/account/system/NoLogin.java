package com.chiorichan.account.system;

import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.LoginException;

public class NoLogin extends Account
{
	public NoLogin() throws LoginException
	{
		super( "noLogin", AccountLookupAdapter.MEMORY_ADAPTER );
	}
}