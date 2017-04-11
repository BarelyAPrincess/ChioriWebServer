/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.utils;

import com.google.common.base.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

import java.nio.charset.Charset;

public class UtilNetty
{
	private static final char[] BYTE2CHAR = new char[256];

	private static final String[] BYTE2HEX = new String[256];

	private static final String[] BYTEPADDING = new String[16];

	private static final String[] HEXDUMP_ROWPREFIXES = new String[65536 >>> 4];

	private static final String[] HEXPADDING = new String[16];

	private static final String NEWLINE = StringUtil.NEWLINE;

	static
	{
		int i;

		// Generate the lookup table for byte-to-hex-dump conversion
		for ( i = 0; i < BYTE2HEX.length; i++ )
			// XXX Fix this! Might requite Netty Alpha 2
			BYTE2HEX[i] = ' ' + "";// + StringUtil.byteToHexStringPadded( i );

		// Generate the lookup table for hex dump padding
		for ( i = 0; i < HEXPADDING.length; i++ )
		{
			int padding = HEXPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding * 3 );
			for ( int j = 0; j < padding; j++ )
				buf.append( "   " );
			HEXPADDING[i] = buf.toString();
		}

		// Generate the lookup table for byte dump padding
		for ( i = 0; i < BYTEPADDING.length; i++ )
		{
			int padding = BYTEPADDING.length - i;
			StringBuilder buf = new StringBuilder( padding );
			for ( int j = 0; j < padding; j++ )
				buf.append( ' ' );
			BYTEPADDING[i] = buf.toString();
		}

		// Generate the lookup table for byte-to-char conversion
		for ( i = 0; i < BYTE2CHAR.length; i++ )
			if ( i <= 0x1f || i >= 0x7f )
				BYTE2CHAR[i] = '.';
			else
				BYTE2CHAR[i] = ( char ) i;

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

	/**
	 * Appends the prefix of each hex dump row. Uses the look-up table for the buffer <= 64 KiB.
	 */
	private static void appendHexDumpRowPrefix( StringBuilder dump, int row, int rowStartIndex )
	{
		if ( row < HEXDUMP_ROWPREFIXES.length )
			dump.append( HEXDUMP_ROWPREFIXES[row] );
		else
		{
			dump.append( NEWLINE );
			dump.append( Long.toHexString( rowStartIndex & 0xFFFFFFFFL | 0x100000000L ) );
			dump.setCharAt( dump.length() - 9, '|' );
			dump.append( '|' );
		}
	}

	public static byte[] byteBuf2Bytes( ByteBuf buf )
	{
		byte[] bytes = new byte[buf.readableBytes()];
		int readerIndex = buf.readerIndex();
		buf.getBytes( readerIndex, bytes );
		return bytes;
	}

	public static String byteBuf2String( ByteBuf buf, Charset charset )
	{
		return new String( byteBuf2Bytes( buf ), charset );
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
			return "Buffer: 0B!";

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
			highlightIndex = highlightIndex - 16 * highlightRow - 1;

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
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			dump.append( " |" );

			// ASCII dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
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
				dump.append( BYTE2HEX[buf.getUnsignedByte( j )] );
			dump.append( HEXPADDING[remainder] );
			dump.append( " |" );

			// Ascii dump
			for ( int j = rowStartIndex; j < rowEndIndex; j++ )
				dump.append( BYTE2CHAR[buf.getUnsignedByte( j )] );
			dump.append( BYTEPADDING[remainder] );
			dump.append( '|' );

			if ( highlightIndex > 0 && highlightRow > fullRows + 1 )
				dump.append( " <--" );
		}

		dump.append( NEWLINE + "+--------+-------------------------------------------------+----------------+" );

		return dump.toString();
	}
}
