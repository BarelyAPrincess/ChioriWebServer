/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.tasks;


class AsyncTaskDebugger
{
	private AsyncTaskDebugger next = null;
	private final int expiry;
	private final TaskRegistrar creator;
	private final Class<? extends Runnable> clazz;
	
	AsyncTaskDebugger( final int expiry, final TaskRegistrar creator, final Class<? extends Runnable> clazz )
	{
		this.expiry = expiry;
		this.creator = creator;
		this.clazz = clazz;
		
	}
	
	final AsyncTaskDebugger getNextHead( final int time )
	{
		AsyncTaskDebugger next, current = this;
		while ( time > current.expiry && ( next = current.next ) != null )
		{
			current = next;
		}
		return current;
	}
	
	final AsyncTaskDebugger setNext( final AsyncTaskDebugger next )
	{
		return this.next = next;
	}
	
	StringBuilder debugTo( final StringBuilder string )
	{
		for ( AsyncTaskDebugger next = this; next != null; next = next.next )
		{
			string.append( next.creator.getName() ).append( ':' ).append( next.clazz.getName() ).append( '@' ).append( next.expiry ).append( ',' );
		}
		return string;
	}
}
