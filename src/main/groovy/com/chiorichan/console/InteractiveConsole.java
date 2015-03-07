/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.session.SessionProvider;
import com.chiorichan.session.SessionProviderQuery;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

/**
 * Used to interact with commands and logs
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class InteractiveConsole
{
	private Map<String, String> metadata = Maps.newConcurrentMap();
	private InteractiveConsoleHandler handler;
	private SessionProvider session;
	private String prompt = "";
	
	private InteractiveConsole( InteractiveConsoleHandler handler, SessionProvider session )
	{
		this.handler = handler;
		this.session = session;
	}
	
	public static InteractiveConsole createInstance( InteractiveConsoleHandler handler, SessionProviderQuery session )
	{
		return new InteractiveConsole( handler, session );
	}
	
	public void displayWelcomeMessage()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
				
				String[] banner = new String( FileUtil.inputStream2Bytes( is ) ).split( "\\n" );
				
				for ( String l : banner )
				{
					handler.println( ConsoleColor.GOLD + l );
				}
				
				handler.println( ConsoleColor.NEGATIVE + "" + ConsoleColor.GOLD + "Welcome to " + Versioning.getProduct() + " Version " + Versioning.getVersion() + "!" );
				handler.println( ConsoleColor.NEGATIVE + "" + ConsoleColor.GOLD + Versioning.getCopyright() );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			
		}
	}
	
	public InteractiveConsoleHandler getHandler()
	{
		return handler;
	}
	
	public SessionProvider getSession()
	{
		return session;
	}
	
	public void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			handler.println( msg );
	}
	
	public void setMetadata( String key, String val )
	{
		if ( val == null )
			metadata.remove( key );
		else
			metadata.put( key, val );
	}
	
	public String getMetadata( String key )
	{
		return getMetadata( key, null );
	}
	
	public String getMetadata( String key, String def )
	{
		if ( !metadata.containsKey( key ) )
			return def;
		
		return metadata.get( key );
	}
	
	public void setPrompt( String prompt )
	{
		if ( prompt != null )
			this.prompt = prompt;
		
		prompt();
	}
	
	public void resetPrompt()
	{
		try
		{
			prompt = ConsoleColor.GREEN + session.getAccount().getAcctId() + "@" + InetAddress.getLocalHost().getHostName() + ConsoleColor.RESET + ":" + ConsoleColor.BLUE + "~" + ConsoleColor.RESET + "$ ";
		}
		catch ( UnknownHostException e )
		{
			prompt = ConsoleColor.GREEN + session.getAccount().getAcctId() + "@localhost ~$ ";
		}
		
		prompt();
	}
	
	public void prompt()
	{
		handler.print( "\r" + prompt );
	}
}
