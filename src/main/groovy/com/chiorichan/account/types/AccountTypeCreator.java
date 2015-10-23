/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.types;

import com.chiorichan.account.AccountCreator;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.event.Listener;

/**
 * Used as Account Type Creator
 */
public abstract class AccountTypeCreator implements Listener, AccountCreator
{
	@Override
	public void save( AccountMeta meta ) throws AccountException
	{
		save( meta.context() );
	}
}
