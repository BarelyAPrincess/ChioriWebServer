/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Vector;

public class RandomReadWriteByteArray
{
	ByteBuffer buffer = ByteBuffer.allocate( 0 );
	
	public void consumeInputStream( InputStream is ) throws IOException
	{
		int bufSize = 1024;
		
		Vector<Byte> byteV = new Vector<Byte>();
		byte[] tmp1 = new byte[bufSize];
		while ( true )
		{
			int r = is.read( tmp1, 0, bufSize );
			if ( r == -1 )
				break;
			for ( int i = 0; i < r; i++ )
			{
				byteV.add( tmp1[i] );
			}
		}
		byte[] tmp2 = new byte[byteV.size()];
		for ( int i = 0; i < byteV.size(); i++ )
		{
			tmp2[i] = byteV.elementAt( i );
		}
		// XXX Allow multiple InputStreams to be written.
		buffer = ByteBuffer.wrap( tmp2 );
	}
	
	public String readLine()
	{
		StringBuilder out = new StringBuilder();
		boolean cont = true;
		do
		{
			try
			{
				byte b = buffer.get();
				
				if ( ( char ) b == '\r' )
				{
					// DO NOTHING
				}
				else if ( ( char ) b == '\n' )
					cont = false;
				else
					out.append( new String( new byte[] {b} ) );
			}
			catch ( BufferUnderflowException e )
			{
				return null;
			}
		}
		while ( cont );
		
		return out.toString();
	}
	
	public byte[] readUntil( String marker )
	{
		return readUntil( marker, false );
	}
	
	public byte[] readUntil( String marker, boolean caseSensitive )
	{
		while ( true )
		{
			String remaining = new String( readRemainingAndReset() );
			
			int index = ( caseSensitive ) ? remaining.indexOf( marker ) : remaining.toLowerCase().indexOf( marker.toLowerCase() );
			
			if ( index == -1 )
				return readRemaining();
			
			byte[] data = read( index );
			
			String trimString = new String( data );
			
			while ( trimString.endsWith( "\n" ) || trimString.endsWith( "\r" ) )
				trimString = trimString.substring( 0, trimString.length() - 1 );
			
			byte[] output = new byte[trimString.length()];
			System.arraycopy( data, 0, output, 0, trimString.length() );
			
			return output;
		}
	}
	
	public byte[] read( int len )
	{
		byte[] bytes = new byte[len];
		
		for ( int i = 0; i < len; i++ )
		{
			try
			{
				bytes[i] = buffer.get();
			}
			catch ( BufferUnderflowException e )
			{
				break;
			}
		}
		
		return bytes;
	}
	
	public byte[] readRemainingAndReset()
	{
		int pos = buffer.position();
		byte[] bytes = readRemaining();
		buffer.position( pos );
		return bytes;
	}
	
	public byte[] readRemaining()
	{
		byte[] bytes = new byte[buffer.remaining()];
		int cnt = 0;
		
		while ( buffer.remaining() > 0 )
		{
			try
			{
				bytes[cnt] = buffer.get();
				cnt++;
			}
			catch ( BufferUnderflowException e )
			{
				break;
			}
		}
		
		return bytes;
	}
	
	public long size()
	{
		return buffer.limit();
	}
	
	public void clear()
	{
		buffer = ByteBuffer.allocate( 0 );
	}
}
