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
import com.chiorichan.account.lang.AccountResult;

/**
 * Fired when the Account was successfully logged in
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class AccountSuccessfulLoginEvent extends AccountFailedLoginEvent
{
	public AccountSuccessfulLoginEvent( AccountMeta meta )
	{
		super( meta, AccountResult.LOGIN_SUCCESS );
	}
}
