/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Provides basic methods for database convenience
 */
public class DbFunc
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
	
	public static Map<String, Map<String, Object>> resultToMap( ResultSet rs ) throws SQLException
	{
		LinkedHashMap<String, Map<String, Object>> result = Maps.newLinkedHashMap();
		int x = 0;
		
		if ( rs == null )
			return result;
		
		boolean next = rs.isFirst() || rs.next();
		while ( next )
		{
			result.put( Integer.toString( x ), rowToMap( rs ) );
			x++;
			next = rs.next();
		}
		
		return result;
	}
	
	public static Set<Map<String, Object>> resultToSet( ResultSet rs ) throws SQLException
	{
		LinkedHashSet<Map<String, Object>> result = Sets.newLinkedHashSet();
		
		if ( rs == null )
			return result;
		
		boolean next = rs.isFirst() || rs.next();
		while ( next )
		{
			result.add( rowToMap( rs ) );
			next = rs.next();
		}
		
		return result;
	}
	
	public static Map<String, Map<String, String>> resultToStringMap( ResultSet rs ) throws SQLException
	{
		LinkedHashMap<String, Map<String, String>> result = Maps.newLinkedHashMap();
		int x = 0;
		
		if ( rs == null )
			return result;
		
		boolean next = rs.isFirst() || rs.next();
		while ( next )
		{
			result.put( "" + x, new MapCaster<String, String>( String.class, String.class ).castTypes( rowToMap( rs ) ) );
			x++;
			next = rs.next();
		}
		
		return result;
	}
	
	public static Set<Map<String, String>> resultToStringSet( ResultSet rs ) throws SQLException
	{
		LinkedHashSet<Map<String, String>> result = Sets.newLinkedHashSet();
		
		if ( rs == null )
			return result;
		
		boolean next = rs.isFirst() || rs.next();
		while ( next )
		{
			result.add( rowToStringMap( rs ) );
			next = rs.next();
		}
		
		return result;
	}
	
	@Deprecated
	public static int rowCount( ResultSet result )
	{
		try
		{
			if ( result == null )
				return -1;
			
			if ( result.getType() == ResultSet.TYPE_FORWARD_ONLY )
				throw new IllegalStateException( "The ResultSet type is TYPE_FORWARD_ONLY, using the standard method to test for row count does not work on this type." );
			
			int row = result.getRow();
			result.last();
			int lastRow = result.getRow();
			result.absolute( row );
			return lastRow;
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	public static Map<String, Object> rowToMap( ResultSet rs ) throws SQLException
	{
		Map<String, Object> result = Maps.newLinkedHashMap();
		
		if ( rs == null )
			return null;
		
		if ( rs.isBeforeFirst() && !rs.next() )
			return null;
		
		ResultSetMetaData rsmd = rs.getMetaData();
		
		int numColumns = rsmd.getColumnCount();
		
		for ( int i = 1; i < numColumns + 1; i++ )
		{
			String columnName = rsmd.getColumnName( i );
			
			// DatastoreManager.getLogger().info( "Column: " + columnName + " <-> " + rsmd.getColumnTypeName( i ) );
			
			if ( rsmd.getColumnType( i ) == Types.ARRAY )
				result.put( columnName, rs.getArray( columnName ).getArray() );
			else if ( rsmd.getColumnType( i ) == Types.BIGINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.TINYINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.BIT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == java.sql.Types.BOOLEAN )
				result.put( columnName, rs.getBoolean( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).contains( "BLOB" ) || rsmd.getColumnType( i ) == Types.BINARY )
			{
				// BLOG = Max Length 65,535. Recommended that you use a LONGBLOG.
				byte[] bytes = rs.getBytes( columnName );
				result.put( columnName, bytes );
				/*
				 * try
				 * {
				 * result.put( columnName, new String( bytes, "ISO-8859-1" ) );
				 * }
				 * catch ( UnsupportedEncodingException e )
				 * {
				 * e.printStackTrace();
				 * }
				 */
			}
			else if ( rsmd.getColumnType( i ) == Types.DOUBLE )
				result.put( columnName, rs.getDouble( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.FLOAT )
				result.put( columnName, rs.getFloat( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).equals( "INT" ) )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.NVARCHAR )
				result.put( columnName, rs.getNString( columnName ) );
			else if ( rsmd.getColumnTypeName( i ).equals( "VARCHAR" ) )
				result.put( columnName, rs.getString( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.SMALLINT )
				result.put( columnName, rs.getInt( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.DATE )
				result.put( columnName, rs.getDate( columnName ) );
			else if ( rsmd.getColumnType( i ) == Types.TIMESTAMP )
				result.put( columnName, rs.getTimestamp( columnName ) );
			else
				result.put( columnName, rs.getObject( columnName ) );
		}
		
		return result;
	}
	
	public static Map<String, String> rowToStringMap( ResultSet rs ) throws SQLException
	{
		return new MapCaster<String, String>( String.class, String.class ).castTypes( rowToMap( rs ) );
	}
	
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
		
		return result;
	}
}
