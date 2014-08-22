package ru.tehkode.permissions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import ru.tehkode.permissions.events.PermissionEntityEvent;

import com.chiorichan.Loader;

/**
 * @author code
 */
public abstract class PermissionEntity
{
	
	protected PermissionManager manager;
	private String name;
	protected boolean virtual = true;
	protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<String, List<String>>();
	protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<String, Long>();
	protected boolean debugMode = false;
	
	public PermissionEntity(String name, PermissionManager manager)
	{
		this.manager = manager;
		this.name = name;
	}
	
	/**
	 * This method 100% run after all constructors have been run and entity
	 * object, and entity object are completely ready to operate
	 */
	public void initialize()
	{
		this.debugMode = this.getOptionBoolean( "debug", null, this.debugMode );
	}
	
	/**
	 * Return name of permission entity (User or Group)
	 * User should be equal to User's name on the server
	 * 
	 * @return name
	 */
	public String getName()
	{
		return this.name;
	}
	
	protected void setName( String name )
	{
		this.name = name;
	}
	
	/**
	 * Returns entity prefix
	 * 
	 * @param siteName
	 * @return prefix
	 */
	public abstract String getPrefix( String siteName );
	
	public String getPrefix()
	{
		return this.getPrefix( null );
	}
	
	/**
	 * Returns entity prefix
	 * 
	 */
	/**
	 * Set prefix to value
	 * 
	 * @param prefix new prefix
	 */
	public abstract void setPrefix( String prefix, String siteName );
	
	/**
	 * Return entity suffix
	 * 
	 * @return suffix
	 */
	public abstract String getSuffix( String siteName );
	
	public String getSuffix()
	{
		return getSuffix( null );
	}
	
	/**
	 * Set suffix to value
	 * 
	 * @param suffix new suffix
	 */
	public abstract void setSuffix( String suffix, String siteName );
	
	/**
	 * Checks if entity has specified permission in default site
	 * 
	 * @param permission Permission to check
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has( String permission )
	{
		return this.has( permission, Loader.getSiteManager().getSites().get( 0 ).getName() );
	}
	
	/**
	 * Check if entity has specified permission in site
	 * 
	 * @param permission Permission to check
	 * @param site Site to check permission in
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has( String permission, String site )
	{
		if ( permission != null && permission.isEmpty() )
		{ // empty permission for public access :)
			return true;
		}
		
		String expression = getMatchingExpression( permission, site );
		
		if ( this.isDebug() )
		{
			Logger.getLogger( "" ).info( "User " + this.getName() + " checked for \"" + permission + "\", " + ( expression == null ? "no permission found" : "\"" + expression + "\" found" ) );
		}
		
		return explainExpression( expression );
	}
	
	/**
	 * Return all entity permissions in specified site
	 * 
	 * @param site Site name
	 * @return Array of permission expressions
	 */
	public abstract String[] getPermissions( String site );
	
	/**
	 * Return permissions for all sites
	 * Common permissions stored as "" (empty string) as site.
	 * 
	 * @return Map with site name as key and permissions array as value
	 */
	public abstract Map<String, String[]> getAllPermissions();
	
	/**
	 * Add permissions for specified site
	 * 
	 * @param permission Permission to add
	 * @param site Site name to add permission to
	 */
	public void addPermission( String permission, String site )
	{
		throw new UnsupportedOperationException( "You shouldn't call this method" );
	}
	
	/**
	 * Add permission in common space (all sites)
	 * 
	 * @param permission Permission to add
	 */
	public void addPermission( String permission )
	{
		this.addPermission( permission, "" );
	}
	
	/**
	 * Remove permission in site
	 * 
	 * @param permission Permission to remove
	 * @param site Site name to remove permission for
	 */
	public void removePermission( String permission, String siteName )
	{
		throw new UnsupportedOperationException( "You shouldn't call this method" );
	}
	
	/**
	 * Remove specified permission from all sites
	 * 
	 * @param permission Permission to remove
	 */
	public void removePermission( String permission )
	{
		for ( String site : this.getAllPermissions().keySet() )
		{
			this.removePermission( permission, site );
		}
	}
	
	/**
	 * Set permissions in site
	 * 
	 * @param permissions Array of permissions to set
	 * @param site Site to set permissions for
	 */
	public abstract void setPermissions( String[] permissions, String site );
	
	/**
	 * Set specified permissions in common space (all site)
	 * 
	 * @param permission Permission to set
	 */
	public void setPermissions( String[] permission )
	{
		this.setPermissions( permission, "" );
	}
	
	/**
	 * Get option in site
	 * 
	 * @param option Name of option
	 * @param site Site to look for
	 * @param defaultValue Default value to fallback if no such option was found
	 * @return Value of option as String
	 */
	public abstract String getOption( String option, String site, String defaultValue );
	
	/**
	 * Return option
	 * Option would be looked up in common options
	 * 
	 * @param option Option name
	 * @return option value or empty string if option was not found
	 */
	public String getOption( String option )
	{
		// @todo Replace empty string with null
		return this.getOption( option, "", "" );
	}
	
	/**
	 * Return option for site
	 * 
	 * @param option Option name
	 * @param site Site to look in
	 * @return option value or empty string if option was not found
	 */
	public String getOption( String option, String site )
	{
		// @todo Replace empty string with null
		return this.getOption( option, site, "" );
	}
	
	/**
	 * Return integer value for option
	 * 
	 * @param optionName
	 * @param site
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not integer
	 */
	public int getOptionInteger( String optionName, String site, int defaultValue )
	{
		try
		{
			return Integer.parseInt( this.getOption( optionName, site, Integer.toString( defaultValue ) ) );
		}
		catch ( NumberFormatException e )
		{}
		
		return defaultValue;
	}
	
	/**
	 * Returns double value for option
	 * 
	 * @param optionName
	 * @param site
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not double
	 */
	public double getOptionDouble( String optionName, String site, double defaultValue )
	{
		String option = this.getOption( optionName, site, Double.toString( defaultValue ) );
		
		try
		{
			return Double.parseDouble( option );
		}
		catch ( NumberFormatException e )
		{}
		
		return defaultValue;
	}
	
	/**
	 * Returns boolean value for option
	 * 
	 * @param optionName
	 * @param site
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not boolean
	 */
	public boolean getOptionBoolean( String optionName, String site, boolean defaultValue )
	{
		String option = this.getOption( optionName, site, Boolean.toString( defaultValue ) );
		
		if ( "false".equalsIgnoreCase( option ) )
		{
			return false;
		}
		else if ( "true".equalsIgnoreCase( option ) )
		{
			return true;
		}
		
		return defaultValue;
	}
	
	/**
	 * Set specified option in site
	 * 
	 * @param option Option name
	 * @param value Value to set, null to remove
	 * @param site Site name
	 */
	public abstract void setOption( String option, String value, String site );
	
	/**
	 * Set option for all sites. Can be overwritten by site specific option
	 * 
	 * @param option Option name
	 * @param value Value to set, null to remove
	 */
	public void setOption( String permission, String value )
	{
		this.setOption( permission, value, "" );
	}
	
	/**
	 * Get options in site
	 * 
	 * @param site
	 * @return Option value as string Map
	 */
	public abstract Map<String, String> getOptions( String site );
	
	/**
	 * Return options for all sites
	 * Common options stored as "" (empty string) as site.
	 * 
	 * @return Map with site name as key and map of options as value
	 */
	public abstract Map<String, Map<String, String>> getAllOptions();
	
	/**
	 * Save in-memory data to storage backend
	 */
	public abstract void save();
	
	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();
	
	/**
	 * Return state of entity
	 * 
	 * @return true if entity is only in-memory
	 */
	public boolean isVirtual()
	{
		return this.virtual;
	}
	
	/**
	 * Return site names where entity have permissions/options/etc
	 * 
	 * @return
	 */
	public abstract String[] getSites();
	
	/**
	 * Return entity timed (temporary) permission for site
	 * 
	 * @param site
	 * @return Array of timed permissions in that site
	 */
	public String[] getTimedPermissions( String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			return new String[0];
		}
		
		return this.timedPermissions.get( site ).toArray( new String[0] );
	}
	
	/**
	 * Returns remaining lifetime of specified permission in site
	 * 
	 * @param permission Name of permission
	 * @param site
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime( String permission, String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissionsTime.containsKey( site + ":" + permission ) )
		{
			return 0;
		}
		
		return (int) ( this.timedPermissionsTime.get( site + ":" + permission ).longValue() - ( System.currentTimeMillis() / 1000L ) );
	}
	
	/**
	 * Adds timed permission to specified site in seconds
	 * 
	 * @param permission
	 * @param site
	 * @param lifeTime Lifetime of permission in seconds. 0 for transient permission (site disappear only after server reload)
	 */
	public void addTimedPermission( final String permission, String site, int lifeTime )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			this.timedPermissions.put( site, new LinkedList<String>() );
		}
		
		this.timedPermissions.get( site ).add( permission );
		
		final String finalSite = site;
		
		if ( lifeTime > 0 )
		{
			TimerTask task = new TimerTask()
			{
				
				@Override
				public void run()
				{
					removeTimedPermission( permission, finalSite );
				}
			};
			
			this.manager.registerTask( task, lifeTime );
			
			this.timedPermissionsTime.put( site + ":" + permission, ( System.currentTimeMillis() / 1000L ) + lifeTime );
		}
		
		this.callEvent( PermissionEntityEvent.Action.PERMISSIONS_CHANGED );
	}
	
	/**
	 * Removes specified timed permission for site
	 * 
	 * @param permission
	 * @param site
	 */
	public void removeTimedPermission( String permission, String site )
	{
		if ( site == null )
		{
			site = "";
		}
		
		if ( !this.timedPermissions.containsKey( site ) )
		{
			return;
		}
		
		this.timedPermissions.get( site ).remove( permission );
		this.timedPermissions.remove( site + ":" + permission );
		
		this.callEvent( PermissionEntityEvent.Action.PERMISSIONS_CHANGED );
	}
	
	protected void callEvent( PermissionEntityEvent event )
	{
		manager.callEvent( event );
	}
	
	protected void callEvent( PermissionEntityEvent.Action action )
	{
		this.callEvent( new PermissionEntityEvent( this, action ) );
	}
	
	@Override
	public boolean equals( Object obj )
	{
		if ( obj == null )
		{
			return false;
		}
		if ( !getClass().equals( obj.getClass() ) )
		{
			return false;
		}
		
		if ( this == obj )
		{
			return true;
		}
		
		final PermissionEntity other = (PermissionEntity) obj;
		return this.name.equals( other.name );
	}
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 89 * hash + ( this.name != null ? this.name.hashCode() : 0 );
		return hash;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + this.getName() + ")";
	}
	
	public String getMatchingExpression( String permission, String site )
	{
		return this.getMatchingExpression( this.getPermissions( site ), permission );
	}
	
	public String getMatchingExpression( String[] permissions, String permission )
	{
		for ( String expression : permissions )
		{
			if ( isMatches( expression, permission, true ) )
			{
				return expression;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if specified permission matches specified permission expression
	 * 
	 * @param expression permission expression - what user have in his permissions list (permission.nodes.*)
	 * @param permission permission which are checking for (permission.node.some.subnode)
	 * @param additionalChecks check for parent node matching
	 * @return
	 */
	public boolean isMatches( String expression, String permission, boolean additionalChecks )
	{
		return this.manager.getPermissionMatcher().isMatches( expression, permission );
	}
	
	public boolean explainExpression( String expression )
	{
		if ( expression == null || expression.isEmpty() )
		{
			return false;
		}
		
		return !expression.startsWith( "-" ); // If expression have - (minus) before then that mean expression are negative
	}
	
	public boolean isDebug()
	{
		return this.debugMode || this.manager.isDebug();
	}
	
	public void setDebug( boolean debug )
	{
		this.debugMode = debug;
	}
}
