package com.chiorichan.account.system;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.LoginException;

public class SystemAccounts
{
	public static Account NO_LOGIN;
	public static Account ROOT;
	
	public SystemAccounts()
	{
		try
		{
			NO_LOGIN = new NoLogin();
			Loader.getAccountsBus().LoadUser( NO_LOGIN, true, true, false );
			
			ROOT = new Root();
			Loader.getAccountsBus().LoadUser( ROOT, true, true, false );
		}
		catch ( LoginException e )
		{
			e.printStackTrace();
		}
	}
}
