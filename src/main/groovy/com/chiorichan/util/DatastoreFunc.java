/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 */
public class DatastoreFunc
{
	public static String escape( String str )
	{
		if ( str == null )
			return null;
		
		if ( str.replaceAll( "[a-zA-Z0-9_!@#$%^&*()-=+~.;:,\\Q[\\E\\Q]\\E<>{}\\/? ]", "" ).length() < 1 )
			return str;
		
		String cleanString = str;
		cleanString = cleanString.replaceAll( "\\\\", "\\\\\\\\" );
		cleanString = cleanString.replaceAll( "\\n", "\\\\n" );
		cleanString = cleanString.replaceAll( "\\r", "\\\\r" );
		cleanString = cleanString.replaceAll( "\\t", "\\\\t" );
		cleanString = cleanString.replaceAll( "\\00", "\\\\0" );
		cleanString = cleanString.replaceAll( "'", "\\\\'" );
		cleanString = cleanString.replaceAll( "\\\"", "\\\\\"" );
		
		return cleanString;
	}
	
	public static List<String> wrap( List<String> list )
	{
		return wrap( list, '`' );
	}
	
	public static List<String> wrap( List<String> list, char wrap )
	{
		List<String> newList = Lists.newArrayList();
		
		for ( String e : list )
			if ( e != null && !e.isEmpty() )
				newList.add( wrap + e + wrap );
		
		return newList;
	}
	
	public static Map<String, String> wrap( Map<String, String> map )
	{
		return wrap( map, '`', '\'' );
	}
	
	public static Map<String, String> wrap( Map<String, String> map, char keyWrap, char valueWrap )
	{
		Map<String, String> newMap = Maps.newHashMap();
		
		for ( Entry<String, String> e : map.entrySet() )
			if ( e.getKey() != null && !e.getKey().isEmpty() )
				newMap.put( keyWrap + e.getKey() + keyWrap, valueWrap + ( e.getValue() == null ? "" : e.getValue() ) + valueWrap );
		
		return newMap;
	}
	
	public static String wrap( String str, char wrap )
	{
		return String.format( "%s%s%s", wrap, str, wrap );
	}
}
