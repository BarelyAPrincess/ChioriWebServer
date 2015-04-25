/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Provides basic methods for database convenience
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class DbFunc
{
	public static Map<Object, Map<String, Object>> sortByColumnValue( Map<String, Map<String, Object>> orig, String key )
	{
		return sortByColumnValue( orig, key, String.class );
	}
	
	public static Map<Object, Map<String, Object>> sortByColumnValue( Map<String, Map<String, Object>> orig, String key, Class<?> keyType )
	{
		return sortByColumnValue( orig, key, keyType, SortStrategy.Default );
	}
	
	public static Map<Object, Map<String, Object>> sortByColumnValue( Map<String, Map<String, Object>> orig, String key, Class<?> keyType, SortStrategy strategy )
	{
		Map<Object, Map<String, Object>> result = new TreeMap<Object, Map<String, Object>>();
		
		if ( ( strategy == SortStrategy.MoveNext || strategy == SortStrategy.MovePrevious ) && keyType != Integer.class )
			throw new IllegalArgumentException( "Sorting Strategy `" + strategy + "` can only be used with key type `Integer`, `" + keyType.getName() + "` was specified." );
		
		if ( orig.size() < 1 )
			return result;
		
		for ( Entry<String, Map<String, Object>> e : orig.entrySet() )
		{
			Map<String, Object> row = e.getValue();
			if ( row.containsKey( key ) )
			{
				if ( strategy == SortStrategy.Default )
					result.put( ObjectFunc.castThis( keyType, row.get( key ) ), row );
				else if ( strategy == SortStrategy.MoveNext )
				{
					int v = ObjectFunc.castThis( keyType, row.get( key ) );
					
					if ( result.containsKey( v ) )
						moveNext( result, v );
					
					result.put( v, row );
				}
				else if ( strategy == SortStrategy.MovePrevious )
				{
					int v = ObjectFunc.castThis( keyType, row.get( key ) );
					
					if ( result.containsKey( v ) )
						movePrevious( result, v );
					
					result.put( v, row );
				}
			}
		}
		
		return result;
	}
	
	private static void movePrevious( Map<Object, Map<String, Object>> result, int key )
	{
		if ( !result.containsKey( key ) )
			return;
		
		Map<String, Object> row = result.remove( key );
		int nextKey = key - 1;
		if ( result.containsKey( nextKey ) )
			movePrevious( result, nextKey );
		result.put( nextKey, row );
	}
	
	private static void moveNext( Map<Object, Map<String, Object>> result, int key )
	{
		if ( !result.containsKey( key ) )
			return;
		
		Map<String, Object> row = result.remove( key );
		int nextKey = key + 1;
		if ( result.containsKey( nextKey ) )
			moveNext( result, nextKey );
		result.put( nextKey, row );
	}
}
