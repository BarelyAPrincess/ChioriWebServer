/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import java.math.BigDecimal;

import com.chiorichan.Loader;

public class ObjectUtil
{
	public static Boolean castToBool( Object value )
	{
		if ( value == null )
			return false;
		
		if ( value.getClass() == Boolean.class )
			return (Boolean) value;
		
		String val = castToString( value );
		
		if ( val == null )
			return false;
		
		switch ( val.trim().toLowerCase() )
		{
			case "yes":
				return true;
			case "no":
				return false;
			case "true":
				return true;
			case "false":
				return false;
			case "1":
				return true;
			case "0":
				return false;
			default:
				return false;
		}
	}
	
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
				return ((boolean) value) ? 1L : 0L;
			case "java.math.BigDecimal":
				return ((BigDecimal) value).setScale( 0, BigDecimal.ROUND_HALF_UP ).longValue();
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
				return ((boolean) value) ? "true" : "false";
			case "java.math.BigDecimal":
				return ((BigDecimal) value).toString();
			case "java.util.Map":
				return value.toString();
			case "java.util.List":
				return value.toString();
			default:
				Loader.getLogger().warning( "Uncaught Convertion to String of Type: " + value.getClass().getName() );
				return null;
		}
	}
}
