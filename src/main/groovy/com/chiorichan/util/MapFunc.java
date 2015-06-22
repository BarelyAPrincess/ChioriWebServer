/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

/**
 * Provides some basic Map utils to the server
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class MapFunc<K, V>
{
	Class<K> kClz;
	Class<V> vClz;
	
	public MapFunc( Class<K> kClz, Class<V> vClz )
	{
		this.kClz = kClz;
		this.vClz = vClz;
	}
	
	public Map<K, V> castTypes( Map<?, ?> map )
	{
		Map<K, V> newMap = Maps.newHashMap();
		
		for ( Entry<?, ?> e : map.entrySet() )
		{
			K k = ObjectFunc.castThis( kClz, e.getKey() );
			V v = ObjectFunc.castThis( vClz, e.getValue() );
			
			if ( k != null && v != null )
				newMap.put( k, v );
		}
		
		return newMap;
	}
}
