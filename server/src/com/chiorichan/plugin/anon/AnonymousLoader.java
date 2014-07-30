package com.chiorichan.plugin.anon;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.Warning;
import com.chiorichan.Warning.WarningState;
import com.chiorichan.bus.bases.EventException;
import com.chiorichan.bus.events.Event;
import com.chiorichan.bus.events.EventHandler;
import com.chiorichan.bus.events.Listener;
import com.chiorichan.plugin.AuthorNagException;
import com.chiorichan.plugin.EventExecutor;
import com.chiorichan.plugin.InvalidDescriptionException;
import com.chiorichan.plugin.InvalidPluginException;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.PluginLoader;
import com.chiorichan.plugin.RegisteredListener;
import com.chiorichan.plugin.TimedRegisteredListener;
import com.chiorichan.plugin.UnknownDependencyException;

public class AnonymousLoader implements PluginLoader
{
	@Override
	public Plugin loadPlugin( File file ) throws InvalidPluginException, UnknownDependencyException
	{
		throw new InvalidPluginException( "The `loadPlugin` should NEVER be called since it's not possible to load modules with the AnonymousLoader." );
	}
	
	@Override
	public PluginDescriptionFile getPluginDescription( File file ) throws InvalidDescriptionException
	{
		throw new InvalidDescriptionException( "The `getPluginDescription` should NEVER be called since it's not possible to load modules with the AnonymousLoader." );
	}
	
	@Override
	public Pattern[] getPluginFileFilters()
	{
		return new Pattern[] {};
	}
	
	@Override
	public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners( Listener listener, Plugin plugin )
	{
		Validate.notNull( plugin, "Plugin can not be null" );
		Validate.notNull( listener, "Listener can not be null" );
		
		boolean useTimings = Loader.getEventBus().useTimings();
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
	
	@Override
	public void enablePlugin( Plugin plugin )
	{
		Loader.getLogger().warning( "AnonymousLoader can't enable or disable plugins. Plugin: " + plugin.getDescription().getFullName() );
	}
	
	@Override
	public void disablePlugin( Plugin plugin )
	{
		Loader.getLogger().warning( "AnonymousLoader can't enable or disable plugins. Plugin: " + plugin.getDescription().getFullName() );
	}
}
