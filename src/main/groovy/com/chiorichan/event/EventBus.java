/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.Warning;
import com.chiorichan.Warning.WarningState;
import com.chiorichan.plugin.AuthorNagException;
import com.chiorichan.plugin.PluginBase;

public class EventBus
{
	private boolean useTimings = false;
	
	public EventBus()
	{
		
	}
	
	/**
	 * Calls an event with the given details.<br>
	 * This method only synchronizes when the event is not asynchronous.
	 * 
	 * @param event
	 *             Event details
	 */
	public void callEvent( Event event )
	{
		try
		{
			if ( event.isAsynchronous() )
			{
				if ( Thread.holdsLock( this ) )
				{
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
				}
				if ( Loader.getConsoleBus().isPrimaryThread() )
				{
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
				}
				fireEvent( event );
			}
			else
			{
				synchronized ( this )
				{
					fireEvent( event );
				}
			}
		}
		catch ( EventException ex )
		{	
			
		}
	}
	
	/**
	 * Calls an event with the given details.<br>
	 * This method only synchronizes when the event is not asynchronous.
	 * 
	 * @param event
	 *             Event details
	 * @throws EventException
	 */
	public void callEventWithException( Event event ) throws EventException
	{
		if ( event.isAsynchronous() )
		{
			if ( Thread.holdsLock( this ) )
			{
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
			}
			if ( Loader.getConsole().isPrimaryThread() )
			{
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
			}
			fireEvent( event );
		}
		else
		{
			synchronized ( this )
			{
				fireEvent( event );
			}
		}
	}
	
	private void fireEvent( Event event ) throws EventException
	{
		HandlerList handlers = event.getHandlers();
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		
		for ( RegisteredListener registration : listeners )
		{
			if ( !registration.getCreator().isEnabled() )
			{
				continue;
			}
			
			try
			{
				registration.callEvent( event );
			}
			catch ( AuthorNagException ex )
			{
				if ( registration.getCreator() instanceof PluginBase )
				{
					PluginBase plugin = (PluginBase) registration.getCreator();
					
					if ( plugin.isNaggable() )
					{
						plugin.setNaggable( false );
						Loader.getLogger().log( Level.SEVERE, String.format( "Nag author(s): '%s' of '%s' about the following: %s", plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(), ex.getMessage() ) );
					}
				}
			}
			catch ( EventException ex )
			{
				if ( ex.getCause() == null )
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getDescription().getFullName() + "\nEvent Exception Reason: " + ex.getMessage() );
				else
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getDescription().getFullName() + "\nEvent Exception Reason: " + ex.getCause().getMessage() );
				throw ex;
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getDescription().getFullName(), ex );
			}
		}
	}
	
	public void unregisterEvents( EventCreator creator )
	{
		HandlerList.unregisterAll( creator );
	}
	
	public void unregisterEvents( Listener listener )
	{
		HandlerList.unregisterAll( listener );
	}
	
	public void registerEvents( Listener listener, EventCreator creator )
	{
		if ( !creator.isEnabled() )
		{
			throw new IllegalCreatorAccessException( "Creator attempted to register " + listener + " while not enabled" );
		}
		
		for ( Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : createRegisteredListeners( listener, creator ).entrySet() )
		{
			getEventListeners( getRegistrationClass( entry.getKey() ) ).registerAll( entry.getValue() );
		}
	}
	
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, EventCreator creator )
	{
		registerEvent( event, listener, priority, executor, creator, false );
	}
	
	/**
	 * Registers the given event to the specified listener using a directly passed EventExecutor
	 * 
	 * @param event
	 *             Event class to register
	 * @param listener
	 *             Listener to register
	 * @param priority
	 *             Priority of this event
	 * @param executor
	 *             EventExecutor to register
	 * @param creator
	 *             Creator to register
	 * @param ignoreCancelled
	 *             Do not call executor if event was already cancelled
	 */
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, EventCreator creator, boolean ignoreCancelled )
	{
		Validate.notNull( listener, "Listener cannot be null" );
		Validate.notNull( priority, "Priority cannot be null" );
		Validate.notNull( executor, "Executor cannot be null" );
		Validate.notNull( creator, "Creator cannot be null" );
		
		if ( !creator.isEnabled() )
		{
			throw new IllegalCreatorAccessException( "Creator attempted to register " + event + " while not enabled" );
		}
		
		if ( useTimings )
		{
			getEventListeners( event ).register( new TimedRegisteredListener( listener, executor, priority, creator, ignoreCancelled ) );
		}
		else
		{
			getEventListeners( event ).register( new RegisteredListener( listener, executor, priority, creator, ignoreCancelled ) );
		}
	}
	
	private HandlerList getEventListeners( Class<? extends Event> type )
	{
		try
		{
			Method method = getRegistrationClass( type ).getDeclaredMethod( "getHandlerList" );
			method.setAccessible( true );
			return (HandlerList) method.invoke( null );
		}
		catch ( Exception e )
		{
			throw new IllegalCreatorAccessException( e.toString() );
		}
	}
	
	private Class<? extends Event> getRegistrationClass( Class<? extends Event> clazz )
	{
		try
		{
			clazz.getDeclaredMethod( "getHandlerList" );
			return clazz;
		}
		catch ( NoSuchMethodException e )
		{
			if ( clazz.getSuperclass() != null && !clazz.getSuperclass().equals( Event.class ) && Event.class.isAssignableFrom( clazz.getSuperclass() ) )
			{
				return getRegistrationClass( clazz.getSuperclass().asSubclass( Event.class ) );
			}
			else
			{
				Loader.getLogger().warning( "Unable to find handler list for event " + clazz.getName() );
				return Event.class;
			}
		}
	}
	
	/**
	 * Sets whether or not per event timing code should be used
	 * 
	 * @param use
	 *             True if per event timing code should be used
	 */
	public void useTimings( boolean use )
	{
		useTimings = use;
	}
	
	public boolean useTimings()
	{
		return useTimings;
	}
	
	public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners( Listener listener, final EventCreator plugin )
	{
		Validate.notNull( plugin, "Creator can not be null" );
		Validate.notNull( listener, "Listener can not be null" );
		
		Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
		Set<Method> methods;
		try
		{
			Method[] publicMethods = listener.getClass().getMethods();
			methods = new HashSet<Method>( publicMethods.length, Float.MAX_VALUE );
			for ( Method method : publicMethods )
			{
				methods.add( method );
			}
			for ( Method method : listener.getClass().getDeclaredMethods() )
			{
				methods.add( method );
			}
		}
		catch ( NoClassDefFoundError e )
		{
			Loader.getLogger().severe( "Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist." );
			return ret;
		}
		
		for ( final Method method : methods )
		{
			final EventHandler eh = method.getAnnotation( EventHandler.class );
			if ( eh == null )
				continue;
			final Class<?> checkClass;
			if ( method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom( checkClass = method.getParameterTypes()[0] ) )
			{
				Loader.getLogger().severe( plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass() );
				continue;
			}
			final Class<? extends Event> eventClass = checkClass.asSubclass( Event.class );
			method.setAccessible( true );
			Set<RegisteredListener> eventSet = ret.get( eventClass );
			if ( eventSet == null )
			{
				eventSet = new HashSet<RegisteredListener>();
				ret.put( eventClass, eventSet );
			}
			
			for ( Class<?> clazz = eventClass; Event.class.isAssignableFrom( clazz ); clazz = clazz.getSuperclass() )
			{
				// This loop checks for extending deprecated events
				if ( clazz.getAnnotation( Deprecated.class ) != null )
				{
					Warning warning = clazz.getAnnotation( Warning.class );
					WarningState warningState = Loader.getInstance().getWarningState();
					if ( !warningState.printFor( warning ) )
					{
						break;
					}
					Loader.getLogger().log( Level.WARNING, String.format( "\"%s\" has registered a listener for %s on method \"%s\", but the event is Deprecated." + " \"%s\"; please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), ( warning != null && warning.reason().length() != 0 ) ? warning.reason() : "Server performance will be affected", Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ), warningState == WarningState.ON ? new AuthorNagException( null ) : null );
					break;
				}
			}
			
			EventExecutor executor = new EventExecutor()
			{
				public void execute( Listener listener, Event event ) throws EventException
				{
					try
					{
						if ( !eventClass.isAssignableFrom( event.getClass() ) )
						{
							return;
						}
						method.invoke( listener, event );
					}
					catch ( InvocationTargetException ex )
					{
						throw new EventException( ex.getCause() );
					}
					catch ( Throwable t )
					{
						throw new EventException( t );
					}
				}
			};
			if ( useTimings )
			{
				eventSet.add( new TimedRegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			}
			else
			{
				eventSet.add( new RegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			}
		}
		return ret;
	}
}
