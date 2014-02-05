package com.chiorichan.user;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

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

public class User implements CommandSender
{
	SqlConnector sql;
	public Loader server;
	// public String userId = "", displayLevel = "", displayName = "",
	// userLevel = "", password = "", lastMsg = "", username = "",
	// email = "";
	
	protected final PermissibleBase perm = new PermissibleBase( this );
	protected UserMetaData metaData = new UserMetaData();
	protected String username;
	protected boolean op, loggedIn = false;
	protected UserHandler handler;
	
	public User(String user, UserLookupAdapter adapter) throws LoginException
	{
		if ( user.isEmpty() )
			throw new LoginException( LoginException.ExceptionReasons.emptyUsername );
		
		if ( adapter == null )
			throw new LoginException( LoginException.ExceptionReasons.unknownError );
		
		metaData = adapter.loadUser( user );
		metaData.set( "username", user );
		username = user;
		op = Loader.getConfig().getList( "framework.users.operators" ).contains( user );
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
	
	public boolean isLoggedIn()
	{
		return loggedIn;
	}
	
	public String getDisplayName()
	{
		return metaData.getString( "displayName" );
	}
	
	public String getName()
	{
		return username;
	}
	
	// TODO: Que kick message in a buffer that is sent to user if they attempt to visit a page using a session user.
	public void kick( String kickMessage )
	{
		loggedIn = false;
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
		
		return false;
	}
	
	@Deprecated
	public boolean isValid()
	{
		return true;
	}
	
	public String getUserId()
	{
		return metaData.getString( "userId" );
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
		return "User{" + metaData.toString() + "}";
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
		handler.sendMessage( messages );
	}
	
	/**
	 * This function checks the users permission level against the permissions table for if the requested permission is
	 * allowed by Current User.
	 */
	/*
	 * public boolean hasPermission( List<String> permName )
	 * {
	 * try
	 * {
	 * if ( permName == null || permName.isEmpty() )
	 * permName = Arrays.asList( "ROOT" );
	 * String idenifier = getUserId();
	 * if ( userLevel == null || userLevel.isEmpty() )
	 * {
	 * Map<String, Object> result = sql.selectOne( "users", "userId", idenifier );
	 * if ( result == null )
	 * return false;
	 * userLevel = (String) result.get( "userlevel" );
	 * }
	 * Map<String, Object> perm = sql.selectOne( "accounts_access", "accessID", userLevel );
	 * if ( perm == null )
	 * return false;
	 * List<String> permList = Arrays.asList( ( (String) perm.get( "permissions" ) ).split( "[|]" ) );
	 * if ( permList.contains( "ROOT" ) )
	 * return true;
	 * if ( permList.contains( "ADMIN" ) )
	 * return true;
	 * for ( String p : permName )
	 * {
	 * boolean granted = false;
	 * String[] pS = p.split( "&" );
	 * for ( String pP : pS )
	 * {
	 * if ( pP.startsWith( "!" ) )
	 * {
	 * if ( permList.contains( pP.substring( 1 ) ) )
	 * {
	 * granted = false;
	 * break;
	 * }
	 * else
	 * {
	 * granted = true;
	 * }
	 * }
	 * else
	 * {
	 * if ( permList.contains( pP ) )
	 * {
	 * granted = true;
	 * }
	 * else
	 * {
	 * granted = false;
	 * break;
	 * }
	 * }
	 * }
	 * Loader.getLogger().info( "Getting Permission: " + permName + " for " + idenifier + " with result " + granted );
	 * if ( granted )
	 * return true; // Return true if one of the requested permission names exists in users allowed permissions
	 * // list.
	 * }
	 * }
	 * catch ( Exception ex )
	 * {
	 * ex.printStackTrace();
	 * }
	 * return false;
	 * }
	 */
	
	/*
	 * public void checkPermision() throws SQLException { checkPermision( "" ); }
	 * /* This function gives scripts easy access to the hasPermission function without the extra requirments.
	 * Recommended uses would be checking if page load is allowed by user.
	 * TODO: Implement this method elsewhere. public boolean checkPermision( String perm_name ) throws SQLException { if
	 * ( perm_name == null ) perm_name = "";
	 * if ( !hasPermission( perm_name ) ) { // XXX: Intent is to give the user an error page if they don't have
	 * permission. _sess.generateError( 401, "This page is limited to members with access to the \"" + perm_name +
	 * "\" permission or better. If access is required please contact us or see your account holder for help." ); return
	 * false; }
	 * return true; }
	 */
	
	public LinkedHashMap<String, Object> getMyLocations( boolean returnOne, boolean returnString, String whereAlt )
	{
		List<String> where = new ArrayList<String>();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		try
		{
			if ( !hasPermission( "ADMIN" ) )
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
		List<String> where = new ArrayList<String>();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		try
		{
			if ( !hasPermission( "ADMIN" ) )
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
		return handler.getSite();
	}
	
	public boolean isPermissionSet( String name )
	{
		return perm.isPermissionSet( name );
	}
	
	public boolean isPermissionSet( Permission perm )
	{
		return this.perm.isPermissionSet( perm );
	}
	
	public boolean hasPermission( String name )
	{
		Loader.getLogger().debug( getName() + " was checked for permission " + name );
		return perm.hasPermission( name );
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
		// TODO Return the last IP Address this user connected from.
		return null;
	}

	public boolean isBanned()
	{
		return false;
	}
}
