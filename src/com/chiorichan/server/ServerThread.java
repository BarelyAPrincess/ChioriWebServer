package com.chiorichan.server;

public class ServerThread extends Thread
{
	public ServerThread()
	{
		setName( "Resin Server Thread" );
	}
	
	@Override
	public void run()
	{
		Server.getResinServer().join();
	}
}
