package com.chiorichan.permission;

public abstract class PermissibleInteractive extends PermissibleBase
{	
	public abstract void sendMessage( String string );
	
	public abstract boolean kick( String kickMessage );
	
	public abstract boolean isValid();
	
	public void sendMessage( String... msgs )
	{
		for ( String m : msgs )
			sendMessage( m );
	}
}
