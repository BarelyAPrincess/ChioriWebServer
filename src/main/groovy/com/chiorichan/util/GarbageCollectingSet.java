/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.google.common.collect.Sets;

/**
 * Implements a self garbage collecting Set, that is Thread-safe
 */
public class GarbageCollectingSet<V, G> implements Iterable<V>
{
	static class CleanupThread extends Thread
	{
		CleanupThread()
		{
			setPriority( Thread.MAX_PRIORITY );
			setName( "GarbageCollectingSet-cleanupthread" );
			setDaemon( true );
		}
		
		@Override
		public void run()
		{
			while ( true )
				try
				{
					GarbageReference<?, ?> ref;
					while ( true )
					{
						ref = ( GarbageReference<?, ?> ) referenceQueue.remove();
						ref.list.remove( ref );
					}
				}
				catch ( InterruptedException e )
				{
					// ignore
				}
		}
	}
	
	static class GarbageReference<V, G> extends WeakReference<G>
	{
		final V value;
		final Set<GarbageReference<V, G>> list;
		
		GarbageReference( G referent, V value, Set<GarbageReference<V, G>> list )
		{
			super( referent, referenceQueue );
			this.value = value;
			this.list = list;
		}
	}
	
	private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
	
	static
	{
		new CleanupThread().start();
	}
	
	private final Set<GarbageReference<V, G>> list = Sets.newCopyOnWriteArraySet();
	
	public void add( V value, G garbageObject )
	{
		Validate.notNull( garbageObject );
		Validate.notNull( value );
		
		if ( value == garbageObject )
			throw new IllegalArgumentException( "value can't be equal to garbageObject for gc to work" );
		
		GarbageReference<V, G> reference = new GarbageReference<V, G>( garbageObject, value, list );
		list.add( reference );
	}
	
	public void addAll( Iterable<V> values, G garbageObject )
	{
		for ( V v : values )
			add( v, garbageObject );
	}
	
	public void clear()
	{
		list.clear();
	}
	
	@Override
	public Iterator<V> iterator()
	{
		return toSet().iterator();
	}
	
	public Set<V> toSet()
	{
		Set<V> values = Sets.newHashSet();
		for ( GarbageReference<V, G> v : list )
			values.add( v.value );
		return values;
	}
}
