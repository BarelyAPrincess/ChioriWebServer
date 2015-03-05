/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan;

import java.io.IOException;
import java.io.InputStream;

import com.chiorichan.session.SessionProviderQuery;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;

/**
 * Used to interact with commands and logs
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class InteractiveConsole
{
	private SessionProviderQuery session;
	private InteractiveConsoleHandler handler;
	
	private InteractiveConsole( InteractiveConsoleHandler handler, SessionProviderQuery session )
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
	
	public void handleMessage( String request )
	{
		if ( request == null || request.isEmpty() )
		{
			handler.println( ConsoleColor.RED + "Your entry was unrecognized. Type \"help\" for help." );
		}
		
		handler.prompt();
	}
	
	public InteractiveConsoleHandler getHandler()
	{
		return handler;
	}
}
