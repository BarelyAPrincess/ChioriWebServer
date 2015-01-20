package com.chiorichan.account;

import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.PermissibleInteractive;


public abstract class AccountHandler extends PermissibleInteractive
{
	protected Permissible permissible = null;
	
	public void attachPermissible( Permissible permissibleAttachment )
	{
		permissible = permissibleAttachment;
	}

	public Permissible getPermissible()
	{
		return permissible;
	}

	public void removePermissible()
	{
		permissible = null;
	}
}
