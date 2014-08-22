package ru.tehkode.permissions.backends;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.sql.SQLConnection;
import ru.tehkode.permissions.backends.sql.SQLEntity;
import ru.tehkode.permissions.backends.sql.SQLGroup;
import ru.tehkode.permissions.backends.sql.SQLUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.configuration.Configuration;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.framework.Site;

/**
 * @author code
 */
public class SQLBackend extends PermissionBackend
{
	
	protected Map<String, String[]> siteInheritanceCache = new HashMap<String, String[]>();
	private Map<String, Object> tableNames;
	private ThreadLocal<SQLConnection> conn;
	
	public SQLBackend(PermissionManager manager, Configuration config)
	{
		super( manager, config );
	}
	
	@Override
	public void initialize() throws PermissionBackendException
	{
		final String dbUri = config.getString( "permissions.backends.sql.uri", "" );
		final String dbUser = config.getString( "permissions.backends.sql.user", "" );
		final String dbPassword = config.getString( "permissions.backends.sql.password", "" );
		
		if ( dbUri == null || dbUri.isEmpty() )
		{
			config.set( "permissions.backends.sql.uri", "mysql://localhost/exampledb" );
			config.set( "permissions.backends.sql.user", "databaseuser" );
			config.set( "permissions.backends.sql.password", "databasepassword" );
			
			throw new PermissionBackendException( "SQL connection is not configured, see config.yml" );
		}
		
		conn = new ThreadLocal<SQLConnection>()
		{
			@Override
			public SQLConnection initialValue()
			{
				return new SQLConnection( dbUri, dbUser, dbPassword, SQLBackend.this );
			}
		};
		
		try
		{
			getSQL(); // Test connection
		}
		catch ( Exception e )
		{
			if ( e.getCause() != null && e.getCause() instanceof Exception )
			{
				e = (Exception) e.getCause();
			}
			throw new PermissionBackendException( e );
		}
		
		Logger.getLogger( "PermissionsEx" ).info( "[PermissionsEx-SQL] Successfully connected to database" );
		
		this.setupAliases( config );
		this.deployTables();
	}
	
	public SQLConnection getSQL()
	{
		return conn.get();
	}
	
	public String getTableName( String identifier )
	{
		Map<String, Object> tableNames = this.tableNames;
		if ( tableNames == null )
		{
			return identifier;
		}
		
		Object ret = tableNames.get( identifier );
		if ( ret == null )
		{
			return identifier;
		}
		return ret.toString();
	}
	
	@Override
	public PermissionUser getUser( String name )
	{
		return new SQLUser( name, manager, this );
	}
	
	@Override
	public PermissionGroup getGroup( String name )
	{
		return new SQLGroup( name, manager, this );
	}
	
	@Override
	public PermissionGroup getDefaultGroup( String siteName )
	{
		try
		{
			ResultSet result;
			
			if ( siteName == null )
			{
				result = getSQL().prepAndBind( "SELECT `name` FROM `{permissions_entity}` WHERE `type` = ? AND `default` = 1 LIMIT 1", SQLEntity.Type.GROUP.ordinal() ).executeQuery();
				
				if ( !result.next() )
				{
					throw new RuntimeException( "There is no default group set, this is a serious issue" );
				}
			}
			else
			{
				result = this.getSQL().prepAndBind( "SELECT `name` FROM `{permissions}` WHERE `permission` = 'default' AND `value` = 'true' AND `type` = ? AND `site` = ?", SQLEntity.Type.GROUP.ordinal(), siteName ).executeQuery();
				
				if ( !result.next() )
				{
					return null;
				}
			}
			
			return this.manager.getGroup( result.getString( "name" ) );
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void setDefaultGroup( PermissionGroup group, String siteName )
	{
		try
		{
			if ( siteName == null )
			{
				// Reset default flag
				this.getSQL().prepAndBind( "UPDATE `{permissions_entity}` SET `default` = 0 WHERE `type` = ? AND `default` = 1 LIMIT 1", SQLEntity.Type.GROUP.ordinal() ).execute();
				// Set default flag
				this.getSQL().prepAndBind( "UPDATE `{permissions_entity}` SET `default` = 1 WHERE `type` = ? AND `name` = ? LIMIT 1", SQLEntity.Type.GROUP.ordinal(), group.getName() ).execute();
			}
			else
			{
				this.getSQL().prepAndBind( "DELETE FROM `{permissions}` WHERE `permission` = 'default' AND `site` = ? AND `type` = ?", siteName, SQLEntity.Type.GROUP.ordinal() ).execute();
				this.getSQL().prepAndBind( "INSERT INTO `{permissions}` (`name`, `permission`, `type`, `site`, `value`) VALUES (?, 'default', ?, ?, 'true')", group.getName(), SQLEntity.Type.GROUP.ordinal(), siteName ).execute();
			}
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( "Failed to set default group", e );
		}
	}
	
	@Override
	public PermissionGroup[] getGroups()
	{
		Set<String> groupNames = SQLEntity.getEntitiesNames( getSQL(), SQLEntity.Type.GROUP, false );
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
		
		for ( String groupName : groupNames )
		{
			groups.add( this.manager.getGroup( groupName ) );
		}
		
		Collections.sort( groups );
		
		return groups.toArray( new PermissionGroup[0] );
	}
	
	@Override
	public PermissionUser[] getRegisteredUsers()
	{
		Set<String> userNames = SQLEntity.getEntitiesNames( getSQL(), SQLEntity.Type.USER, false );
		PermissionUser[] users = new PermissionUser[userNames.size()];
		
		int index = 0;
		for ( String groupName : userNames )
		{
			users[index++] = this.manager.getUser( groupName );
		}
		
		return users;
	}
	
	@Override
	public Collection<String> getRegisteredGroupNames()
	{
		return SQLEntity.getEntitiesNames( getSQL(), SQLEntity.Type.GROUP, false );
	}
	
	@Override
	public Collection<String> getRegisteredUserNames()
	{
		return SQLEntity.getEntitiesNames( getSQL(), SQLEntity.Type.USER, false );
	}
	
	protected final void setupAliases( Configuration config )
	{
		ConfigurationSection aliases = config.getConfigurationSection( "permissions.backends.sql.aliases" );
		
		if ( aliases == null )
		{
			return;
		}
		
		tableNames = aliases.getValues( false );
	}
	
	private void executeStream( SQLConnection conn, InputStream str ) throws SQLException, IOException
	{
		String deploySQL = StringUtils.readStream( str );
		
		Statement s = conn.getStatement();
		
		for ( String sqlQuery : deploySQL.trim().split( ";" ) )
		{
			sqlQuery = sqlQuery.trim();
			if ( sqlQuery.isEmpty() )
			{
				continue;
			}
			
			sqlQuery = conn.expandQuery( sqlQuery + ";" );
			
			s.addBatch( sqlQuery );
		}
		s.executeBatch();
	}
	
	protected final void deployTables() throws PermissionBackendException
	{
		try
		{
			if ( this.getSQL().hasTable( "{permissions}" ) )
			{
				return;
			}
			InputStream databaseDumpStream = getClass().getResourceAsStream( "/sql/" + getSQL().getDriver() + ".sql" );
			
			if ( databaseDumpStream == null )
			{
				throw new Exception( "Can't find appropriate database dump for used database (" + getSQL().getDriver() + "). Is it bundled?" );
			}
			
			Logger.getLogger( "PermissionsEx" ).info( "Deploying default database scheme" );
			
			executeStream( getSQL(), databaseDumpStream );
			
			PermissionGroup defGroup = getGroup( "default" );
			String deployFile = config.getString( "permissions.backends.sql.deploy", "" );
			if ( deployFile.length() > 0 )
			{
				final File deploy = new File( PermissionsEx.getPlugin().getDataFolder(), deployFile );
				if ( !deploy.exists() )
				{
					throw new Exception( "Permissions deploy file for SQL does not exist!" );
				}
				executeStream( getSQL(), new FileInputStream( deploy ) );
				config.set( "permissions.backends.sql.deploy", null );
				
			}
			else
			{
				defGroup.addPermission( "modifysite.*" );
				setDefaultGroup( defGroup, null );
			}
			
			Logger.getLogger( "PermissionsEx" ).info( "Database scheme deploying complete." );
			
		}
		catch ( Exception e )
		{
			throw new PermissionBackendException( "Deploying of default data failed. Please initialize database manually using " + getSQL().getDriver() + ".sql", e );
		}
	}
	
	private static String blankIfNull( String in )
	{
		return in == null ? "" : in;
	}
	
	@Override
	public void dumpData( OutputStreamWriter writer ) throws IOException
	{
		// Users
		for ( PermissionUser user : this.manager.getUsers() )
		{
			writer.append( "/* User " ).append( user.getName() ).append( " */\n" );
			// Basic info (Prefix/Suffix)
			writer.append( "INSERT INTO `{permissions_entity}` ( `name`, `type`, `prefix`, `suffix` ) VALUES ( '" ).append( user.getName() ).append( "', 1, '" ).append( blankIfNull( user.getOwnPrefix() ) ).append( "','" ).append( blankIfNull( user.getOwnSuffix() ) ).append( "' );\n" );
			
			// Inheritance
			for ( String group : user.getGroupsNames() )
			{
				writer.append( "INSERT INTO `{permissions_inheritance}` ( `child`, `parent`, `type` ) VALUES ( '" ).append( user.getName() ).append( "', '" ).append( group ).append( "',  1);\n" );
			}
			
			// Permissions
			for ( Map.Entry<String, String[]> entry : user.getAllPermissions().entrySet() )
			{
				for ( String permission : entry.getValue() )
				{
					String site = entry.getKey();
					
					if ( site == null )
					{
						site = "";
					}
					
					writer.append( "INSERT INTO `{permissions}` ( `name`, `type`, `permission`, `site`, `value` ) VALUES ('" ).append( user.getName() ).append( "', 1, '" ).append( permission ).append( "', '" ).append( site ).append( "', ''); \n" );
				}
			}
			
			for ( String site : user.getSites() )
			{
				if ( site == null )
					continue;
				
				final String sitePrefix = user.getOwnPrefix( site );
				final String siteSuffix = user.getOwnSuffix( site );
				
				if ( sitePrefix != null && !sitePrefix.isEmpty() )
				{
					writer.append( "INSERT INTO `{permissions}` (`name`, `type`, `permission`, `site`, `value`) VALUES ('" ).append( user.getName() ).append( "', 1, 'prefix', '" ).append( site ).append( "', '" ).append( sitePrefix ).append( "');\n" );
				}
				if ( siteSuffix != null && !siteSuffix.isEmpty() )
				{
					writer.append( "INSERT INTO `{permissions}` (`name`, `type`, `permission`, `site`, `value`) VALUES ('" ).append( user.getName() ).append( "', 1, 'suffix', '" ).append( site ).append( "', '" ).append( siteSuffix ).append( "');\n" );
				}
			}
			
			// Options
			for ( Map.Entry<String, Map<String, String>> entry : user.getAllOptions().entrySet() )
			{
				for ( Map.Entry<String, String> option : entry.getValue().entrySet() )
				{
					String value = option.getValue().replace( "'", "\\'" );
					String site = entry.getKey();
					
					if ( site == null )
					{
						site = "";
					}
					
					writer.append( "INSERT INTO `{permissions}` ( `name`, `type`, `permission`, `site`, `value` ) VALUES ('" ).append( user.getName() ).append( "', 1, '" ).append( option.getKey() ).append( "', '" ).append( site ).append( "', '" ).append( value ).append( "' );\n" );
				}
			}
		}
		
		PermissionGroup defaultGroup = manager.getDefaultGroup();
		
		// Groups
		for ( PermissionGroup group : this.manager.getGroups() )
		{
			writer.append( "/* Group " ).append( group.getName() ).append( " */\n" );
			// Basic info (Prefix/Suffix)
			writer.append( "INSERT INTO `{permissions_entity}` ( `name`, `type`, `prefix`, `suffix`, `default` ) VALUES ( '" ).append( group.getName() ).append( "', 0, '" ).append( blankIfNull( group.getOwnPrefix() ) ).append( "','" ).append( blankIfNull( group.getOwnSuffix() ) ).append( "', " ).append( group.equals( defaultGroup ) ? "1" : "0" ).append( " );\n" );
			
			for ( String site : group.getSites() )
			{
				if ( site == null )
					continue;
				
				final String sitePrefix = group.getOwnPrefix( site );
				final String siteSuffix = group.getOwnSuffix( site );
				
				if ( sitePrefix != null && !sitePrefix.isEmpty() )
				{
					writer.append( "INSERT INTO `{permissions}` (`name`, `type`, `permission`, `site`, `value`) VALUES ('" ).append( group.getName() ).append( "', 0, 'prefix', '" ).append( site ).append( "', '" ).append( sitePrefix ).append( "');\n" );
				}
				if ( siteSuffix != null && !siteSuffix.isEmpty() )
				{
					writer.append( "INSERT INTO `{permissions}` (`name`, `type`, `permission`, `site`, `value`) VALUES ('" ).append( group.getName() ).append( "', 0, 'suffix', '" ).append( site ).append( "', '" ).append( siteSuffix ).append( "');\n" );
				}
			}
			
			// Inheritance
			for ( String parent : group.getParentGroupsNames() )
			{
				writer.append( "INSERT INTO `{permissions_inheritance}` ( `child`, `parent`, `type` ) VALUES ( '" ).append( group.getName() ).append( "', '" ).append( parent ).append( "',  0);\n" );
			}
			
			// Permissions
			for ( Map.Entry<String, String[]> entry : group.getAllPermissions().entrySet() )
			{
				for ( String permission : entry.getValue() )
				{
					String site = entry.getKey();
					
					if ( site == null )
					{
						site = "";
					}
					
					writer.append( "INSERT INTO `{permissions}` ( `name`, `type`, `permission`, `site`, `value`) VALUES ('" ).append( group.getName() ).append( "', 0, '" ).append( permission ).append( "', '" ).append( site ).append( "', '');\n" );
				}
			}
			
			// Options
			for ( Map.Entry<String, Map<String, String>> entry : group.getAllOptions().entrySet() )
			{
				for ( Map.Entry<String, String> option : entry.getValue().entrySet() )
				{
					String value = option.getValue().replace( "'", "\\'" );
					String site = entry.getKey();
					
					if ( site == null )
					{
						site = "";
					}
					
					writer.append( "INSERT INTO `{permissions}` ( `name`, `type`, `permission`, `site`, `value` ) VALUES ('" ).append( group.getName() ).append( "', 0, '" ).append( option.getKey() ).append( "', '" ).append( site ).append( "', '" ).append( value ).append( "' );\n" );
				}
			}
		}
		
		// Site-inheritance
		writer.append( "/* Site Inheritance */\n" );
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			String[] parentSites = manager.getSiteInheritance( site.getName() );
			if ( parentSites.length == 0 )
			{
				continue;
			}
			
			for ( String parentSite : parentSites )
			{
				writer.append( "INSERT INTO `{permissions_inheritance}` ( `child`, `parent`, `type` ) VALUES ( '" ).append( site.getName() ).append( "', '" ).append( parentSite ).append( "',  2);\n" );
			}
		}
		
		writer.flush();
	}
	
	@Override
	public String[] getSiteInheritance( String site )
	{
		if ( site == null || site.isEmpty() )
		{
			return new String[0];
		}
		
		if ( !siteInheritanceCache.containsKey( site ) )
		{
			try
			{
				ResultSet result = this.getSQL().prepAndBind( "SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2;", site ).executeQuery();
				LinkedList<String> siteParents = new LinkedList<String>();
				
				while ( result.next() )
				{
					siteParents.add( result.getString( "parent" ) );
				}
				
				this.siteInheritanceCache.put( site, siteParents.toArray( new String[0] ) );
			}
			catch ( SQLException e )
			{
				throw new RuntimeException( e );
			}
		}
		
		return siteInheritanceCache.get( site );
	}
	
	@Override
	public void setSiteInheritance( String siteName, String[] parentSites )
	{
		if ( siteName == null || siteName.isEmpty() )
		{
			return;
		}
		
		try
		{
			this.getSQL().prepAndBind( "DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2", siteName ).execute();
			
			PreparedStatement statement = this.getSQL().prepAndBind( "INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`) VALUES (?, ?, 2)", siteName, "toset" );
			for ( String parentSite : parentSites )
			{
				statement.setString( 2, parentSite );
				statement.addBatch();
			}
			statement.executeBatch();
			
			this.siteInheritanceCache.put( siteName, parentSites );
			
		}
		catch ( SQLException e )
		{
			throw new RuntimeException( e );
		}
	}
	
	@Override
	public void reload()
	{
		siteInheritanceCache.clear();
	}
}
