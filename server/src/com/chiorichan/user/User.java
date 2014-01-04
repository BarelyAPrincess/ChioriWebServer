package com.chiorichan.user;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLoginEvent.Result;
import com.chiorichan.permissions.Permission;
import com.chiorichan.permissions.PermissionAttachment;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.util.Common;
import com.chiorichan.util.ObjectUtil;

public class User implements CommandSender
{
	SqlConnector sql;
	public Loader server;
	public boolean valid = false;
	public String userId = "", displayLevel = "", displayName = "",
			userLevel = "", password = "", lastMsg = "", username = "",
			email = "";
	
	private LinkedHashMap<String, String> sqlMap = new LinkedHashMap<String, String>();
	
	public static LinkedHashMap<String, String> reasons = new LinkedHashMap<String, String>();
	
	static
	{
		reasons.put( "accountNotActivated", "Account is not activated." );
		reasons.put( "underAttackPleaseWait", "Max fail login tries reached. Account locked for 30 minutes." );
		reasons.put( "emptyUsername", "The specified username was empty. Please try again." );
		reasons.put( "emptyPassword", "The specified password was empty. Please try again." );
		reasons.put( "incorrectLogin", "Username and Password provided did not match any users on file." );
		reasons.put( "successLogin", "Your login has been successfully authenticated." );
		reasons.put( "unknownError", "Your login has failed due to an unknown internal error, Please try again." );
		reasons.put( "permissionsError", "Fatal error was detected with your user permissions. Please notify an administrator ASAP." );
	}
	
	public void invalidate( String key )
	{
		valid = false;
		lastMsg = ( reasons.containsKey( key ) ) ? reasons.get( key ) : reasons.get( "unknownError" );
	}
	
	public User(SqlConnector sql, String username, String password)
	{
		try
		{
			this.sql = sql;
			this.username = username;
			this.password = password;
			
			valid = true;
			
			if ( username == null || username.isEmpty() )
				invalidate( "emptyUsername" );
			
			if ( password == null || password.isEmpty() )
				invalidate( "emptyPassword" );
			
			if ( valid == false )
				return;
			
			// TODO: Site config additional login fields.
			
			UserLoginEvent event = new UserLoginEvent( this );
			
			Loader.getPluginManager().callEvent( event );
			
			String additionalUserFields = "";
			for ( String s : event.getAdditionalUserFields() )
			{
				additionalUserFields += " OR `" + s + "` = '" + username + "'";
			}
			
			ResultSet rs = sql.query( "SELECT * FROM `users` WHERE (`username` = '" + username + "' OR `userID` = '" + username + "'" + additionalUserFields + ") AND (`password` = '" + password + "' OR `password` = '" + DigestUtils.md5Hex( password ) + "' OR md5(`password`) = '" + password + "');" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				event.setResult( Result.DENIED );
			else
				event.setResult( Result.ALLOWED );
			
			Loader.getPluginManager().callEvent( event );
			
			if ( event.getResult() != Result.ALLOWED )
			{
				// TODO: Add returned messages for the other results.
				// TODO: Add whitelist and banned user check.
				
				if ( event.getKickMessage().isEmpty() )
					invalidate( "incorrectLogin" );
				else
				{
					valid = false;
					lastMsg = event.getKickMessage();
				}
				
				return;
			}
			
			LinkedHashMap<String, Object> sqlCast = new LinkedHashMap<String, Object>();
			try
			{
				sqlCast = SqlConnector.convertRow( rs );
			}
			catch ( JSONException e )
			{
				e.printStackTrace();
			}
			
			sqlMap.clear();
			
			for ( Entry<String, Object> e : sqlCast.entrySet() )
				sqlMap.put( (String) e.getKey(), ObjectUtil.castToString( e.getValue() ) );
			
			rs.first();
			
			if ( rs.getInt( "numloginfail" ) > 5 )
			{
				if ( rs.getInt( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				{
					invalidate( "underAttackPleaseWait" );
					return;
				}
			}
			
			if ( !rs.getString( "actnum" ).equals( "0" ) )
			{
				invalidate( "accountNotActivated" );
				return;
			}
			
			lastMsg = reasons.get( "successLogin" );
			userLevel = rs.getString( "userlevel" );
			userId = rs.getString( "userID" );
			email = rs.getString( "email" );
			
			Map<String, Object> level = sql.selectOne( "accounts_access", "accessID", rs.getString( "userlevel" ) );
			
			if ( level == null )
			{
				invalidate( "permissionError" );
				return;
			}
			
			valid = true;
			
			displayName = ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" );
			displayLevel = (String) level.get( "title" );
			
			sqlMap.put( "displayname", displayName );
			sqlMap.put( "displaylevel", displayLevel );
			
			sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + Common.getEpoch() + "' WHERE `userID` = '" + getUserId() + "'" );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
			invalidate( "unknownError" );
		}
	}
	
	public Loader getServer()
	{
		return server;
	}
	
	public String getName()
	{
		return username;
	}
	
	public void kick( String kickMessage )
	{
		
	}
	
	public void save()
	{
		
	}
	
	public void recalculatePermissions()
	{
		
	}
	
	public void sendPluginMessage( Plugin source, String channel, byte[] message )
	{
		// TODO Auto-generated method stub
		
	}
	
	public Collection<? extends String> getListeningPluginChannels()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void sendMessage( String string )
	{
		// TODO Auto-generated method stub
		
	}
	
	public void setBanned( boolean b )
	{
		// TODO Auto-generated method stub
		
	}
	
	public String getAddress()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setOp( boolean b )
	{
		
	}
	
	public boolean isOp()
	{
		return ( userLevel.equals( "0" ) );
	}
	
	public String getDisplayName()
	{
		return displayName;
	}
	
	public boolean canSee( User user )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public void setWhitelisted( boolean b )
	{
		// TODO Auto-generated method stub
		
	}
	
	public boolean isWhitelisted()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public Set<PermissionAttachmentInfo> getEffectivePermissions()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public String getUserId()
	{
		return userId;
	}
	
	public String getUserLevel()
	{
		return userLevel;
	}
	
	public String getDisplayLevel()
	{
		return displayLevel;
	}
	
	public String getLastError()
	{
		return lastMsg;
	}
	
	public String getUserName()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public String toString()
	{
		return "User{user=" + username + ",pass=" + password + ",userId=" + userId + ",level=" + userLevel + ",valid=" + valid + ",lastMsg=" + lastMsg + "}";
	}
	
	public String getEmail()
	{
		return email;
	}
	
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	public String getString( String key, String def )
	{
		if ( !sqlMap.containsKey( key ) )
			return def;
		
		return sqlMap.get( key );
	}
	
	@Override
	public boolean isPermissionSet( String name )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isPermissionSet( Permission perm )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean hasPermission( Permission perm )
	{
		Loader.getLogger().info( "User was checked for permission: " + perm );
		
		return true;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PermissionAttachment addAttachment( Plugin plugin, int ticks )
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void removeAttachment( PermissionAttachment attachment )
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sendMessage( String[] messages )
	{
		// TODO Auto-generated method stub
		
	}
	
	// TODO: Permissions system needs revamping to make it more like Bukkit Permissions
	// broadcastChannelAdministrative
	public boolean hasPermission( String permName )
	{
		return hasPermission( Arrays.asList( permName ) );
	}
	
	/**
	 * This function checks the users permission level against the permissions table for if the requested permission is
	 * allowed by Current User.
	 */
	public boolean hasPermission( List<String> permName )
	{
		try
		{
			if ( permName == null || permName.isEmpty() )
				permName = Arrays.asList( "ROOT" );
			
			String idenifier = getUserId();
			
			if ( userLevel == null || userLevel.isEmpty() )
			{
				Map<String, Object> result = sql.selectOne( "users", "userId", idenifier );
				
				if ( result == null )
					return false;
				
				userLevel = (String) result.get( "userlevel" );
			}
			
			Map<String, Object> perm = sql.selectOne( "accounts_access", "accessID", userLevel );
			
			if ( perm == null )
				return false;
			
			List<String> permList = Arrays.asList( ( (String) perm.get( "permissions" ) ).split( "[|]" ) );
			
			if ( permList.contains( "ROOT" ) )
				return true;
			
			if ( permList.contains( "ADMIN" ) )
				return true;
			
			for ( String p : permName )
			{
				boolean granted = false;
				String[] pS = p.split( "&" );
				
				for ( String pP : pS )
				{
					if ( pP.startsWith( "!" ) )
					{
						if ( permList.contains( pP.substring( 1 ) ) )
						{
							granted = false;
							break;
						}
						else
						{
							granted = true;
						}
					}
					else
					{
						if ( permList.contains( pP ) )
						{
							granted = true;
						}
						else
						{
							granted = false;
							break;
						}
					}
				}
				
				Loader.getLogger().info( "Getting Permission: " + permName + " for " + idenifier + " with result " + granted );
				
				if ( granted )
					return true; // Return true if one of the requested permission names exists in users allowed permissions
										// list.
			}
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	/*
	 * public void checkPermision() throws SQLException { checkPermision( "" ); }
	 * 
	 * /* This function gives scripts easy access to the hasPermission function without the extra requirments.
	 * Recommended uses would be checking if page load is allowed by user.
	 * 
	 * TODO: Implement this method elsewhere. public boolean checkPermision( String perm_name ) throws SQLException { if
	 * ( perm_name == null ) perm_name = "";
	 * 
	 * if ( !hasPermission( perm_name ) ) { // XXX: Intent is to give the user an error page if they don't have
	 * permission. _sess.generateError( 401, "This page is limited to members with access to the \"" + perm_name +
	 * "\" permission or better. If access is required please contact us or see your account holder for help." ); return
	 * false; }
	 * 
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
}
