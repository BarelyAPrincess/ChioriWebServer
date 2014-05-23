package com.chiorichan.event;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.plugin.AuthorNagException;
import com.chiorichan.plugin.EventExecutor;
import com.chiorichan.plugin.IllegalPluginAccessException;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.RegisteredListener;
import com.chiorichan.plugin.TimedRegisteredListener;

public class EventBus
{
	private final Loader server;
	private boolean useTimings = false;
	
	public EventBus(Loader _server)
	{
		server = _server;
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
				if ( server.isPrimaryThread() )
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
			if ( server.isPrimaryThread() )
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
			if ( !registration.getPlugin().isEnabled() )
			{
				continue;
			}
			
			try
			{
				registration.callEvent( event );
			}
			catch ( AuthorNagException ex )
			{
				Plugin plugin = registration.getPlugin();
				
				if ( plugin.isNaggable() )
				{
					plugin.setNaggable( false );
					
					Loader.getLogger().log( Level.SEVERE, String.format( "Nag author(s): '%s' of '%s' about the following: %s", plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(), ex.getMessage() ) );
				}
			}
			catch ( EventException ex )
			{
				if ( ex.getCause() == null )
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getDescription().getFullName() + "\nEvent Exception Reason: " + ex.getMessage() );
				else
					Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getDescription().getFullName() + "\nEvent Exception Reason: " + ex.getCause().getMessage() );
				throw ex;
			}
			catch ( Throwable ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getDescription().getFullName(), ex );
			}
		}
	}
	
	public void registerEvents( Listener listener, Plugin plugin )
	{
		if ( !plugin.isEnabled() )
		{
			throw new IllegalPluginAccessException( "Plugin attempted to register " + listener + " while not enabled" );
		}
		
		for ( Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners( listener, plugin ).entrySet() )
		{
			getEventListeners( getRegistrationClass( entry.getKey() ) ).registerAll( entry.getValue() );
		}
	}
	
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin )
	{
		registerEvent( event, listener, priority, executor, plugin, false );
	}
	
	/**
	 * Registers the given event to the specified listener using a directly passed EventExecutor
	 * 
	 * @param event
	 *             Event class to register
	 * @param listener
	 *             PlayerListener to register
	 * @param priority
	 *             Priority of this event
	 * @param executor
	 *             EventExecutor to register
	 * @param plugin
	 *             Plugin to register
	 * @param ignoreCancelled
	 *             Do not call executor if event was already cancelled
	 */
	public void registerEvent( Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled )
	{
		Validate.notNull( listener, "Listener cannot be null" );
		Validate.notNull( priority, "Priority cannot be null" );
		Validate.notNull( executor, "Executor cannot be null" );
		Validate.notNull( plugin, "Plugin cannot be null" );
		
		if ( !plugin.isEnabled() )
		{
			throw new IllegalPluginAccessException( "Plugin attempted to register " + event + " while not enabled" );
		}
		
		if ( useTimings )
		{
			getEventListeners( event ).register( new TimedRegisteredListener( listener, executor, priority, plugin, ignoreCancelled ) );
		}
		else
		{
			getEventListeners( event ).register( new RegisteredListener( listener, executor, priority, plugin, ignoreCancelled ) );
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
			throw new IllegalPluginAccessException( e.toString() );
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
				
				// throw new IllegalPluginAccessException( "Unable to find handler list for event " + clazz.getName() );
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
}
