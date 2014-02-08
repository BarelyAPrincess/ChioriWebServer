package com.chiorichan.user;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.framework.Site;
import com.chiorichan.permissions.PermissibleBase;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionAttachment;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.user.builtin.UserLookupAdapter;
import com.google.common.collect.Sets;

public class User implements CommandSender
{
	public Loader server;
	
	protected final PermissibleBase perm = new PermissibleBase( this );
	protected UserMetaData metaData = new UserMetaData();
	protected String userId;
	protected boolean op;
	protected Set<UserHandler> handlers = Sets.newLinkedHashSet(); // Set this handler for the last login
	protected UserLookupAdapter _cachedAdapter;
	
	public User(String user, UserLookupAdapter adapter) throws LoginException
	{
		if ( user.isEmpty() )
			throw new LoginException( LoginException.ExceptionReasons.emptyUsername );
		
		if ( adapter == null )
			throw new LoginException( LoginException.ExceptionReasons.unknownError );
		
		_cachedAdapter = adapter;
		
		metaData = adapter.loadUser( user );
		userId = metaData.getUserId();
		
		List<String> ops = Loader.getConfig().getStringList( "users.operators" );
		op = ( ops.contains( userId ) || ops.contains( user ) );
	}
	
	public UserMetaData getMetaData()
	{
		return metaData;
	}
	
	public boolean validatePassword( String _password )
	{
		String password = metaData.getPassword();
		return ( password.equals( _password ) || password.equals( DigestUtils.md5Hex( _password ) ) || DigestUtils.md5Hex( password ).equals( _password ) );
	}
	
	protected String getPassword()
	{
		return metaData.getPassword();
	}
	
	public String getDisplayName()
	{
		return metaData.getString( "displayName" );
	}
	
	public String getUsername()
	{
		return metaData.getUsername();
	}
	
	public String getName()
	{
		return metaData.getUserId();
	}
	
	public void kick( String kickMessage )
	{
		for ( UserHandler handler : handlers )
			handler.kick( kickMessage );
	}
	
	public void save()
	{
		
	}
	
	public void sendPluginMessage( Plugin source, String channel, byte[] message )
	{
		
	}
	
	public Collection<? extends String> getListeningPluginChannels()
	{
		
		return null;
	}
	
	public void sendMessage( String string )
	{
		
	}
	
	public void setBanned( boolean b )
	{
		
	}
	
	public boolean canSee( User user )
	{
		return false;
	}
	
	public void setWhitelisted( boolean b )
	{
		
	}
	
	public boolean isWhitelisted()
	{
		// TODO Check the whitelist
		return true;
	}
	
	public String getUserId()
	{
		String uid = metaData.getString( "userId" );
		
		// temp
		if ( uid == null )
			uid = metaData.getString( "userID" );
		
		return uid;
	}
	
	@Deprecated
	public String getUserLevel()
	{
		return "-1";
	}
	
	@Deprecated
	public String getDisplayLevel()
	{
		return "Deprecated";
	}
	
	public String toString()
	{
		return "User{" + metaData.toString() + ",Handlers{" + handlers.toString() + "}}";
	}
	
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	public String getString( String key, String def )
	{
		if ( !metaData.containsKey( key ) )
			return def;
		
		return metaData.getString( key );
	}
	
	@Override
	public void sendMessage( String[] messages )
	{
		for ( UserHandler handler : handlers )
			handler.sendMessage( messages );
	}
	
	public LinkedHashMap<String, Object> getMyLocations( boolean returnOne, boolean returnString, String whereAlt )
	{
		List<String> where = new ArrayList<String>();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		SqlConnector sql = getSite().getDatabase();
		
		if ( whereAlt == null )
			whereAlt = "";
		
		try
		{
			if ( !hasPermission( "applebloom.admin" ) )
			{
				ResultSet rs = sql.query( "SELECT * FROM `accounts` WHERE `maintainers` like '%" + getUserId() + "%';" );
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						where.add( "`acctID` = '" + rs.getString( "acctID" ) + "'" );
					}
					while ( rs.next() );
				}
				
				rs = sql.query( "SELECT * FROM `locations` WHERE `maintainers` like '%" + getUserId() + "%';" );
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						where.add( "`locID` = '" + rs.getString( "locID" ) + "'" );
					}
					while ( rs.next() );
				}
				
				if ( hasPermission( "STORE" ) )
					where.add( "`locID` = '" + getUserId() + "'" );
				
				if ( where.isEmpty() )
					return result;
				
				StringBuilder sb = new StringBuilder();
				for ( String s : where )
				{
					sb.append( " OR " + s );
				}
				
				sqlWhere = sb.toString().substring( 4 );
			}
			
			if ( !whereAlt.isEmpty() )
			{
				sqlWhere = sqlWhere + " AND " + whereAlt;
			}
			
			if ( !sqlWhere.isEmpty() )
				sqlWhere = " WHERE " + sqlWhere;
			
			ResultSet rs = sql.query( "SELECT * FROM `locations`" + sqlWhere + ";" );
			
			if ( sql.getRowCount( rs ) < 1 )
				return result;
			
			result = SqlConnector.convert( rs );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return result;
		}
		
		if ( returnOne || returnString )
		{
			Object o = result.get( 0 );
			
			if ( o instanceof LinkedHashMap )
			{
				@SuppressWarnings( "unchecked" )
				LinkedHashMap<String, Object> one = (LinkedHashMap<String, Object>) o;
				return one;
			}
			else
			{
				return new LinkedHashMap<String, Object>();
			}
		}
		else
		{
			return result;
		}
	}
	
	public Map<String, Object> getMyLocations( boolean returnOne, boolean returnString )
	{
		return getMyLocations( returnOne, returnString, null );
	}
	
	public Map<String, Object> getMyLocations( boolean returnOne )
	{
		return getMyLocations( returnOne, false, null );
	}
	
	public Map<String, Object> getMyLocations()
	{
		return getMyLocations( false, false, null );
	}
	
	public LinkedHashMap<String, Object> getMyAccounts( boolean returnOne, boolean returnString, String whereAlt )
	{
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		SqlConnector sql = getSite().getDatabase();
		
		if ( whereAlt == null )
			whereAlt = "";
		
		try
		{
			if ( !hasPermission( "applebloom.admin" ) )
				sqlWhere = "`maintainers` like '%" + getUserId() + "%'";
			
			if ( !whereAlt.isEmpty() )
			{
				sqlWhere = sqlWhere + " AND " + whereAlt;
			}
			
			if ( !sqlWhere.isEmpty() )
				sqlWhere = " WHERE " + sqlWhere;
			
			ResultSet rs = sql.query( "SELECT * FROM `accounts`" + sqlWhere + ";" );
			
			if ( sql.getRowCount( rs ) < 1 )
				return result;
			
			result = SqlConnector.convert( rs );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return result;
		}
		
		if ( returnOne || returnString )
		{
			Object o = result.get( 0 );
			
			if ( o instanceof LinkedHashMap )
			{
				@SuppressWarnings( "unchecked" )
				LinkedHashMap<String, Object> one = (LinkedHashMap<String, Object>) o;
				return one;
			}
			else
			{
				return new LinkedHashMap<String, Object>();
			}
		}
		else
		{
			return result;
		}
	}
	
	public LinkedHashMap<String, Object> getMyAccounts( boolean returnOne, boolean returnString )
	{
		return getMyAccounts( returnOne, returnString, null );
	}
	
	public LinkedHashMap<String, Object> getMyAccounts( boolean returnOne )
	{
		return getMyAccounts( returnOne, false, null );
	}
	
	public LinkedHashMap<String, Object> getMyAccounts()
	{
		return getMyAccounts( false, false, null );
	}
	
	public Site getSite()
	{
		Site site = null;
		
		for ( UserHandler handler : handlers )
			if ( handler != null && handler.getSite() != null )
				site = handler.getSite();
		
		if ( site == null )
			return Loader.getPersistenceManager().getSiteManager().getFrameworkSite();
		
		return site;
	}
	
	public boolean isPermissionSet( String name )
	{
		return perm.isPermissionSet( name );
	}
	
	public boolean isPermissionSet( Permission perm )
	{
		return this.perm.isPermissionSet( perm );
	}
	
	public boolean hasPermission( String req )
	{
		Loader.getLogger().info( ChatColor.GREEN + "Checking `" + getUserId() + "` for permission `" + req + "` with result `" + perm.hasPermission( req ) + "`" );
		
		// Everyone
		if ( req.equals( "-1" ) )
			return true;
		
		// OP Only
		if ( req.equals( "0" ) )
			return isOp();
		
		return perm.hasPermission( req );
	}
	
	public boolean hasPermission( Permission perm )
	{
		return this.perm.hasPermission( perm );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		return perm.addAttachment( plugin, name, value );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		return perm.addAttachment( plugin );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		return perm.addAttachment( plugin, name, value, ticks );
	}
	
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		return perm.addAttachment( plugin, ticks );
	}
	
	public void removeAttachment( PermissionAttachment attachment )
	{
		perm.removeAttachment( attachment );
	}
	
	public void recalculatePermissions()
	{
		perm.recalculatePermissions();
	}
	
	public void setOp( boolean value )
	{
		op = value;
		perm.recalculatePermissions();
	}
	
	public boolean isOp()
	{
		return op;
	}
	
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		return perm.getEffectivePermissions();
	}
	
	public void disconnect( String reason )
	{
		perm.clearPermissions();
	}
	
	public String getAddress()
	{
		if ( handlers.size() > 0 )
			return handlers.toArray( new UserHandler[0] )[handlers.size()].getIpAddr();
		
		return null;
	}
	
	public boolean isBanned()
	{
		return false;
	}
	
	public void putHandler( UserHandler handler )
	{
		if ( !handlers.contains( handler ) )
			handlers.add( handler );
	}
	
	public void removeHandler( UserHandler handler )
	{
		handlers.remove( handler );
	}
	
	public void reloadAndValidate() throws LoginException
	{
		metaData = _cachedAdapter.loadUser( userId );
		
		List<String> ops = Loader.getConfig().getStringList( "users.operators" );
		op = ( ops.contains( userId ) || ops.contains( metaData.getUsername() ) );
	}
}
