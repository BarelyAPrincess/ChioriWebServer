/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.scheduler;


class ChioriAsyncDebugger
{
	private ChioriAsyncDebugger next = null;
	private final int expiry;
	private final TaskCreator creator;
	private final Class<? extends Runnable> clazz;
	
	ChioriAsyncDebugger(final int expiry, final TaskCreator creator, final Class<? extends Runnable> clazz)
	{
		this.expiry = expiry;
		this.creator = creator;
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
			string.append( next.creator.getName() ).append( ':' ).append( next.clazz.getName() ).append( '@' ).append( next.expiry ).append( ',' );
		}
		return string;
	}
}
