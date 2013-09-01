package com.chiorichan.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.user.UserLoginEvent;
import com.chiorichan.event.user.UserLoginEvent.Result;
import com.chiorichan.framework.Session;
import com.chiorichan.permissions.PermissionAttachmentInfo;
import com.chiorichan.plugin.Plugin;
import com.google.gson.Gson;

public class User extends Session
{
	public Loader server;
	public boolean valid = false;
	public String userId = "", displayLevel = "", displayName = "",
			userLevel = "", password = "", lastMsg = "", username = "", email = "";
	
	private Map<String, String> sqlMap = new HashMap<String, String>();
	
	public static Map<String, String> reasons = new HashMap<String, String>();
	
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
			
			JSONObject json = new JSONObject();
			try
			{
				json = SqlConnector.convert( rs );
			}
			catch ( SQLException | JSONException e )
			{
				e.printStackTrace();
			}
			
			sqlMap = (Map<String, String>) new Gson().fromJson( json.toString(), TreeMap.class ).get( "0" );
			rs.first();
			
			if ( rs.getInt( "numloginfail" ) > 5 )
			{
				if ( rs.getInt( "lastloginfail" ) > ( System.currentTimeMillis() - 1800 ) )
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
			
			sql.queryUpdate( "UPDATE `users` SET `lastactive` = '" + System.currentTimeMillis() + "' WHERE `userID` = '" + getUserId() + "'" );
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
	
	public boolean hasPermission( String broadcastChannelAdministrative )
	{
		// TODO Auto-generated method stub
		return false;
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
}
