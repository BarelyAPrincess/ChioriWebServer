/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import java.math.BigDecimal;

import com.chiorichan.Loader;

public class ObjectUtil
{
	public static Long castToLong( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return (Long) value;
			case "java.lang.String":
				return Long.parseLong( (String) value );
			case "java.lang.Integer":
				return (Long) value;
			case "java.lang.Double":
				return (Long) value;
			case "java.lang.Boolean":
				return ( (boolean) value ) ? 1L : 0L;
			case "java.math.BigDecimal":
				return ( (BigDecimal) value ).setScale( 0, BigDecimal.ROUND_HALF_UP ).longValue();
			default:
				Loader.getLogger().warning( "Uncaught Convertion to String of Type: " + value.getClass().getName() );
				return null;
		}
	}
	
	public static String castToString( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return Long.toString( (long) value );
			case "java.lang.String":
				return (String) value;
			case "java.lang.Integer":
				return Integer.toString( (int) value );
			case "java.lang.Double":
				return Double.toString( (double) value );
			case "java.lang.Boolean":
				return ( (boolean) value ) ? "true" : "false";
			case "java.math.BigDecimal":
				return ( (BigDecimal) value ).toString();
			default:
				Loader.getLogger().warning( "Uncaught Convertion to String of Type: " + value.getClass().getName() );
				return null;
		}
	}
	/*
	 * public static void ArrayValueCast( ArrayValue array, String key, Object obj )
	 * {
	 * switch ( obj.getClass().getName() )
	 * {
	 * case "java.lang.Long":
	 * array.put( key, (long) obj );
	 * break;
	 * case "java.lang.String":
	 * array.put( key, (String) obj );
	 * break;
	 * case "java.lang.Integer":
	 * array.put( key, (int) obj );
	 * break;
	 * case "java.lang.Double":
	 * array.put( key, (double) obj );
	 * break;
	 * case "java.lang.Boolean":
	 * array.put( key, (boolean) obj );
	 * break;
	 * case "java.lang.Char":
	 * array.put( key, (char) obj );
	 * break;
	 * default:
	 * if ( obj instanceof Map )
	 * {
	 * StringValue sv = new LargeStringBuilderValue();
	 * sv.append( key );
	 * array.put( sv, castToArrayValue( (Map) obj ) );
	 * }
	 * else
	 * {
	 * array.put( key, obj.toString() );
	 * Loader.getLogger().warning( "Uncaught Convertion to Type: " + obj.getClass().getName() );
	 * }
	 * }
	 * }
	 */
}
