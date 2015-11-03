/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import com.chiorichan.account.lang.AccountResult;

/**
 * Indicates kickable account logins
 */
public interface Kickable
{
	/**
	 * Attempts to kick Account from server
	 * 
	 * @param reason
	 *            The reason for kick
	 * @return Result of said kick attempt
	 */
	AccountResult kick( String reason );
	
	String getId();
}
