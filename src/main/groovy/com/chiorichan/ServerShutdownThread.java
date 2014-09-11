/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import com.chiorichan.command.Command;

public class ServerShutdownThread extends Thread
{
	//private final Loader server;
	
	public ServerShutdownThread(Loader loader)
	{
		//this.server = loader;
	}
	
	@Override
	public void run()
	{
		try
		{
			Command.broadcastCommandMessage( Loader.getConsole().getConsoleReader(), "Stopping the server... Goodbye!" );
			
			if ( Loader.isRunning() )
				Loader.stop( "Stopping the server... Goodbye!" );
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				Loader.getConsole().getReader().getTerminal().restore();
			}
			catch ( Exception e )
			{}
		}
	}
}
