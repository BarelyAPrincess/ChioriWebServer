package com.chiorichan;

import java.io.IOException;

import jline.console.ConsoleReader;

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
		
		ConsoleReader bufferedreader = console.reader;
		String s;
		
		try
		{
			while ( console.isRunning() )
			{
				if ( !console.promptForAuth() )
				{
					if ( console.useJline )
					{
						s = bufferedreader.readLine( "?>", null );
					}
					else
					{
						s = bufferedreader.readLine();
					}
					
					if ( s != null )
					{
						console.issueCommand( s, console );
					}
				}
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "Exception encountered in the ThreadedConsoleReader.", e );
		}
	}
}
