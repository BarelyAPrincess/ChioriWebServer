package com.chiorichan.scheduler;

import com.chiorichan.plugin.Plugin;

class ChioriAsyncDebugger
{
	private ChioriAsyncDebugger next = null;
	private final int expiry;
	private final Plugin plugin;
	private final Class<? extends Runnable> clazz;
	
	ChioriAsyncDebugger(final int expiry, final Plugin plugin, final Class<? extends Runnable> clazz)
	{
		this.expiry = expiry;
		this.plugin = plugin;
		this.clazz = clazz;
		
	}
	
	final ChioriAsyncDebugger getNextHead( final int time )
	{
		ChioriAsyncDebugger next, current = this;
		while ( time > current.expiry && ( next = current.next ) != null )
		{
			current = next;
		}
		return current;
	}
	
	final ChioriAsyncDebugger setNext( final ChioriAsyncDebugger next )
	{
		return this.next = next;
	}
	
	StringBuilder debugTo( final StringBuilder string )
	{
		for ( ChioriAsyncDebugger next = this; next != null; next = next.next )
		{
			string.append( next.plugin.getDescription().getName() ).append( ':' ).append( next.clazz.getName() ).append( '@' ).append( next.expiry ).append( ',' );
		}
		return string;
	}
}
