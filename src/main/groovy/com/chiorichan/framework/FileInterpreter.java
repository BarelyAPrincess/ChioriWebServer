/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.util.Charsets;

import com.chiorichan.ContentTypes;
import com.chiorichan.InterpreterOverrides;
import com.chiorichan.Loader;
import com.google.common.collect.Maps;

public class FileInterpreter
{
	protected Charset encoding = null;
	protected Map<String, String> interpParams = Maps.newTreeMap();
	protected ByteBuf data = Unpooled.buffer();
	protected File cachedFile = null;
	
	public Charset getEncoding()
	{
		return encoding;
	}
	
	public String getEncodingName()
	{
		return encoding.name();
	}
	
	public void setEncoding( Charset encoding )
	{
		this.encoding = encoding;
		interpParams.put( "encoding", encoding.name() );
	}
	
	@Override
	public String toString()
	{
		String overrides = "";
		
		for ( Entry<String, String> o : interpParams.entrySet() )
		{
			overrides += "," + o.getKey() + "=" + o.getValue();
		}
		
		if ( overrides.length() > 1 )
			overrides = overrides.substring( 1 );
		
		String cachedFileStr = ( cachedFile == null ) ? "N/A" : cachedFile.getAbsolutePath();
		
		return "FileInterpreter{content=" + data.writerIndex() + " bytes,file=" + cachedFileStr + ",overrides={" + overrides + "}}";
	}
	
	public File getFile()
	{
		return cachedFile;
	}
	
	public FileInterpreter()
	{
		encoding = Charsets.toCharset( Loader.getConfig().getString( "server.defaultBinaryEncoding", "ISO-8859-1" ) );
		
		// All param keys are lower case. No such thing as a non-lowercase param keys because keys are forced to lowercase.
		interpParams.put( "title", null );
		interpParams.put( "reqperm", "-1" );
		interpParams.put( "theme", null );
		interpParams.put( "view", null );
		
		interpParams.put( "html", null );
		interpParams.put( "file", null );
		
		// Shell Options (groovy,text,html)
		interpParams.put( "shell", null );
		interpParams.put( "encoding", encoding.name() );
	}
	
	public FileInterpreter( File file ) throws IOException
	{
		this();
		interpretParamsFromFile( file );
	}
	
	public static String determineShellFromName( String fileName )
	{
		fileName = fileName.toLowerCase();
		
		String shell = InterpreterOverrides.getShellForExt( InterpreterOverrides.getFileExtension( fileName ) );
		
		if ( shell == null || shell.isEmpty() )
			return InterpreterOverrides.getFileExtension( fileName );
		
		return shell;
	}
	
	public static String readLine( ByteBuf buf )
	{
		String op = "";
		while ( buf.isReadable() )
		{
			byte bb = buf.readByte();
			if ( bb == '\n' )
				break;
			op += ( char ) bb;
		}
		return op;
	}
	
	public final void interpretParamsFromFile( File file ) throws IOException
	{
		if ( file == null || !file.exists() )
			return;
		
		FileInputStream is = null;
		try
		{
			cachedFile = file;
			
			interpParams.put( "file", file.getAbsolutePath() );
			
			if ( file.isDirectory() )
			{
				interpParams.put( "shell", "embedded" );
			}
			else
			{
				if ( !interpParams.containsKey( "shell" ) || interpParams.get( "shell" ) == null )
				{
					String shell = determineShellFromName( file.getName() );
					if ( shell != null && !shell.isEmpty() )
						interpParams.put( "shell", shell );
				}
				
				is = new FileInputStream( file );
				
				ByteBuf buf = Unpooled.wrappedBuffer( IOUtils.toByteArray( is ) );
				boolean beginContent = false;
				int lastInx;
				
				data = Unpooled.buffer();
				
				do
				{
					lastInx = buf.readerIndex();
					String l = readLine( buf );
					if ( l.trim().startsWith( "@" ) )
						try
						{
							/* Only solution I could think of for CSS files since they use @annotations too, so we share them. */
							if ( ContentTypes.getContentType( file ).equalsIgnoreCase( "text/css" ) )
								data.writeBytes( ( l + "\n" ).getBytes() );
							/* Only solution I could think of for CSS files since they use @annotations too, so we share them. */
							
							String key;
							String val = "";
							
							if ( l.contains( " " ) )
							{
								key = l.trim().substring( 1, l.trim().indexOf( " " ) );
								val = l.trim().substring( l.trim().indexOf( " " ) + 1 );
							}
							else
								key = l;
							
							if ( val.endsWith( ";" ) )
								val = val.substring( 0, val.length() - 1 );
							
							if ( val.startsWith( "'" ) && val.endsWith( "'" ) )
								val = val.substring( 1, val.length() - 1 );
							
							interpParams.put( key.toLowerCase(), val );
							Loader.getLogger().finer( "Setting param '" + key + "' to '" + val + "'" );
							
							if ( key.equals( "encoding" ) )
							{
								if ( Charset.isSupported( val ) )
									setEncoding( Charsets.toCharset( val ) );
								else
									Loader.getLogger().severe( "The file '" + file.getAbsolutePath() + "' requested encoding '" + val + "' but it's not supported by the JVM!" );
							}
						}
						catch ( NullPointerException | ArrayIndexOutOfBoundsException e )
						{
							
						}
					else if ( l.trim().isEmpty() )
					{
						// Continue reading, this line is empty.
					}
					else
					{
						// We encountered the beginning of the file content.
						beginContent = true;
						buf.readerIndex( lastInx ); // This rewinds the buffer to the last reader index
					}
				}
				while ( !beginContent );
				
				data.writeBytes( buf );
			}
		}
		finally
		{
			if ( is != null )
				is.close();
		}
	}
	
	public String getContentType()
	{
		if ( cachedFile == null )
			return "text/html";
		
		String type = get( "contenttype" );
		
		if ( type == null || type.isEmpty() )
			type = ContentTypes.getContentType( cachedFile.getAbsoluteFile() );
		
		if ( type.startsWith( "text" ) )
			setEncoding( Charsets.toCharset( Loader.getConfig().getString( "server.defaultTextEncoding", "UTF-8" ) ) );
		else
			setEncoding( Charsets.toCharset( Loader.getConfig().getString( "server.defaultBinaryEncoding", "ISO-8859-1" ) ) );
		
		return type;
	}
	
	public Map<String, String> getParams()
	{
		return interpParams;
	}
	
	public String consumeString()
	{
		return new String( consumeBytes(), encoding );
	}
	
	public byte[] consumeBytes()
	{
		byte[] bytes = new byte[data.readableBytes()];
		int inx = data.readerIndex();
		data.readBytes( bytes );
		data.readerIndex( inx );
		return bytes;
	}
	
	public String get( String key )
	{
		if ( !interpParams.containsKey( key.toLowerCase() ) )
			return null;
		
		return interpParams.get( key.toLowerCase() );
	}
	
	public void put( String key, String value )
	{
		interpParams.put( key.toLowerCase(), value );
	}
}
