/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

import java.math.BigDecimal;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Strings;

public class ObjectFunc
{
	private static final String[] HEXDUMP_ROWPREFIXES = new String[65536 >>> 4];
	private static final String NEWLINE = StringUtil.NEWLINE;
	private static final String[] BYTE2HEX = new String[256];
	private static final char[] BYTE2CHAR = new char[256];
	private static final String[] HEXPADDING = new String[16];
	private static final String[] BYTEPADDING = new String[16];
	
	static
	{
		int i;
		
		// Generate the lookup table for byte-to-hex-dump conversion
		for ( i = 0; i < BYTE2HEX.length; i++ )
		{
			// XXX Fix this! Might requite Netty Alpha 2
			BYTE2HEX[i] = ' ' + "";// + StringUtil.byteToHexStringPadded( i );
		}
		
		// Generate the lookup table for hex dump paddings
		for ( i = 0; i < HEXPADDING.length; i++ )
		{
			int padding = HEXPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding * 3 );
			for ( int j = 0; j < padding; j++ )
			{
				buf.append( "   " );
			}
			HEXPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte dump paddings
		for ( i = 0; i < BYTEPADDING.length; i++ )
		{
			int padding = BYTEPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding );
			for ( int j = 0; j < padding; j++ )
			{
				buf.append( ' ' );
			}
			BYTEPADDING[i] = buf.toString();
		}
		
		// Generate the lookup table for byte-to-char conversion
		for ( i = 0; i < BYTE2CHAR.length; i++ )
		{
			if ( i <= 0x1f || i >= 0x7f )
			{
				BYTE2CHAR[i] = '.';
			}
			else
			{
				BYTE2CHAR[i] = ( char ) i;
			}
		}
		
		// Generate the lookup table for the start-offset header in each row (up to 64KiB).
		for ( i = 0; i < HEXDUMP_ROWPREFIXES.length; i++ )
		{
			StringBuilder buf = new StringBuilder( 12 );
			buf.append( NEWLINE );
			buf.append( Long.toHexString( i << 4 & 0xFFFFFFFFL | 0x100000000L ) );
			buf.setCharAt( buf.length() - 9, '|' );
			buf.append( '|' );
			HEXDUMP_ROWPREFIXES[i] = buf.toString();
		}
	}
	
	public static String hexDump( ByteBuf buf )
	{
		return hexDump( buf, buf.readerIndex() );
	}
	
	public static String hexDump( ByteBuf buf, int highlightIndex )
	{
		if ( buf == null )
			return "Buffer: null!";
		
		if ( buf.capacity() < 1 )
		{
			return "Buffer: 0B!";
		}
		
		StringBuilder dump = new StringBuilder();
		
		final int startIndex = 0;
		final int endIndex = buf.capacity();
		final int length = endIndex - startIndex;
		final int fullRows = length >>> 4;
		final int remainder = length & 0xF;
		
		int highlightRow = -1;
		
		dump.append( NEWLINE + "         +-------------------------------------------------+" + NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		if ( highlightIndex > 0 )
		{
			highlightRow = highlightIndex >>> 4;
			highlightIndex = highlightIndex - ( 16 * highlightRow ) - 1;
			
			dump.append( NEWLINE + "|        |" + Strings.repeat( "   ", highlightIndex ) + " $$" + Strings.repeat( "   ", 15 - highlightIndex ) );
			dump.append( " |" + Strings.repeat( " ", highlightIndex ) + "$" + Strings.repeat( " ", 15 - highlightIndex ) + "|" );
		}
		
		// Dump the rows which have 16 bytes.
		for ( int row = 0; row < fullRows; row++ )
		{
			int rowStartIndex = row << 4;
			
			// Per-row prefix.
			appendHexDumpRowPrefix( dump, row, rowStartIndex );
			
			// Hex dump
			int rowEndIndex = rowStartIndex + 16;
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			}
			dump.append( " |" );
			
			// ASCII dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
			}
			dump.append( '|' );
			
			if ( highlightIndex > 0 && highlightRow == row + 1 )
				dump.append( " <--" );
		}
		
		// Dump the last row which has less than 16 bytes.
		if ( remainder != 0 )
		{
			int rowStartIndex = fullRows << 4;
			appendHexDumpRowPrefix( dump, fullRows, rowStartIndex );
			
			// Hex dump
			int rowEndIndex = rowStartIndex + remainder;
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			}
			dump.append( HEXPADDING[remainder] );
			dump.append( " |" );
			
			// Ascii dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
			{
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
			}
			dump.append( BYTEPADDING[remainder] );
			dump.append( '|' );
			
			if ( highlightIndex > 0 && highlightRow > fullRows + 1 )
				dump.append( " <--" );
		}
		
		dump.append( NEWLINE + "+--------+-------------------------------------------------+----------------+" );
		
		return dump.toString();
	}
	
	/**
	 * Appends the prefix of each hex dump row. Uses the look-up table for the buffer <= 64 KiB.
	 */
	private static void appendHexDumpRowPrefix( StringBuilder dump, int row, int rowStartIndex )
	{
		if ( row < HEXDUMP_ROWPREFIXES.length )
		{
			dump.append( HEXDUMP_ROWPREFIXES[row] );
		}
		else
		{
			dump.append( NEWLINE );
			dump.append( Long.toHexString( rowStartIndex & 0xFFFFFFFFL | 0x100000000L ) );
			dump.setCharAt( dump.length() - 9, '|' );
			dump.append( '|' );
		}
	}
	
	public static String hex2Readable( int... elements )
	{
		byte[] e2 = new byte[elements.length];
		for ( int i = 0; i < elements.length; i++ )
			e2[i] = ( byte ) elements[i];
		return hex2Readable( e2 );
	}
	
	public static String hex2Readable( byte... elements )
	{
		// TODO Char Dump
		String result = "";
		char[] chars = Hex.encodeHex( elements, true );
		for ( int i = 0; i < chars.length; i = i + 2 )
			result += " " + chars[i] + chars[i + 1];
		
		if ( result.length() > 0 )
			result = result.substring( 1 );
		
		return result;
	}
	
	public static int safeLongToInt( long l )
	{
		if ( l < Integer.MIN_VALUE )
			return Integer.MIN_VALUE;
		if ( l > Integer.MAX_VALUE )
			return Integer.MAX_VALUE;
		return ( int ) l;
	}
	
	public static Boolean castToBoolWithException( Object value ) throws ClassCastException
	{
		if ( value == null )
			throw new ClassCastException( "Can't Cast `null` to Boolean" );
		
		if ( value.getClass() == boolean.class || value.getClass() == Boolean.class )
			return ( boolean ) value;
		
		String val = castToStringWithException( value );
		
		if ( val == null )
			throw new ClassCastException( "Uncaught Convertion to Boolean of Type: " + value.getClass().getName() );
		
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
				throw new ClassCastException( "Uncaught Convertion to Boolean of Type: " + value.getClass().getName() );
		}
	}
	
	public static Boolean castToBool( Object value )
	{
		try
		{
			return castToBoolWithException( value );
		}
		catch ( Exception e )
		{
			return false;
		}
	}
	
	public static Double castToDoubleWithException( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return ( ( Long ) value ).doubleValue();
			case "java.lang.String":
				return Double.parseDouble( ( String ) value );
			case "java.lang.Integer":
				return ( ( Integer ) value ).doubleValue();
			case "java.lang.Double":
				return ( Double ) value;
			case "java.lang.Boolean":
				return ( ( boolean ) value ) ? 1D : 0D;
			case "java.math.BigDecimal":
				return ( ( BigDecimal ) value ).setScale( 0, BigDecimal.ROUND_HALF_UP ).doubleValue();
			default:
				throw new ClassCastException( "Uncaught Convertion to Integer of Type: " + value.getClass().getName() );
		}
	}
	
	public static Double castToDouble( Object value )
	{
		try
		{
			return castToDoubleWithException( value );
		}
		catch ( Exception e )
		{
			return 0D;
		}
	}
	
	public static Integer castToIntWithException( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				if ( ( long ) value < Integer.MIN_VALUE || ( long ) value > Integer.MAX_VALUE )
					return ( Integer ) value;
				else
					return null;
			case "java.lang.String":
				return Integer.parseInt( ( String ) value );
			case "java.lang.Integer":
				return ( Integer ) value;
			case "java.lang.Double":
				return ( Integer ) value;
			case "java.lang.Boolean":
				return ( ( boolean ) value ) ? 1 : 0;
			case "java.math.BigDecimal":
				return ( ( BigDecimal ) value ).setScale( 0, BigDecimal.ROUND_HALF_UP ).intValue();
			default:
				throw new ClassCastException( "Uncaught Convertion to Integer of Type: " + value.getClass().getName() );
		}
	}
	
	public static Integer castToInt( Object value )
	{
		try
		{
			return castToIntWithException( value );
		}
		catch ( Exception e )
		{
			return -1;
		}
	}
	
	public static Long castToLongWithException( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return ( Long ) value;
			case "java.lang.String":
				return Long.parseLong( ( String ) value );
			case "java.lang.Integer":
				return Long.parseLong( "" + value );
			case "java.lang.Double":
				return Long.parseLong( "" + value );
			case "java.lang.Boolean":
				return ( ( boolean ) value ) ? 1L : 0L;
			case "java.math.BigDecimal":
				return ( ( BigDecimal ) value ).setScale( 0, BigDecimal.ROUND_HALF_UP ).longValue();
			default:
				throw new ClassCastException( "Uncaught Convertion to Long of Type: " + value.getClass().getName() );
		}
	}
	
	public static Long castToLong( Object value )
	{
		try
		{
			return castToLongWithException( value );
		}
		catch ( ClassCastException e )
		{
			e.printStackTrace();
			return 0L;
		}
	}
	
	public static String castToStringWithException( Object value )
	{
		if ( value == null )
			return null;
		
		switch ( value.getClass().getName() )
		{
			case "java.lang.Long":
				return Long.toString( ( long ) value );
			case "java.lang.String":
				return ( String ) value;
			case "java.lang.Integer":
				return Integer.toString( ( int ) value );
			case "java.lang.Double":
				return Double.toString( ( double ) value );
			case "java.lang.Boolean":
				return ( ( boolean ) value ) ? "true" : "false";
			case "java.math.BigDecimal":
				return ( ( BigDecimal ) value ).toString();
			case "java.util.Map":
				return value.toString();
			case "java.util.List":
				return value.toString();
			default:
				throw new ClassCastException( "Uncaught Convertion to String of Type: " + value.getClass().getName() );
		}
	}
	
	public static String castToString( Object value )
	{
		try
		{
			return castToStringWithException( value );
		}
		catch ( ClassCastException e )
		{
			return null;
		}
	}
	
	@SuppressWarnings( "unchecked" )
	public static <O> O castThis( Class<?> clz, Object o )
	{
		try
		{
			if ( clz == Integer.class )
				return ( O ) castToIntWithException( o );
			if ( clz == Long.class )
				return ( O ) castToLongWithException( o );
			if ( clz == Double.class )
				return ( O ) castToDoubleWithException( o );
			if ( clz == Boolean.class )
				return ( O ) castToBoolWithException( o );
			if ( clz == String.class )
				return ( O ) castToStringWithException( o );
		}
		catch ( Exception e1 )
		{
			try
			{
				return ( O ) o;
			}
			catch ( Exception e2 )
			{
				try
				{
					return ( O ) castToStringWithException( o );
				}
				catch ( Exception e3 )
				{
					try
					{
						/*
						 * Last and final attempt to get something out of this
						 * object even if it results in the toString() method.
						 */
						return ( O ) ( "" + o );
					}
					catch ( Exception e4 )
					{
						
					}
				}
			}
		}
		
		return null;
	}
}
