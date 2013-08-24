package com.chiorichan;

import java.io.IOException;

import com.chiorichan.server.Server;

public class ThreadCommandReader extends Thread
{
	public Server server;
	
	public ThreadCommandReader(Server ic)
	{
		server = ic;
	}
	
	public void run()
	{
		if ( !Main.useConsole )
		{
			return;
		}
		
		jline.console.ConsoleReader bufferedreader = server.reader;
		String s;
		
		try
		{
			while ( server.isRunning() )
			{
				if ( Main.useJline )
				{
					s = bufferedreader.readLine( ">", null );
				}
				else
				{
					s = bufferedreader.readLine();
				}
				if ( s != null )
				{
					server.issueCommand( s, null );
				}
			}
		}
		catch ( IOException ioexception )
		{
			java.util.logging.Logger.getLogger( "" ).log( java.util.logging.Level.SEVERE, null, ioexception );
		}
	}
}
