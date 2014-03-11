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
			Command.broadcastCommandMessage( Loader.getConsole(), "Stopping the server... Goodbye!" );
			System.out.println( "Stopping the server... Goodbye!" );
			
			if ( server.isRunning() )
				Loader.stop();
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
