package com.chiorichan.server;

import com.chiorichan.Loader;

public class ServerThread extends Thread
{
	public ServerThread()
	{
		setName( "Resin Server Thread" );
	}
	
	@Override
	public void run()
	{
		try
		{
			Loader.getInstance().getServer().join();
		}
		catch ( InterruptedException e )
		{
			Loader.getLogger().severe( e.getMessage() );
			e.printStackTrace();
		}
	}
}
