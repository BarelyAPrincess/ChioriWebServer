/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Collection;

import com.google.common.collect.Lists;

/**
 * Used to cast a list of objects from an unknown type to a single target type
 */
public class CollectionCaster<V>
{
	Class<V> vClz;
	
	public CollectionCaster( Class<V> vClz )
	{
		this.vClz = vClz;
	}
	
	public Collection<V> castTypes( Collection<?> col )
	{
		Collection<V> newCol = Lists.newLinkedList();
		
		for ( Object e : col )
		{
			V v = ObjectFunc.castThis( vClz, e );
			
			if ( v != null )
				newCol.add( v );
		}
		
		return newCol;
	}
}
