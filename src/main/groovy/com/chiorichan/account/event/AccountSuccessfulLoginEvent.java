/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.event;

import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.lang.AccountResult;

/**
 * Fired when the Account was successfully logged in
 */
public class AccountSuccessfulLoginEvent extends AccountFailedLoginEvent
{
	AccountPermissible via;
	
	public AccountSuccessfulLoginEvent( AccountMeta meta, AccountPermissible via, AccountResult result )
	{
		super( meta, via, result );
		this.via = via;
	}
	
	public AccountPermissible getAccountPermissible()
	{
		return via;
	}
}
