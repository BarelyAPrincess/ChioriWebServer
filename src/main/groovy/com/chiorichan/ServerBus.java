/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import joptsimple.OptionSet;

import com.chiorichan.account.AccountAttachment;
import com.chiorichan.account.AccountInstance;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.auth.AccountAuthenticator;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.lang.StartupException;
import com.chiorichan.messaging.MessageSender;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.site.Site;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.terminal.CommandDispatch;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Provides the console output of the server.
 * We also attach the Root Account here
 */
public class ServerBus extends AccountPermissible implements Runnable, AccountAttachment
{
	public static int lastFiveTick = -1;
	
	public static int currentTick = ( int ) ( System.currentTimeMillis() / 50 );
	private ServerLogManager logManager;
	private OptionSet options;
	
	public Thread primaryThread;
	
	public boolean useColors = true;
	
	public Loader loader;
	
	ServerBus()
	{
		
	}
	
	protected static void handleException( StartupException t )
	{
		if ( t.getCause() != null )
			t.getCause().printStackTrace();
		else
			t.printStackTrace();
		
		// TODO Make it so this exception (and possibly other critical exceptions) are reported to
		// us without user interaction. Should also find a way that the log can be sent along with it.
		
		safeLog( Level.SEVERE, LogColor.RED + "" + LogColor.NEGATIVE + "THE SERVER FAILED TO START, CHECK THE LOGS AND TRY AGAIN (" + ( System.currentTimeMillis() - Loader.startTime ) + "ms)!" );
	}
	
	protected static void handleException( Throwable t )
	{
		t.printStackTrace();
		safeLog( Level.WARNING, LogColor.RED + "" + LogColor.NEGATIVE + "**** WE ENCOUNTERED AN UNEXPECTED EXCEPTION ****" );
	}
	
	protected static void safeLog( Level l, String msg )
	{
		if ( Loader.getLogger() != null )
			Loader.getLogger().log( l, msg );
		else
		{
			msg = msg.replaceAll( "&.", "" );
			msg = msg.replaceAll( "§.", "" );
			
			if ( l.intValue() >= Level.WARNING.intValue() )
				System.err.println( msg );
			else
				System.out.println( msg );
		}
	}
	
	@Override
	protected void failedLogin( AccountResult result )
	{
		// Do Nothing
	}
	
	@Override
	public String getDisplayName()
	{
		return "Console";
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		return AccountType.ACCOUNT_ROOT.getEntity();
	}
	
	@Override
	public String getId()
	{
		return AccountType.ACCOUNT_ROOT.getId();
	}
	
	@Override
	public String getIpAddr()
	{
		return null;
	}
	
	@Override
	public Collection<String> getIpAddresses()
	{
		return Lists.newArrayList();
	}
	
	public ServerLogger getLogger()
	{
		return logManager.getLogger();
	}
	
	public ServerLogger getLogger( String loggerId )
	{
		return logManager.getLogger( loggerId );
	}
	
	public ServerLogManager getLogManager()
	{
		return logManager;
	}
	
	@Override
	public AccountPermissible getPermissible()
	{
		return this;
	}
	
	@Override
	public Site getSite()
	{
		return AccountType.ACCOUNT_ROOT.getSite();
	}
	
	@Override
	public String getSiteId()
	{
		return AccountType.ACCOUNT_ROOT.getSiteId();
	}
	
	@Override
	public String getVariable( String string )
	{
		return null;
	}
	
	@Override
	public String getVariable( String key, String def )
	{
		return def;
	}
	
	void init( Loader parent, OptionSet optionSet )
	{
		loader = parent;
		options = optionSet;
		
		if ( options.has( "nocolor" ) )
			useColors = false;
		
		logManager = new ServerLogManager();
		
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter( new DefaultLogFormatter() );
		logManager.addHandler( consoleHandler );
		
		System.setOut( new PrintStream( new LoggerOutputStream( getLogger( "SysOut" ).getLogger(), Level.INFO ), true ) );
		System.setErr( new PrintStream( new LoggerOutputStream( getLogger( "SysErr" ).getLogger(), Level.SEVERE ), true ) );
		
		new ShutdownHook();
		
		primaryThread = new Thread( this, "Server Thread" );
		primaryThread.setPriority( Thread.MAX_PRIORITY );
	}
	
	@Override
	public AccountInstance instance()
	{
		return AccountType.ACCOUNT_ROOT.instance();
	}
	
	public boolean isPrimaryThread()
	{
		return Thread.currentThread().equals( primaryThread );
	}
	
	@Override
	public void login()
	{
		// Disabled and Ignored!
	}
	
	@Override
	public AccountResult login( AccountAuthenticator auth, String acctId, Object... credObjs )
	{
		return new AccountResult( acctId, AccountDescriptiveReason.FEATURE_DISABLED );
	}
	
	@Override
	public AccountResult loginWithException( AccountAuthenticator auth, String acctId, Object... credObjs ) throws AccountException
	{
		throw new AccountException( AccountDescriptiveReason.FEATURE_DISABLED, acctId );
	}
	
	@Override
	public AccountResult logout()
	{
		return new AccountResult( AccountType.ACCOUNT_NONE, AccountDescriptiveReason.FEATURE_DISABLED );
	}
	
	@Override
	public AccountMeta meta()
	{
		return AccountType.ACCOUNT_ROOT;
	}
	
	public void pause( String msg, int timeout )
	{
		int last = 100;
		do
		{
			if ( timeout / 1000 < last )
			{
				getLogger().info( LogColor.GOLD + "" + LogColor.NEGATIVE + String.format( msg, ( ( timeout / 1000 ) + 1 ) + " seconds" ).toUpperCase() + "/r" );
				last = timeout / 1000;
			}
			
			try
			{
				timeout = timeout - 250;
				Thread.sleep( 250 );
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
		while ( timeout > 0 );
	}
	
	public String prompt( String msg, String... keys )
	{
		Scanner scanner = new Scanner( System.in );
		
		getLogger().highlight( msg );
		
		while ( true )
		{
			String key = scanner.next();
			
			for ( String s : keys )
				if ( key.equalsIgnoreCase( s ) || key.toUpperCase().startsWith( s.toUpperCase() ) )
				{
					scanner.close();
					return s;
				}
			
			Loader.getLogger().warning( key + " is not an available option, please press " + Joiner.on( "," ).join( keys ) + " to continue." );
		}
	}
	
	@Override
	public void run()
	{
		try
		{
			long i = System.currentTimeMillis();
			
			// @SuppressWarnings( "unused" )
			// boolean g = false;
			
			// long j = 0L; Loader.isRunning; g = true
			
			long q = 0L;
			long j = 0L;
			for ( ;; )
			{
				long k = System.currentTimeMillis();
				long l = k - i;
				
				if ( l > 2000L && i - q >= 15000L )
				{
					if ( loader.getWarnOnOverload() )
						getLogger().warning( "Can't keep up! Did the system time change, or is the server overloaded?" );
					l = 2000L;
					q = i;
				}
				
				if ( l < 0L )
				{
					getLogger().warning( "Time ran backwards! Did the system time change?" );
					l = 0L;
				}
				
				j += l;
				i = k;
				while ( j > 50L )
				{
					currentTick = ( int ) ( System.currentTimeMillis() / 50 );
					j -= 50L;
					
					CommandDispatch.handleCommands();
					TaskManager.INSTANCE.heartbeat( currentTick );
				}
				
				if ( !Loader.isRunning() )
					break;
				
				Thread.sleep( 1L );
			}
		}
		catch ( Throwable t )
		{
			getLogger().severe( "There was a severe exception thrown by the main tick loop", t );
			// Crash report generate here
		}
		finally
		{
			try
			{
				Loader.shutdown();
			}
			catch ( Throwable throwable1 )
			{
				throwable1.printStackTrace();
			}
		}
	}
	
	@Override
	public void sendMessage( MessageSender sender, Object... objs )
	{
		for ( Object obj : objs )
			try
			{
				logManager.getLogger().info( sender.getDisplayName() + ": " + ObjectFunc.castToStringWithException( obj ) );
			}
			catch ( ClassCastException e )
			{
				logManager.getLogger().info( sender.getDisplayName() + " sent object " + obj.getClass().getName() + " but we had no idea how to properly output it to your terminal." );
			}
	}
	
	@Override
	public void sendMessage( Object... objs )
	{
		for ( Object obj : objs )
			try
			{
				logManager.getLogger().info( ObjectFunc.castToStringWithException( obj ) );
			}
			catch ( ClassCastException e )
			{
				logManager.getLogger().info( "Received object " + obj.getClass().getName() + " but we had no idea how to properly output it to your terminal." );
			}
	}
	
	@Override
	public void setVariable( String key, String value )
	{
		// TODO New Empty Method
	}
	
	public void showBanner()
	{
		try
		{
			InputStream is = null;
			try
			{
				is = Loader.class.getClassLoader().getResourceAsStream( "com/chiorichan/banner.txt" );
				
				String[] banner = new String( FileFunc.inputStream2Bytes( is ) ).split( "\\n" );
				
				for ( String l : banner )
					getLogger().info( LogColor.GOLD + l );
				
				getLogger().info( LogColor.NEGATIVE + "" + LogColor.GOLD + "Starting " + Versioning.getProduct() + " Version " + Versioning.getVersion() );
				getLogger().info( LogColor.NEGATIVE + "" + LogColor.GOLD + Versioning.getCopyright() );
			}
			finally
			{
				if ( is != null )
					is.close();
			}
		}
		catch ( IOException e )
		{
			
		}
	}
	
	@Override
	protected void successfulLogin()
	{
		// Do Nothing
	}
}
