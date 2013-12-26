package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Session;

import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.user.User;

public class FrameworkUserService
{
	protected Framework fw;
	protected User currentUser = null;
	protected PersistentSession _sess;
	
	public FrameworkUserService(Framework fw0)
	{
		fw = fw0;
		_sess = fw0.getSession();
	}
	
	public boolean getUserState()
	{
		return ( currentUser != null );
	}
	
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	public String getString( String key, String def )
	{
		if ( currentUser == null )
			return def;
		
		return currentUser.getString( key, def );
	}
	
	public boolean initalize( String reqLevel ) throws SQLException
	{
		if ( fw.getRequest().getSite().getUserList() == null )
			return true;
		
		String username = fw.getServer().getRequest( "user" );
		String password = fw.getServer().getRequest( "pass" );
		String target = fw.getServer().getRequest( "target" );
		
		if ( fw.getServer().getRequest( "logout", "", true ) != null )
		{
			logout();
			
			if ( target.isEmpty() )
			{
				target = fw.getRequest().getSite().getYaml().getString( "scripts.login-form", "/login" );
			}
			
			fw.getServer().dummyRedirect( target );
		}
		
		if ( !username.isEmpty() && !password.isEmpty() )
		{
			User user = fw.getRequest().getSite().getUserList().validateUser( fw, username, password );
			
			Loader.getLogger().info( "User: " + user );
			
			if ( user != null && user.isValid() )
			{
				currentUser = user;
				
				String loginPost = ( target.isEmpty() ) ? fw.getRequest().getSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				Loader.getLogger().info( "Login Success: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginPost );
			}
			else if ( user == null )
			{
				return false;
			}
			else
			{
				String loginForm = fw.getRequest().getSite().getYaml().getString( "scripts.login-form", "/login" );
				
				Loader.getLogger().warning( "Login Failed: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginForm + "?msg=" + user.getLastError() + "&target=" + target );
			}
		}
		else
		{
			username = fw.getUserService().getSessionString( "user" );
			password = fw.getUserService().getSessionString( "pass" );
			
			if ( !username.isEmpty() && !password.isEmpty() )
			{
				User user = fw.getRequest().getSite().getUserList().validateUser( fw, username, password );
				
				if ( user != null && user.isValid() )
				{
					currentUser = user;
					
					String loginPost = ( target == null || target.isEmpty() ) ? fw.getRequest().getSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
					
					Loader.getLogger().info( "Login Success: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
					// fw.getServer().dummyRedirect( loginPost );
				}
				else
				{
					Loader.getLogger().warning( "Login Status: No Valid Login Present" );
				}
				
				// -1 = Allow All | 0 = Operator
				if ( !reqLevel.equals( "-1" ) )
				{
					if ( !getPermission( "", user.getUserId() ) && reqLevel.equals( "0" ) ) // Root Check
					{
						fw.getServer().panic( 401, "This page is limited to Operators only!" );
					}
					else if ( getPermission( reqLevel, user.getUserId() ) && !user.getUserLevel().equals( "0" ) )
					{
						fw.getServer().panic( 401, "This page is limited to members with access to the \"" + reqLevel + "\" permission or better." );
					}
					else if ( !user.isValid() )
					{
						String loginForm = fw.getRequest().getSite().getYaml().getString( "scripts.login-form", "/login" );
						fw.getServer().dummyRedirect( loginForm + "?msg=You must be logged in to view that page!&target=" + fw.getRequest().getURI() );
						return false;
					}
				}
			}
			else
			{
				if ( !reqLevel.equals( "-1" ) )
				{
					String loginForm = fw.getRequest().getSite().getYaml().getString( "scripts.login-form", "/login" );
					fw.getServer().dummyRedirect( loginForm + "?msg=You must be logged in to view that page!&target=" + fw.getRequest().getURI() );
					return false;
				}
			}
		}
		
		return true;
	}
	
	private void logout()
	{
		_sess.setArgument( "user", null );
		_sess.setArgument( "pass", null );
		Loader.getLogger().info( "User Logout" );
	}
	
	public boolean hasPermission( String key )
	{
		if ( !getUserState() )
			return false;
		
		return getPermission( key, currentUser.getUserId() );
	}
	
	public boolean getPermission( String key )
	{
		return getPermission( Arrays.asList( key ), null );
	}
	
	public boolean getPermission( List<String> permName )
	{
		return getPermission( permName, null );
	}
	
	public boolean getPermission( String permName, String idenifier )
	{
		return getPermission( Arrays.asList( permName ), idenifier );
	}
	
	/**
	 * This function checks the users permission level against the permissions table for if the requested permission is
	 * allowed by Current User.
	 */
	public boolean getPermission( List<String> permName, String idenifier )
	{
		try
		{
			SqlConnector sql = fw.getRequest().getSite().sql;
			
			if ( permName == null || permName.isEmpty() )
				permName = Arrays.asList( "ROOT" );
			
			String userLevel = null;
			
			if ( currentUser != null && ( idenifier == null || idenifier.isEmpty() ) )
			{
				idenifier = currentUser.getUserId();
				userLevel = currentUser.getUserLevel();
			}
			
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
	
	public void CheckPermision() throws SQLException
	{
		CheckPermision( "" );
	}
	
	/*
	 * This function gives scripts easy access to the getPermission function without the extra requirments. Recommended
	 * uses would be checking if page load is allowed by user.
	 */
	public boolean CheckPermision( String perm_name ) throws SQLException
	{
		if ( perm_name == null )
			perm_name = "";
		
		if ( !getPermission( perm_name ) )
		{
			// XXX: Intent is to give the user an error page if they don't have permission.
			fw.generateError( 401, "This page is limited to members with access to the \"" + perm_name + "\" permission or better. If access is required please contact us or see your account holder for help." );
			return false;
		}
		
		return true;
	}
	
	public User getCurrentUser()
	{
		return currentUser;
	}
	
	public String getSessionString( String key )
	{
		return getSessionString( key, "" );
	}
	
	public String getSessionString( String key, String def )
	{
		String val = _sess.getArgument( key );
		
		if ( val == null || val.isEmpty() )
			return def;
		
		return val;
	}
	
	public boolean setSessionString( String key )
	{
		return setSessionString( key, "" );
	}
	
	public boolean setSessionString( String key, String value )
	{
		if ( value == null )
			value = "";
		
		_sess.setArgument( key, value );
		
		return true;
	}
	
	public void setCookieExpiry( int valid )
	{
		_sess.setCookieExpiry( valid );
	}
	
	public void destroySession()
	{
		_sess.destroy();
	}
	
	public void saveSession()
	{
		_sess.saveSession();
	}
	
	public boolean isSessionStringSet( String key )
	{
		return _sess.isSet( key );
	}
	
	public LinkedHashMap<String, Object> getMyLocations( boolean returnOne, boolean returnString, String whereAlt )
	{
		SqlConnector sql = fw.getRequest().getSite().getDatabase();
		List<String> where = new ArrayList<String>();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		if ( currentUser == null )
			return result;
		
		try
		{
			if ( !getPermission( "ADMIN" ) )
			{
				ResultSet rs = sql.query( "SELECT * FROM `accounts` WHERE `maintainers` like '%" + currentUser.getUserId() + "%';" );
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						where.add( "`acctID` = '" + rs.getString( "acctID" ) + "'" );
					}
					while ( rs.next() );
				}
				
				rs = sql.query( "SELECT * FROM `locations` WHERE `maintainers` like '%" + currentUser.getUserId() + "%';" );
				if ( sql.getRowCount( rs ) > 0 )
				{
					do
					{
						where.add( "`locID` = '" + rs.getString( "locID" ) + "'" );
					}
					while ( rs.next() );
				}
				
				if ( getPermission( "STORE" ) )
					where.add( "`locID` = '" + getCurrentUser().getUserId() + "'" );
				
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
		SqlConnector sql = fw.getRequest().getSite().getDatabase();
		List<String> where = new ArrayList<String>();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		if ( currentUser == null )
			return result;
		
		try
		{
			if ( !getPermission( "ADMIN" ) )
				sqlWhere = "`maintainers` like '%" + currentUser.getUserId() + "%'";
			
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
	
	public boolean settingCompare( String settings )
	{
		return settingCompare( Arrays.asList( settings ) );
	}
	
	/**
	 * Does a setting compare based on a string if No expected mean is interped as a boolean of true. ex.
	 * USER_BETA_TESTER&USER_RANK=USER|USER_RANK=ADMIN
	 * 
	 * @param String
	 *           settingString
	 * @throws JSONException
	 */
	public boolean settingCompare( List<String> settings )
	{
		for ( String setting : settings )
		{
			boolean granted = false;
			for ( String key : setting.split( "[&]" ) )
			{
				String value;
				
				if ( key.indexOf( "=" ) < 0 )
					value = "1";
				else
				{
					value = key.substring( key.indexOf( "=" ) );
					key = key.substring( 0, key.indexOf( "=" ) );
				}
				
				if ( key.startsWith( "!" ) )
				{
					if ( get( key.substring( 1 ) ).equals( value ) )
						granted = true;
					else
					{
						granted = false;
						break;
					}
				}
				else
				{
					if ( get( key ).equals( value ) )
						granted = true;
					else
					{
						granted = false;
						break;
					}
				}
			}
			
			if ( granted )
				return true;
		}
		
		return false;
	}
	
	public boolean getBoolean( String key )
	{
		Object result = get( key );
		
		if ( result instanceof Boolean )
			return ( (Boolean) result );
		
		if ( result instanceof Integer && ( (Integer) result ) == 1 )
			return true;
		
		if ( result instanceof Long && ( (Long) result ) == 1 )
			return true;
		
		if ( result instanceof String && ( (String) result ).equals( "1" ) )
			return true;
		
		return false;
	}
	
	public Object get( String key )
	{
		return get( key, null, null, false );
	}
	
	public Object get( String key, String idenifier )
	{
		return get( key, idenifier, null, false );
	}
	
	public Object get( String key, String idenifier, boolean defaultValue )
	{
		return get( key, idenifier, defaultValue, false );
	}
	
	public Object get( String key, String idenifier, Object defaultValue )
	{
		return get( key, idenifier, defaultValue, false );
	}
	
	public Object get( String key, String idenifier, boolean defaultValue, boolean returnRow )
	{
		return get( key, idenifier, defaultValue, returnRow );
	}
	
	public Object get( String key, String idenifier, Object defaultValue, boolean returnRow )
	{
		if ( defaultValue == null )
			defaultValue = "";
		
		try
		{
			SqlConnector sql = fw.getRequest().getSite().sql;
			
			if ( idenifier == null || idenifier == "-1" )
			{
				idenifier = ( fw.getUserService().getUserState() ) ? fw.getUserService().getCurrentUser().getUserId() : "";
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return defaultValue;
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			
			Map<String, Object> defop = SqlConnector.convertRow( defaultRs );
			defop.put( "default", defop.get( "value" ) );
			
			if ( customRs == null || sql.getRowCount( customRs ) < 1 )
			{
				defaultRs.first();
				
				if ( !returnRow )
					return defaultRs.getString( "value" );
				
				return defop;
			}
			else
			{
				if ( !returnRow )
					return customRs.getString( "value" );
				
				Map<String, Object> op = SqlConnector.convertRow( customRs );
				
				defop.putAll( op );
				
				return defop;
			}
		}
		catch ( SQLException | JSONException e )
		{
			e.printStackTrace();
			return defaultValue;
		}
	}
	
	/*
	 * Empty value deletes resets setting to default.
	 */
	public boolean set( String key, String value, String idenifier )
	{
		try
		{
			SqlConnector sql = fw.getRequest().getSite().sql;
			
			if ( idenifier == null || idenifier == "-1" )
			{
				// TODO: Set idenifier to the logged in userId
				
				// if ( key.startsWith( "TEXT" ) || key.startsWith( "LOCATION" ) )
				// TODO: Set to the first location user is allowed to use
				// else if ( key.startsWith( "ACCOUNT" ) )
				
				// else
				idenifier = "";
			}
			
			ResultSet defaultRs = sql.query( "SELECT * FROM `settings_default` WHERE `key` = '" + key + "';" );
			
			if ( defaultRs == null || sql.getRowCount( defaultRs ) < 1 )
				return false;
			
			if ( value.isEmpty() || defaultRs.getString( "value" ).equals( value ) )
			{
				sql.queryUpdate( "DELETE FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "'" );
				return true;
			}
			
			ResultSet customRs = sql.query( "SELECT * FROM `settings_custom` WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "' LIMIT 1;" );
			
			if ( customRs == null || sql.getRowCount( customRs ) < 1 )
			{
				sql.queryUpdate( "UPDATE `settings_custom` SET `value` = '" + value + "' WHERE `key` = '" + key + "' AND `owner` = '" + idenifier + "';" );
			}
			else
			{
				sql.queryUpdate( "INSERT INTO `settings_custom` (`key`, `value`, `owner`)VALUES('" + key + "', '" + value + "', '" + idenifier + "');" );
			}
			
			return true;
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
			return false;
		}
	}
}
