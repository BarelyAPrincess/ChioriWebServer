/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Lists;

/**
 * Monitors and oversees all Watchdog protection
 */
public class Watchdog implements Runnable, TaskRegistrar
{
	public static final int UNKNOWN = -1;
	public static final int TERMINATE = 0;
	public static final int RUNNING = 1;
	public static final int RESTART = 2;
	public static final int CRASHED = 3;
	
	private int state = -1;
	
	private int lastOutput = Timings.epoch();
	private int lastRestart = Timings.epoch();
	
	private Future<String> future = null;
	private ProcessBuilder processBuilder = null;
	private Process process = null;
	
	private SimpleDateFormat dateTimeFormat = new SimpleDateFormat( "MM-dd HH:mm:ss.SSS" );
	
	private Thread watchdogThread;
	int crashCount = 0;
	
	public static int getPid( Process process )
	{
		if ( !process.getClass().getName().equals( "java.lang.UNIXProcess" ) )
			return -1;
		
		try
		{
			Class<?> cProcessImpl = process.getClass();
			Field fPid = cProcessImpl.getDeclaredField( "pid" );
			if ( !fPid.isAccessible() )
				fPid.setAccessible( true );
			return fPid.getInt( process );
		}
		catch ( Exception e )
		{
			return -1;
		}
	}
	
	@Override
	public String getName()
	{
		return "Watchdog";
	}
	
	public void initChild()
	{
		TaskManager.INSTANCE.scheduleAsyncRepeatingTask( this, Ticks.MINUTE_3, Ticks.MINUTE_3, new Runnable()
		{
			@Override
			public void run()
			{
				Loader.getLogger().info( "Watchdog: keepalive" );
			}
		} );
	}
	
	public void initDaemon( String childArgs, OptionSet options )
	{
		if ( !Loader.getServerJar().getName().endsWith( ".jar" ) )
			throw new IllegalStateException( "Watchdog process can only run from compiled jar files" );
		
		log( "Starting " + Versioning.getProduct() + " with Watchdog protection" );
		
		List<String> commands = Lists.newArrayList();
		
		if ( Versioning.isWindows() )
			commands.add( "javaw" );
		else
			commands.add( "java" );
		
		commands.add( "-Xmx" + ( Runtime.getRuntime().maxMemory() / 1000000 ) + "M" );
		
		if ( childArgs != null && !childArgs.isEmpty() )
			commands.addAll( Arrays.asList( childArgs.trim().split( " " ) ) );
		
		commands.add( "-jar" );
		commands.add( Loader.getServerJar().getAbsolutePath() );
		
		for ( Entry<OptionSpec<?>, List<?>> e : options.asMap().entrySet() )
		{
			String key = e.getKey().options().toArray( new String[0] )[0];
			if ( key != null && !key.isEmpty() && !key.equals( "watchdog" ) )
			{
				String arg = ( key.length() == 1 ? "-" : "--" ) + key;
				for ( Object v : e.getValue() )
					if ( v != null && v instanceof String )
					{
						String value = ( ( String ) v );
						commands.add( arg + "=" + ( value.contains( " " ) ? "\"" + value + "\"" : value ) );
					}
			}
		}
		
		commands.add( "--watchdog" );
		commands.add( "--child" );
		
		processBuilder = new ProcessBuilder( commands );
		processBuilder.redirectErrorStream( true );
		
		watchdogThread = new Thread( this, "Watchdog Protection and Monitoring Thread" );
		watchdogThread.setPriority( Thread.MAX_PRIORITY );
		watchdogThread.start();
		
		if ( Versioning.isUnixLikeOS() )
		{
			Signal.handle( new Signal( "TERM" ), new SignalHandler()
			{
				@Override
				public void handle( Signal arg0 )
				{
					log( "Received SIGTERM - Terminate" );
					state = Watchdog.TERMINATE;
					
					if ( future != null )
						future.cancel( true );
				}
			} );
			
			Signal.handle( new Signal( "INT" ), new SignalHandler()
			{
				@Override
				public void handle( Signal arg0 )
				{
					log( "Received SIGINT - Restarting" );
					state = Watchdog.RESTART;
					
					if ( future != null )
						future.cancel( true );
				}
			} );
		}
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
	
	public void log( String msg )
	{
		System.out.println( LogColor.transAltColors( LogColor.RESET + "[" + LogColor.LIGHT_PURPLE + "Watchdog" + LogColor.RESET + "] " + LogColor.GRAY + dateTimeFormat.format( new Date() ) + " " + LogColor.GOLD + msg ) );
	}
	
	@Override
	public void run()
	{
		try
		{
			for ( ;; )
			{
				state = Watchdog.RUNNING;
				process = processBuilder.start();
				InputStream stdout = process.getInputStream();
				
				ExecutorService executor = Executors.newFixedThreadPool( 2 );
				// final PipedOutputStream outputStream = new PipedOutputStream();
				final BufferedReader reader = new BufferedReader( new InputStreamReader( stdout ) );
				
				/*
				 * Runnable writeTask = new Runnable() {
				 * 
				 * @Override
				 * public void run() {
				 * try {
				 * outputStream.write(1);
				 * outputStream.write(2);
				 * Thread.sleep(5000);
				 * outputStream.write(3);
				 * outputStream.close();
				 * } catch (Exception e) {
				 * e.printStackTrace();
				 * }
				 * }
				 * };
				 * executor.submit(writeTask);
				 */
				
				Callable<String> readTask = new Callable<String>()
				{
					@Override
					public String call() throws Exception
					{
						return reader.readLine();
					}
				};
				
				for ( ;; )
					try
					{
						if ( state != RUNNING )
							break;
						
						future = executor.submit( readTask );
						String line = future.get( 30, TimeUnit.SECONDS );
						
						if ( line != null )
						{
							lastOutput = Timings.epoch();
							
							System.out.println( line );
							
							if ( line.contains( "Watchdog: keepalive" ) )
								log( "Watchdog: stillalive" );
						}
						else
							state = TERMINATE;
					}
					catch ( CancellationException e )
					{
						if ( Versioning.isUnixLikeOS() && getPid( process ) > 0 )
							Runtime.getRuntime().exec( "kill -SIGTERM " + getPid( process ) );
						else
							process.destroy();
						break;
					}
					catch ( TimeoutException e )
					{
						int lastTotal = Timings.epoch() - lastOutput;
						if ( lastTotal > Timings.MINUTE_15 )
						{
							log( "No output detected, it's assumed that the server has crashed. Server will now be restarted!" );
							state = RESTART;
						}
						else if ( lastTotal > Timings.MINUTE_5 )
						{
							log( "No output detected for the last " + lastTotal + " seconds." );
							lastOutput = Timings.epoch();
						}
					}
				
				reader.close();
				Thread.sleep( 2000 );
				long exitValue;
				
				try
				{
					exitValue = process.exitValue();
				}
				catch ( IllegalThreadStateException e )
				{
					process.destroy();
					Thread.sleep( 3000 );
					
					try
					{
						exitValue = process.exitValue();
					}
					catch ( IllegalThreadStateException ee )
					{
						exitValue = -1L;
					}
				}
				
				if ( exitValue == 99L )
					state = RESTART;
				
				if ( exitValue == 143L )
					log( "Child process was SIGTERM'd" );
				
				if ( exitValue != 0L && state == RUNNING )
				{
					log( "The server has crashed with exit value " + exitValue + "!" );
					
					state = CRASHED;
					crashCount++;
					
					if ( Timings.epoch() - lastRestart < 5 && crashCount > 3 )
					{
						log( "The server is crashing too frequently! This is obviously not going to succeed, we will quit now." );
						System.exit( 0 );
					}
				}
				
				if ( state == RESTART || state == CRASHED )
				{
					log( "Restarting " + Versioning.getProduct() + "!" );
					lastRestart = Timings.epoch();
				}
				
				if ( state == TERMINATE )
				{
					log( "The server has terminated, have a nice day! :D" );
					System.exit( 0 );
				}
			}
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
}
