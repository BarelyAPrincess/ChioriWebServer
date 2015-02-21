/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import com.chiorichan.permission.Permissible;

public abstract class AccountHandler extends Permissible implements InteractiveEntity
{
	Account<?> currentAccount = null;
	
	public void attachAccount( Account<?> acct )
	{
		this.currentAccount = acct;
	}
	
	public Account<?> getAccount()
	{
		return currentAccount;
	}
	
	public void reset()
	{
		currentAccount = null;
	}
	
	@Override
	public boolean isValid()
	{
		return currentAccount != null;
	}
	
	@Override
	public String getId()
	{
		return currentAccount.getAcctId();
	}
}
