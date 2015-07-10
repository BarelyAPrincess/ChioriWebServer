/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
import com.chiorichan.ServerManager;
import com.chiorichan.lang.DeprecatedDetail;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.AuthorNagException;

public class EventBus implements ServerManager
{
	public static final EventBus INSTANCE = new EventBus();
	private static boolean isInitialized = false;
	
	private boolean useTimings = false;
	
	private EventBus()
	{
		
	}
	
	public static void init( boolean useTimings )
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Event Bus has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0( useTimings );
		
		isInitialized = true;
	}
	
	/**
	 * Calls an event with the given details.<br>
	 * This method only synchronizes when the event is not asynchronous.
	 * 
	 * @param event
	 *            Event details
	 */
	public void callEvent( Event event )
	{
		try
		{
			if ( event.isAsynchronous() )
			{
				if ( Thread.holdsLock( this ) )
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
				if ( Loader.getConsole().isPrimaryThread() )
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
				fireEvent( event );
			}
			else
				synchronized ( this )
				{
					fireEvent( event );
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
	 *            Event details
	 * @throws EventException
	 */
	public void callEventWithException( Event event ) throws EventException
	{
		if ( event.isAsynchronous() )
		{
			if ( Thread.holdsLock( this ) )
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
			if ( Loader.getConsole().isPrimaryThread() )
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
			fireEvent( event );
		}
		else
			synchronized ( this )
			{
				fireEvent( event );
			}
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
				methods.add( method );
			for ( Method method : listener.getClass().getDeclaredMethods() )
				methods.add( method );
		}
		catch ( NoClassDefFoundError e )
		{
			Loader.getLogger().severe( String.format( "Plugin %s has failed to register events for %s because %s does not exist.", plugin.getDescription().getFullName(), listener.getClass(), e.getMessage() ) );
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
			
			if ( ErrorReporting.E_DEPRECATED.isEnabledLevel() )
				for ( Class<?> clazz = eventClass; Event.class.isAssignableFrom( clazz ); clazz = clazz.getSuperclass() )
				{
					if ( clazz.isAnnotationPresent( DeprecatedDetail.class ) )
					{
						DeprecatedDetail deprecated = clazz.getAnnotation( DeprecatedDetail.class );
						
						PluginManager.getLogger().warning( String.format( "The plugin '%s' has registered a listener for %s on method '%s', but the event is Deprecated for reason '%s'; please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), deprecated.reason(), Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ) );
						break;
					}
					
					if ( clazz.isAnnotationPresent( Deprecated.class ) )
					{
						PluginManager.getLogger().warning( String.format( "The plugin '%s' has registered a listener for %s on method '%s', but the event is Deprecated! Please notify the authors %s.", plugin.getDescription().getFullName(), clazz.getName(), method.toGenericString(), Arrays.toString( plugin.getDescription().getAuthors().toArray() ) ) );
						break;
					}
				}
			
			EventExecutor executor = new EventExecutor()
			{
				@Override
				public void execute( Listener listener, Event event ) throws EventException
				{
					try
					{
						if ( !eventClass.isAssignableFrom( event.getClass() ) )
							return;
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
				eventSet.add( new TimedRegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
			else
				eventSet.add( new RegisteredListener( listener, executor, eh.priority(), plugin, eh.ignoreCancelled() ) );
		}
		return ret;
	}
	
	private void fireEvent( Event event ) throws EventException
	{
		HandlerList handlers = event.getHandlers();
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		
		for ( RegisteredListener registration : listeners )
		{
			if ( !registration.getCreator().isEnabled() )
				continue;
			
			try
			{
				registration.callEvent( event );
			}
			catch ( AuthorNagException ex )
			{
				if ( registration.getCreator() instanceof PluginBase )
				{
					PluginBase plugin = ( PluginBase ) registration.getCreator();
					
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
				{
					ex.printStackTrace();
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getName() + "\nEvent Exception Reason: " + ex.getMessage() );
				}
				else
				{
					ex.getCause().printStackTrace();
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getName() + "\nEvent Exception Reason: " + ex.getCause().getMessage() );
				}
				throw ex;
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getCreator().getName(), ex );
			}
		}
		
		if ( event instanceof SelfHandling )
			( ( SelfHandling ) event ).handle();
	}
	
	private HandlerList getEventListeners( Class<? extends Event> type )
	{
		try
		{
			Method method = getRegistrationClass( type ).getDeclaredMethod( "getHandlerList" );
			method.setAccessible( true );
			return ( HandlerList ) method.invoke( null );
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
				return getRegistrationClass( clazz.getSuperclass().asSubclass( Event.class ) );
			else
			{
				Loader.getLogger().warning( "Unable to find handler list for event " + clazz.getName() );
				return Event.class;
			}
		}
	}
	
	private void init0( boolean useTimings )
	{
		this.useTimings = useTimings;
	}
	
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, EventCreator creator )
	{
		registerEvent( event, listener, priority, executor, creator, false );
	}
	
	/**
	 * Registers the given event to the specified listener using a directly passed EventExecutor
	 * 
	 * @param event
	 *            Event class to register
	 * @param listener
	 *            Listener to register
	 * @param priority
	 *            Priority of this event
	 * @param executor
	 *            EventExecutor to register
	 * @param creator
	 *            Creator to register
	 * @param ignoreCancelled
	 *            Do not call executor if event was already cancelled
	 */
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, EventCreator creator, boolean ignoreCancelled )
	{
		Validate.notNull( listener, "Listener cannot be null" );
		Validate.notNull( priority, "Priority cannot be null" );
		Validate.notNull( executor, "Executor cannot be null" );
		Validate.notNull( creator, "Creator cannot be null" );
		
		if ( !creator.isEnabled() )
			throw new IllegalCreatorAccessException( "Creator attempted to register " + event + " while not enabled" );
		
		if ( useTimings )
			getEventListeners( event ).register( new TimedRegisteredListener( listener, executor, priority, creator, ignoreCancelled ) );
		else
			getEventListeners( event ).register( new RegisteredListener( listener, executor, priority, creator, ignoreCancelled ) );
	}
	
	public void registerEvents( Listener listener, EventCreator creator )
	{
		if ( !creator.isEnabled() )
			throw new IllegalCreatorAccessException( "Creator attempted to register " + listener + " while not enabled" );
		
		for ( Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : createRegisteredListeners( listener, creator ).entrySet() )
			getEventListeners( getRegistrationClass( entry.getKey() ) ).registerAll( entry.getValue() );
	}
	
	public void unregisterEvents( EventCreator creator )
	{
		HandlerList.unregisterAll( creator );
	}
	
	public void unregisterEvents( Listener listener )
	{
		HandlerList.unregisterAll( listener );
	}
	
	public boolean useTimings()
	{
		return useTimings;
	}
	
	/**
	 * Sets whether or not per event timing code should be used
	 * 
	 * @param use
	 *            True if per event timing code should be used
	 */
	public void useTimings( boolean use )
	{
		useTimings = use;
	}
}
