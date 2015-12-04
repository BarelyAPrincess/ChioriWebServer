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
import com.chiorichan.SourceContext;
import com.chiorichan.lang.DeprecatedDetail;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.AuthorNagException;
import com.google.common.collect.Maps;

public class EventBus implements ServerManager
{
	private static Map<Class<? extends AbstractEvent>, EventHandlers> handlers = Maps.newConcurrentMap();
	
	public static final EventBus INSTANCE = new EventBus();
	private static boolean isInitialized = false;
	private boolean useTimings = false;
	private Object lock = new Object();
	
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
	public <T extends AbstractEvent> T callEvent( T event )
	{
		try
		{
			if ( event.isAsynchronous() )
			{
				if ( Thread.holdsLock( lock ) )
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
				if ( Loader.getServerBus().isPrimaryThread() )
					throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
				fireEvent( event );
			}
			else
				synchronized ( lock )
				{
					fireEvent( event );
				}
		}
		catch ( EventException ex )
		{
			
		}
		
		return event;
	}
	
	/**
	 * Calls an event with the given details.<br>
	 * This method only synchronizes when the event is not asynchronous.
	 * 
	 * @param event
	 *            Event details
	 * @throws EventException
	 */
	public <T extends AbstractEvent> T callEventWithException( T event ) throws EventException
	{
		if ( event.isAsynchronous() )
		{
			if ( Thread.holdsLock( lock ) )
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from inside synchronized code." );
			if ( Loader.getServerBus().isPrimaryThread() )
				throw new IllegalStateException( event.getEventName() + " cannot be triggered asynchronously from primary server thread." );
			fireEvent( event );
		}
		else
			synchronized ( lock )
			{
				fireEvent( event );
			}
		
		return event;
	}
	
	public Map<Class<? extends AbstractEvent>, Set<RegisteredListener>> createRegisteredListeners( Listener listener, final SourceContext context )
	{
		Validate.notNull( context, "Context can not be null" );
		Validate.notNull( listener, "Listener can not be null" );
		
		Map<Class<? extends AbstractEvent>, Set<RegisteredListener>> ret = new HashMap<Class<? extends AbstractEvent>, Set<RegisteredListener>>();
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
			Loader.getLogger().severe( String.format( "Plugin %s has failed to register events for %s because %s does not exist.", context.getFullName(), listener.getClass(), e.getMessage() ) );
			return ret;
		}
		
		for ( final Method method : methods )
		{
			final EventHandler eh = method.getAnnotation( EventHandler.class );
			if ( eh == null )
				continue;
			final Class<?> checkClass;
			if ( method.getParameterTypes().length != 1 || !AbstractEvent.class.isAssignableFrom( checkClass = method.getParameterTypes()[0] ) )
			{
				Loader.getLogger().severe( context.getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass() );
				continue;
			}
			final Class<? extends AbstractEvent> eventClass = checkClass.asSubclass( AbstractEvent.class );
			method.setAccessible( true );
			Set<RegisteredListener> eventSet = ret.get( eventClass );
			if ( eventSet == null )
			{
				eventSet = new HashSet<RegisteredListener>();
				ret.put( eventClass, eventSet );
			}
			
			if ( ReportingLevel.E_DEPRECATED.isEnabledLevel() )
				for ( Class<?> clazz = eventClass; AbstractEvent.class.isAssignableFrom( clazz ); clazz = clazz.getSuperclass() )
				{
					if ( clazz.isAnnotationPresent( DeprecatedDetail.class ) )
					{
						DeprecatedDetail deprecated = clazz.getAnnotation( DeprecatedDetail.class );
						
						PluginManager.getLogger().warning( String.format( "The creator '%s' has registered a listener for %s on method '%s', but the event is Deprecated for reason '%s'; please notify the authors %s.", context.getFullName(), clazz.getName(), method.toGenericString(), deprecated.reason(), Arrays.toString( context.getAuthors().toArray() ) ) );
						break;
					}
					
					if ( clazz.isAnnotationPresent( Deprecated.class ) )
					{
						PluginManager.getLogger().warning( String.format( "The creator '%s' has registered a listener for %s on method '%s', but the event is Deprecated! Please notify the authors %s.", context.getFullName(), clazz.getName(), method.toGenericString(), Arrays.toString( context.getAuthors().toArray() ) ) );
						break;
					}
				}
			
			EventExecutor executor = new EventExecutor()
			{
				@Override
				public void execute( Listener listener, AbstractEvent event ) throws EventException
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
				eventSet.add( new TimedRegisteredListener( listener, executor, eh.priority(), context, eh.ignoreCancelled() ) );
			else
				eventSet.add( new RegisteredListener( listener, executor, eh.priority(), context, eh.ignoreCancelled() ) );
		}
		return ret;
	}
	
	private void fireEvent( AbstractEvent event ) throws EventException
	{
		for ( RegisteredListener registration : getEventListeners( event.getClass() ) )
		{
			if ( !registration.getContext().isEnabled() )
				continue;
			
			try
			{
				registration.callEvent( event );
			}
			catch ( AuthorNagException ex )
			{
				if ( registration.getContext().getSource() instanceof PluginBase )
				{
					PluginBase creator = ( PluginBase ) registration.getContext().getSource();
					
					if ( creator.isNaggable() )
					{
						creator.setNaggable( false );
						Loader.getLogger().log( Level.SEVERE, String.format( "Nag author(s): '%s' of '%s' about the following: %s", creator.getDescription().getAuthors(), creator.getDescription().getFullName(), ex.getMessage() ) );
					}
				}
			}
			catch ( EventException ex )
			{
				if ( ex.getCause() == null )
				{
					ex.printStackTrace();
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getContext().getName() + "\nEvent Exception Reason: " + ex.getMessage() );
				}
				else
				{
					ex.getCause().printStackTrace();
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getContext().getName() + "\nEvent Exception Reason: " + ex.getCause().getMessage() );
				}
				throw ex;
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getContext().getName(), ex );
			}
		}
		
		if ( event instanceof SelfHandling )
			( ( SelfHandling ) event ).handle();
	}
	
	private EventHandlers getEventListeners( Class<? extends AbstractEvent> event )
	{
		EventHandlers eventHandlers = handlers.get( event );
		
		if ( eventHandlers == null )
		{
			eventHandlers = new EventHandlers();
			handlers.put( event, eventHandlers );
		}
		
		return eventHandlers;
	}
	
	private void init0( boolean useTimings )
	{
		this.useTimings = useTimings;
	}
	
	public void registerEvent( Class<? extends AbstractEvent> event, Listener listener, EventPriority priority, EventExecutor executor, EventRegistrar creator )
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
	public void registerEvent( Class<? extends AbstractEvent> event, Listener listener, EventPriority priority, EventExecutor executor, Object source, boolean ignoreCancelled )
	{
		Validate.notNull( listener, "Listener cannot be null" );
		Validate.notNull( priority, "Priority cannot be null" );
		Validate.notNull( executor, "Executor cannot be null" );
		Validate.notNull( source, "Creator cannot be null" );
		
		SourceContext context = SourceContext.produce( source );
		
		if ( useTimings )
			getEventListeners( event ).register( new TimedRegisteredListener( listener, executor, priority, context, ignoreCancelled ) );
		else
			getEventListeners( event ).register( new RegisteredListener( listener, executor, priority, context, ignoreCancelled ) );
	}
	
	public void registerEvents( Listener listener, Object source )
	{
		SourceContext context = SourceContext.produce( source );
		
		for ( Map.Entry<Class<? extends AbstractEvent>, Set<RegisteredListener>> entry : createRegisteredListeners( listener, context ).entrySet() )
			getEventListeners( entry.getKey() ).registerAll( entry.getValue() );
	}
	
	public void unregisterEvents( EventRegistrar creator )
	{
		EventHandlers.unregisterAll( creator );
	}
	
	public void unregisterEvents( Listener listener )
	{
		EventHandlers.unregisterAll( listener );
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
