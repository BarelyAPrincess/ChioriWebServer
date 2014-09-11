/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
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
			Loader.getAccountsManager().LoadAccount( NO_LOGIN, true, true, false );
			
			ROOT = new Root();
			Loader.getAccountsManager().LoadAccount( ROOT, true, true, false );
		}
		catch ( LoginException e )
		{
			e.printStackTrace();
		}
	}
}
