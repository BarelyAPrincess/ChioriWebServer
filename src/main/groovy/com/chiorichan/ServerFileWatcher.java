/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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

import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.services.AppManager;
import com.chiorichan.services.ServiceManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Maps;

/**
 * Provides a simple file watching service for the Server
 * Common detections will be the main jar and plugins
 */
public class ServerFileWatcher implements Runnable, ServiceManager, TaskRegistrar
{
	public interface EventCallback
	{
		void call( Kind<?> kind, File file, boolean isDirectory );
	}

	private class TriggerRef
	{
		Kind<?> kind;
		Path path;
		boolean isDirectory;
		EventCallback callback;

		long epoch = Timings.epoch();
		boolean called = false;

		TriggerRef( Kind<?> kind, Path path, boolean isDirectory, EventCallback callback )
		{
			this.kind = kind;
			this.path = path;
			this.isDirectory = isDirectory;
			this.callback = callback;
		}
	}

	private class WatchRef
	{
		WatchKey key;
		Path path;
		EventCallback callback;
		boolean recursive;

		WatchRef( WatchKey key, Path path, EventCallback callback, boolean recursive )
		{
			this.key = key;
			this.path = path;
			this.callback = callback;
			this.recursive = recursive;
		}

		public void pollEvents()
		{
			boolean allCalled = true;
			for ( TriggerRef ref : triggerReferences.values() )
				if ( !ref.called )
					allCalled = false;
			if ( allCalled )
				triggerReferences.clear();

			for ( WatchEvent<?> event : key.pollEvents() )
			{
				if ( event.kind() == StandardWatchEventKinds.OVERFLOW )
					continue;

				WatchEvent<Path> ev = cast( event );
				Path name = ev.context();
				Path child = path.resolve( name );

				boolean isDirectory = Files.isDirectory( child, LinkOption.NOFOLLOW_LINKS );

				if ( recursive && event.kind() == StandardWatchEventKinds.ENTRY_CREATE && isDirectory )
					registerRecursive( child, callback );

				TriggerRef ref = triggerReferences.get( child.toString() + "--" + event.kind().name() );

				if ( ref == null )
					triggerReferences.put( child.toString() + "--" + event.kind().name(), new TriggerRef( event.kind(), child, isDirectory, callback ) );
				else
				{
					ref.epoch = Timings.epoch();
					ref.called = false;
				}
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	static <T> WatchEvent<T> cast( WatchEvent<?> event )
	{
		return ( WatchEvent<T> ) event;
	}

	public static Log getLogger()
	{
		return AppManager.manager( ServerFileWatcher.class ).getLogger();
	}

	public static ServerFileWatcher instance()
	{
		return AppManager.manager( ServerFileWatcher.class ).instance();
	}

	private final Map<String, TriggerRef> triggerReferences = Maps.newLinkedHashMap();

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

	@Override
	public String getLoggerId()
	{
		return "FileWatcher";
	}

	@Override
	public String getName()
	{
		return "ServerFileWatcher";
	}

	@Override
	public void init() throws ApplicationException
	{
		TaskManager.instance().scheduleAsyncRepeatingTask( this, Ticks.SECOND_5, Ticks.SECOND_5, () ->
		{
			long epoch = Timings.epoch();
			for ( TriggerRef tr : triggerReferences.values().toArray( new TriggerRef[0] ) )
				if ( epoch - tr.epoch > 1 && !tr.called )
				{
					tr.callback.call( tr.kind, tr.path.toFile(), tr.isDirectory );
					tr.called = true;
				}
		} );
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}

	public void register( File file, EventCallback callback ) throws IOException
	{
		final Path path = FileSystems.getDefault().getPath( file.getAbsolutePath() );
		register( path, callback );
	}

	/**
	 * Register the given directory with the WatchService
	 */
	public void register( Path dir, EventCallback callback ) throws IOException
	{
		register( dir, callback, false );
	}

	private void register( Path dir, EventCallback callback, boolean recursive ) throws IOException
	{
		WatchKey key = dir.register( watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY );
		WatchRef ref = new WatchRef( key, dir, callback, recursive );

		WatchRef prev = keys.get( key );
		if ( prev == null )
			getLogger().fine( String.format( "Now watching directory '%s' for changes", dir ) );
		else if ( !dir.equals( prev.path ) )
			getLogger().fine( String.format( "Updated directory watch from '%s' to '%s'", prev.path, dir ) );

		keys.put( key, ref );
	}

	public void registerRecursive( File file, EventCallback callback )
	{
		final Path path = FileSystems.getDefault().getPath( file.getAbsolutePath() );
		registerRecursive( path, callback );
	}

	/**
	 * Register the given directory, and all its sub-directories, with the WatchService.
	 */
	public void registerRecursive( final Path start, final EventCallback callback )
	{
		try
		{
			Files.walkFileTree( start, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException
				{
					register( dir, callback, true );
					return FileVisitResult.CONTINUE;
				}
			} );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		for ( ; ; )
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

			ref.pollEvents();

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
