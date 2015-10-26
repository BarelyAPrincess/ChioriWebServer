/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import com.chiorichan.lang.StartupException;
import com.chiorichan.plugin.PluginManager;
import com.google.common.collect.Maps;

/**
 * Provides a simple file watching service for the Server
 * Common detections will be the main jar and plugins
 */
public class ServerFileWatcher implements Runnable, ServerManager
{
	private abstract class EventCallback
	{
		abstract void call( Kind<?> kind, Path path );
	}
	
	private class WatchRef
	{
		WatchKey key;
		Path path;
		
		WatchRef( WatchKey key, Path path )
		{
			this.key = key;
			this.path = path;
		}
		
		public void pollEvents( EventCallback callback )
		{
			for ( WatchEvent<?> event : key.pollEvents() )
			{
				if ( event.kind() == StandardWatchEventKinds.OVERFLOW )
					continue;
				
				WatchEvent<Path> ev = cast( event );
				Path name = ev.context();
				Path child = path.resolve( name );
				
				
				callback.call( event.kind(), child );
				
				if ( event.kind() == StandardWatchEventKinds.ENTRY_CREATE )
					try
					{
						if ( Files.isDirectory( child, LinkOption.NOFOLLOW_LINKS ) )
							registerAll( child );
					}
					catch ( IOException x )
					{
						// Ignore
					}
			}
			
			Loader.getLogger().debug( "Timing Break" );
		}
	}
	
	public static final ServerFileWatcher INSTANCE = new ServerFileWatcher();
	
	private static boolean isInitialized = false;
	private final Thread watcherThread;
	private final WatchService watcher;
	
	private final Map<WatchKey, WatchRef> keys = Maps.newHashMap();
	
	private ServerFileWatcher()
	{
		try
		{
			watcher = FileSystems.getDefault().newWatchService();
			
			watcherThread = new Thread( this, "Server File Watcher Thread" );
			watcherThread.setPriority( Thread.MIN_PRIORITY );
			watcherThread.start();
		}
		catch ( IOException e )
		{
			throw new StartupException( e );
		}
	}
	
	@SuppressWarnings( "unchecked" )
	static <T> WatchEvent<T> cast( WatchEvent<?> event )
	{
		return ( WatchEvent<T> ) event;
	}
	
	public static void init() throws StartupException
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Server File Watcher has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	/**
	 * Initializes the Server File Watcher
	 * 
	 * @throws StartupException
	 *             If there was any problems
	 */
	private void init0() throws StartupException
	{
		try
		{
			register( Loader.getServerRoot() );
			registerAll( Loader.getPluginsDirectory() );
		}
		catch ( IOException e )
		{
			throw new StartupException( e );
		}
	}
	
	private void register( File file ) throws IOException
	{
		if ( !file.isDirectory() )
			throw new IOException( "Path is not a directory" );
		
		final Path path = FileSystems.getDefault().getPath( file.getAbsolutePath() );
		register( path );
	}
	
	/**
	 * Register the given directory with the WatchService
	 */
	private void register( Path dir ) throws IOException
	{
		WatchKey key = dir.register( watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY );
		WatchRef ref = new WatchRef( key, dir );
		
		WatchRef prev = keys.get( key );
		if ( prev == null )
			Loader.getLogger().fine( String.format( "Now watching directory '%s'", dir ) );
		else if ( !dir.equals( prev.path ) )
			Loader.getLogger().fine( String.format( "Updated directory watch from '%s' to '%s'", prev.path, dir ) );
		
		keys.put( key, ref );
	}
	
	private void registerAll( File file ) throws IOException
	{
		if ( !file.isDirectory() )
			throw new IOException( "Path is not a directory" );
		
		final Path path = FileSystems.getDefault().getPath( file.getAbsolutePath() );
		registerAll( path );
	}
	
	/**
	 * Register the given directory, and all its sub-directories, with the WatchService.
	 */
	private void registerAll( final Path start ) throws IOException
	{
		Files.walkFileTree( start, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
			{
				register( dir );
				return FileVisitResult.CONTINUE;
			}
		} );
	}
	
	@Override
	public void run()
	{
		for ( ;; )
		{
			WatchKey key;
			try
			{
				key = watcher.take();
			}
			catch ( InterruptedException x )
			{
				return;
			}
			
			WatchRef ref = keys.get( key );
			
			if ( ref == null )
				continue;
			
			ref.pollEvents( new EventCallback()
			{
				@Override
				void call( Kind<?> kind, Path path )
				{
					Loader.getLogger().info( String.format( "%s: %s", kind.name(), path ) );
				}
			} );
			
			boolean valid = key.reset();
			if ( !valid )
			{
				keys.remove( key );
				
				if ( keys.isEmpty() )
					break;
			}
		}
	}
}
