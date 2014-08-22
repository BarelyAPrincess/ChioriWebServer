package com.chiorichan;

import java.io.IOException;

import jline.console.ConsoleReader;

import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.account.system.SystemAccounts;
import com.chiorichan.bus.ConsoleBus;

public class ThreadCommandReader extends Thread implements SentientHandler
{
	public ConsoleBus console;
	public Sentient sentient = (Sentient) SystemAccounts.NO_LOGIN;
	
	public ThreadCommandReader(ConsoleBus cb)
	{
		console = cb;
		sentient.putHandler( this );
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
			while ( Loader.isRunning() )
			{
				if ( console.useJline )
				{
					String login = ( sentient == null ) ? "noLogin" : sentient.getId();
					s = bufferedreader.readLine( "(" + login + ") ?>", null );
				}
				else
				{
					s = bufferedreader.readLine();
				}
				
				if ( s != null )
				{
					console.issueCommand( this, s );
				}
			}
		}
		catch ( IOException e )
		{
			Loader.getLogger().severe( "Exception encountered in the ThreadedConsoleReader.", e );
		}
	}
	
	@Override
	public boolean kick( String kickMessage )
	{
		console.getLogger().info( kickMessage );
		
		removeSentient();
		return true;
	}
	
	@Override
	public void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			console.getLogger().info( msg );
	}
	
	@Override
	public void attachSentient( Sentient _sentient )
	{
		sentient = _sentient;
		sentient.putHandler( this );
		
		Loader.getPermissionsManager().subscribeToPermission( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE, _sentient );
		Loader.getPermissionsManager().subscribeToPermission( Loader.BROADCAST_CHANNEL_USERS, _sentient );
	}
	
	@Override
	public void removeSentient()
	{
		Loader.getPermissionsManager().unsubscribeFromPermission( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE, sentient );
		Loader.getPermissionsManager().unsubscribeFromPermission( Loader.BROADCAST_CHANNEL_USERS, sentient );
		
		sentient = SystemAccounts.NO_LOGIN;
	}
	
	@Override
	public Sentient getSentient()
	{
		return sentient;
	}
	
	@Override
	public boolean isValid()
	{
		return true; // ALWAYS VALID
	}

	@Override
	public String getIpAddr()
	{
		return null;
	}
}
