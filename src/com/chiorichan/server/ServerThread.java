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
		Loader.getInstance().getResinServer().join();
	}
}
