/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
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
@SuppressWarnings( "unchecked" )
public class MapFunc<K, V>
{
	public Map<K, V> castTypes( Map<?, ?> map )
	{
		K tK = null;
		V tV = null;
		Map<K, V> newMap = Maps.newHashMap();
		
		for ( Entry<?, ?> e : map.entrySet() )
		{
			try
			{
				K k = ( K ) ( ( tK instanceof String ) ? ObjectFunc.castToStringWithException( e.getKey() ) : e.getKey() );
				V v = ( V ) ( ( tV instanceof String ) ? ObjectFunc.castToStringWithException( e.getValue() ) : e.getValue() );
				
				newMap.put( k, v );
			}
			catch ( ClassCastException cce )
			{
				// Obviously key or value were not compatible with specified generic types
			}
		}
		
		return newMap;
	}
}
