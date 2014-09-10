package ru.tehkode.permissions.bukkit;

import com.chiorichan.Loader;

/**
 * Interface to get versioned obfuscation of CraftLoader classes
 */
public class ServerInterface
{
	private static final String CRAFTBUKKIT_PREFIX = "com.chiorichan";
	private static final String VERSION;
	
	static
	{
		Class serverClass = Loader.getInstance().getClass();
		if ( !serverClass.getSimpleName().equals( "Loader" ) )
		{
			VERSION = null;
		}
		else if ( serverClass.getName().equals( "com.chiorichan.Loader" ) )
		{
			VERSION = ".";
		}
		else
		{
			String name = serverClass.getName();
			name = name.substring( "com.chiorichan".length() );
			name = name.substring( 0, name.length() - "Loader".length() );
			VERSION = name;
		}
	}
	
	private ServerInterface()
	{
	}
	
	/**
	 * Get the versioned class name from a class name without the o.b.c prefix.
	 * 
	 * @param simpleName The name of the class without the "org.bukkit.craftbukkit" prefix
	 * @return The versioned class name, or {@code null} if not CraftLoader.
	 */
	public static String getCBClassName( String simpleName )
	{
		if ( VERSION == null )
		{
			return null;
		}
		
		return CRAFTBUKKIT_PREFIX + VERSION + simpleName;
	}
	
	/**
	 * Get the class from the name returned by passing {@code name} into {@link #getCBClassName(String)}
	 * 
	 * @param name The name of the class without the "org.bukkit.craftbukkit" prefix
	 * @return The versioned class, or {@code null} if not CraftLoader
	 */
	public static Class getCBClass( String name )
	{
		if ( VERSION == null )
		{
			return null;
		}
		
		try
		{
			return Class.forName( getCBClassName( name ) );
		}
		catch ( ClassNotFoundException e )
		{
			return null;
		}
	}
}