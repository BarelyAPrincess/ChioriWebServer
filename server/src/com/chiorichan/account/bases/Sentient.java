package com.chiorichan.account.bases;

import com.chiorichan.permissions.Permissible;

public abstract class Sentient extends Permissible
{
	public void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			sendMessage( msg );
	}
	
	public abstract void sendMessage( String msg);

	public abstract String getName();
}
