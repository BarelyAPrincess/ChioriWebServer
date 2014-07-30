package com.chiorichan;

import com.chiorichan.command.Command;

public class ServerShutdownThread extends Thread
{
	private final Loader server;
	
	public ServerShutdownThread(Loader loader)
	{
		this.server = loader;
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
