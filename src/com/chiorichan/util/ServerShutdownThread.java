package com.chiorichan.util;

import com.chiorichan.server.Server;

public class ServerShutdownThread extends Thread
{
	private final Server server;
	
	public ServerShutdownThread(Server server)
	{
		this.server = server;
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
				server.reader.getTerminal().restore();
			}
			catch ( Exception e )
			{}
		}
	}
}
