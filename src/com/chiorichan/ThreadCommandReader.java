package com.chiorichan;

import java.io.IOException;

public class ThreadCommandReader extends Thread
{
	public Console console;
	
	public ThreadCommandReader(Console ic)
	{
		console = ic;
	}
	
	public void run()
	{
		if ( !console.useConsole )
		{
			return;
		}
		
		jline.console.ConsoleReader bufferedreader = console.reader;
		String s;
		
		try
		{
			while ( console.isRunning() )
			{
				if ( console.useJline )
				{
					s = bufferedreader.readLine( ">", null );
				}
				else
				{
					s = bufferedreader.readLine();
				}
				if ( s != null )
				{
					console.issueCommand( s, null );
				}
			}
		}
		catch ( IOException ioexception )
		{
			java.util.logging.Logger.getLogger( "" ).log( java.util.logging.Level.SEVERE, null, ioexception );
		}
	}
}
