/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.lang;

/**
 * Used when the Account Instance has not been initialized
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class AccountNotInitialized extends IllegalStateException
{
	private static final long serialVersionUID = 6660944940197619678L;
	
	public AccountNotInitialized()
	{
		super( "Account has not been initialized" );
	}
}
