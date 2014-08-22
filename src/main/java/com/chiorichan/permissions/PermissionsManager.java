package com.chiorichan.permissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import com.chiorichan.Loader;
import com.google.common.collect.ImmutableSet;

public class PermissionsManager
{
	private final Map<String, Permission> permissions = new HashMap<String, Permission>();
	private final Map<Boolean, Set<Permission>> defaultPerms = new LinkedHashMap<Boolean, Set<Permission>>();
	private final Map<String, Map<Permissible, Boolean>> permSubs = new HashMap<String, Map<Permissible, Boolean>>();
	private final Map<Boolean, Map<Permissible, Boolean>> defSubs = new HashMap<Boolean, Map<Permissible, Boolean>>();
	
	private final Yaml yaml = new Yaml( new SafeConstructor() );
	private static PermissionsManager instance;
	
	public PermissionsManager()
	{
		instance = this;
		
		defaultPerms.put( true, new HashSet<Permission>() );
		defaultPerms.put( false, new HashSet<Permission>() );
	}
	
	public static PermissionsManager getInstance()
	{
		return instance;
	}
	
	@SuppressWarnings( { "unchecked", "finally" } )
	public void loadCustomPermissions()
	{
		File file = new File( Loader.getConfig().getString( "settings.permissions-file" ) );
		FileInputStream stream;
		
		try
		{
			stream = new FileInputStream( file );
		}
		catch ( FileNotFoundException ex )
		{
			try
			{
				file.createNewFile();
			}
			finally
			{
				return;
			}
		}
		
		Map<String, Map<String, Object>> perms;
		
		try
		{
			perms = (Map<String, Map<String, Object>>) yaml.load( stream );
		}
		catch ( MarkedYAMLException ex )
		{
			Loader.getLogger().log( Level.WARNING, "Server permissions file " + file + " is not valid YAML: " + ex.toString() );
			return;
		}
		catch ( Throwable ex )
		{
			Loader.getLogger().log( Level.WARNING, "Server permissions file " + file + " is not valid YAML.", ex );
			return;
		}
		finally
		{
			try
			{
				stream.close();
			}
			catch ( IOException ex )
			{}
		}
		
		if ( perms == null )
		{
			Loader.getLogger().log( Level.INFO, "Server permissions file " + file + " is empty, ignoring it" );
			return;
		}
		
		List<Permission> permsList = Permission.loadPermissions( perms, "Permission node '%s' in " + file + " is invalid", Permission.DEFAULT_PERMISSION );
		
		for ( Permission perm : permsList )
		{
			try
			{
				addPermission( perm );
			}
			catch ( IllegalArgumentException ex )
			{
				Loader.getLogger().log( Level.SEVERE, "Permission in " + file + " was already defined", ex );
			}
		}
	}
	
	public void clearPermissions()
	{
		synchronized ( this )
		{
			permissions.clear();
			defaultPerms.get( true ).clear();
			defaultPerms.get( false ).clear();
		}
	}
	
	public Permission getPermission( String name )
	{
		return permissions.get( name.toLowerCase() );
	}
	
	public void addPermission( Permission perm )
	{
		String name = perm.getName().toLowerCase();
		
		if ( permissions.containsKey( name ) )
		{
			throw new IllegalArgumentException( "The permission " + name + " is already defined!" );
		}
		
		permissions.put( name, perm );
		calculatePermissionDefault( perm );
	}
	
	public Set<Permission> getDefaultPermissions( boolean op )
	{
		return ImmutableSet.copyOf( defaultPerms.get( op ) );
	}
	
	public void removePermission( Permission perm )
	{
		removePermission( perm.getName() );
	}
	
	public void removePermission( String name )
	{
		permissions.remove( name.toLowerCase() );
	}
	
	public void recalculatePermissionDefaults( Permission perm )
	{
		if ( permissions.containsValue( perm ) )
		{
			defaultPerms.get( true ).remove( perm );
			defaultPerms.get( false ).remove( perm );
			
			calculatePermissionDefault( perm );
		}
	}
	
	private void calculatePermissionDefault( Permission perm )
	{
		if ( ( perm.getDefault() == PermissionDefault.OP ) || ( perm.getDefault() == PermissionDefault.TRUE ) )
		{
			defaultPerms.get( true ).add( perm );
			dirtyPermissibles( true );
		}
		if ( ( perm.getDefault() == PermissionDefault.NOT_OP ) || ( perm.getDefault() == PermissionDefault.TRUE ) )
		{
			defaultPerms.get( false ).add( perm );
			dirtyPermissibles( false );
		}
	}
	
	private void dirtyPermissibles( boolean op )
	{
		Set<Permissible> permissibles = getDefaultPermSubscriptions( op );
		
		for ( Permissible p : permissibles )
		{
			p.recalculatePermissions();
		}
	}
	
	public void subscribeToPermission( String permission, Permissible permissible )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map == null )
		{
			map = new WeakHashMap<Permissible, Boolean>();
			permSubs.put( name, map );
		}
		
		map.put( permissible, true );
	}
	
	public void unsubscribeFromPermission( String permission, Permissible permissible )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map != null )
		{
			map.remove( permissible );
			
			if ( map.isEmpty() )
			{
				permSubs.remove( name );
			}
		}
	}
	
	public Set<Permissible> getPermissionSubscriptions( String permission )
	{
		String name = permission.toLowerCase();
		Map<Permissible, Boolean> map = permSubs.get( name );
		
		if ( map == null )
		{
			return ImmutableSet.of();
		}
		else
		{
			return ImmutableSet.copyOf( map.keySet() );
		}
	}
	
	public void subscribeToDefaultPerms( boolean op, Permissible permissible )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map == null )
		{
			map = new WeakHashMap<Permissible, Boolean>();
			defSubs.put( op, map );
		}
		
		map.put( permissible, true );
	}
	
	public void unsubscribeFromDefaultPerms( boolean op, Permissible permissible )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map != null )
		{
			map.remove( permissible );
			
			if ( map.isEmpty() )
			{
				defSubs.remove( op );
			}
		}
	}
	
	public Set<Permissible> getDefaultPermSubscriptions( boolean op )
	{
		Map<Permissible, Boolean> map = defSubs.get( op );
		
		if ( map == null )
		{
			return ImmutableSet.of();
		}
		else
		{
			return ImmutableSet.copyOf( map.keySet() );
		}
	}
	
	public Set<Permission> getPermissions()
	{
		return new HashSet<Permission>( permissions.values() );
	}
}
