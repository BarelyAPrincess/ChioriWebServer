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
import com.chiorichan.account.LoginException;
import com.chiorichan.account.adapter.memory.MemoryAccount;

public class SystemAccounts
{
	public static MemoryAccount NO_LOGIN;
	public static MemoryAccount ROOT;
	
	static
	{
		try
		{
			NO_LOGIN = new NoLogin();
			Loader.getAccountManager().LoadAccount( NO_LOGIN, true, true, false );
			
			ROOT = new Root();
			Loader.getAccountManager().LoadAccount( ROOT, true, true, true );
		}
		catch ( LoginException e )
		{
			e.printStackTrace();
		}
	}
}
