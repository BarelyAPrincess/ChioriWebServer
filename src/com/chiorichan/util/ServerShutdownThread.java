package com.chiorichan.util;

import com.chiorichan.Loader;

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
			server.shutdown();
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
