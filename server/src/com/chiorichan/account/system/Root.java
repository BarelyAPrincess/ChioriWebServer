package com.chiorichan.account.system;

import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.LoginException;

public class Root extends Account
{
	public Root() throws LoginException
	{
		super( "root", AccountLookupAdapter.MEMORY_ADAPTER );
	}
}
