package com.chiorichan.framework;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.user.User;
import com.google.gson.Gson;

public class FrameworkUserService
{
	protected Framework fw;
	protected User currentUser = null;
	protected Session _sess;
	
	public FrameworkUserService(Framework fw0)
	{
		fw = fw0;
		_sess = new Session( fw0 );
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
		
		String op = def;
		
		switch ( key )
		{
			case "displayname":
				op = currentUser.getDisplayName();
				break;
			case "displaylevel":
				op = currentUser.getDisplayLevel();
				break;
			case "email":
				op = currentUser.getEmail();
				break;
		}
		
		return op;
	}
	
	public boolean hasPermission( String key ) throws SQLException
	{
		if ( !getUserState() )
			return false;
		
		return GetPermission( key, currentUser.getUserId() );
	}
	
	public boolean initalize( String reqLevel ) throws SQLException
	{
		if ( fw.getCurrentSite().getUserList() == null )
			return true;
		
		HttpServletRequest req = fw.getRequest();
		
		String username = fw.getServer().getRequest( "user" );
		String password = fw.getServer().getRequest( "pass" );
		String target = fw.getServer().getRequest( "target" );
		
		if ( !fw.getServer().getRequest( "logout" ).isEmpty() )
		{
			logout();
			
			if ( target.isEmpty() )
			{
				target = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
			}
			
			fw.getServer().dummyRedirect( target );
		}
		
		if ( !username.isEmpty() && !password.isEmpty() )
		{
			User user = fw.getCurrentSite().getUserList().validateUser( fw, username, password );
			
			Loader.getLogger().info( "User: " + user );
			
			if ( user != null && user.isValid() )
			{
				currentUser = user;
				
				String loginPost = ( target.isEmpty() ) ? fw.getCurrentSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				Loader.getLogger().info( "Login Success: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginPost );
			}
			else if ( user == null )
			{
				return false;
			}
			else
			{
				String loginForm = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
				
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
				User user = fw.getCurrentSite().getUserList().validateUser( fw, username, password );
				
				if ( user != null && user.isValid() )
				{
					currentUser = user;
					
					String loginPost = ( target == null || target.isEmpty() ) ? fw.getCurrentSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
					
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
					if ( !GetPermission( "", user.getUserId() ) && reqLevel.equals( "0" ) ) // Root Check
					{
						fw.getServer().panic( 401, "This page is limited to Operators only!" );
					}
					else if ( GetPermission( reqLevel, user.getUserId() ) && user.getUserLevel() != "0" )
					{
						fw.getServer().panic( 401, "This page is limited to members with access to the \"" + reqLevel + "\" permission or better." );
					}
					else if ( !user.isValid() )
					{
						String loginForm = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
						fw.getServer().dummyRedirect( loginForm + "?msg=You must be logged in to view that page!&target=" + fw.getRequest().getRequestURI() );
						return false;
					}
				}
			}
			else
			{
				if ( !reqLevel.equals( "-1" ) )
				{
					String loginForm = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
					fw.getServer().dummyRedirect( loginForm + "?msg=You must be logged in to view that page!&target=" + fw.getRequest().getRequestURI() );
					return false;
				}
			}
		}
		
		return true;
	}
	
	private void logout()
	{
		fw.getUserService().destroySession();
		Loader.getLogger().info( "User Logout" );
	}
	
	public boolean GetPermission( String permName ) throws SQLException
	{
		return GetPermission( Arrays.asList( permName ), null );
	}
	
	public boolean GetPermission( List<String> permName ) throws SQLException
	{
		return GetPermission( permName, null );
	}
	
	public boolean GetPermission( String permName, String idenifier ) throws SQLException
	{
		return GetPermission( Arrays.asList( permName ), idenifier );
	}
	
	/*
	 * This function checks the users permission level againts the permissions table for if the requested permission is
	 * allowed by Current User.
	 */
	public boolean GetPermission( List<String> permName, String idenifier ) throws SQLException
	{
		SqlConnector sql = fw.getCurrentSite().sql;
		
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
		
		return false;
	}
	
	public void CheckPermision() throws SQLException
	{
		CheckPermision( "" );
	}
	
	/*
	 * This function gives scripts easy access to the GetPermission function without the extra requirments. Recommended
	 * uses would be checking if page load is allowed by user.
	 */
	public boolean CheckPermision( String perm_name ) throws SQLException
	{
		if ( perm_name == null )
			perm_name = "";
		
		if ( !GetPermission( perm_name ) )
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
		_sess.saveSession( fw );
	}
	
	public boolean isSessionStringSet( String key )
	{
		return _sess.isSet( key );
	}
	
	public Map<String, Map<String, Object>> getMyLocations( boolean returnOne, boolean returnString, String whereAlt )
	{
		SqlConnector sql = fw.getCurrentSite().getDatabase();
		List<String> where = new ArrayList<String>();
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		if ( currentUser == null )
			return result;
		
		try
		{
			if ( !GetPermission( "ADMIN" ) )
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
				
				if ( GetPermission( "STORE" ) )
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
			
			json = FrameworkDatabaseEngine.convert( rs );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return result;
		}
		
		result = new Gson().fromJson( json.toString(), Map.class );
		
		if ( returnOne || returnString )
		{
			Map<String, Object> one = result.get( 0 );
			result = new HashMap<String, Map<String, Object>>();
			result.put( "0", one );
			return result;
		}
		else
		{
			return result;
		}
	}
	
	public Map<String, Map<String, Object>> getMyLocations( boolean returnOne, boolean returnString )
	{
		return getMyLocations( returnOne, returnString, null );
	}
	
	public Map<String, Map<String, Object>> getMyLocations( boolean returnOne )
	{
		return getMyLocations( returnOne, false, null );
	}
	
	public Map<String, Map<String, Object>> getMyLocations()
	{
		return getMyLocations( false, false, null );
	}
	
	public Map<String, Map<String, Object>> getMyAccounts( boolean returnOne, boolean returnString, String whereAlt )
	{
		SqlConnector sql = fw.getCurrentSite().getDatabase();
		List<String> where = new ArrayList<String>();
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		String sqlWhere = "";
		JSONObject json;
		
		if ( whereAlt == null )
			whereAlt = "";
		
		if ( currentUser == null )
			return result;
		
		try
		{
			if ( !GetPermission( "ADMIN" ) )
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
			
			json = FrameworkDatabaseEngine.convert( rs );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return result;
		}
		
		result = new Gson().fromJson( json.toString(), Map.class );
		
		if ( returnOne || returnString )
		{
			Map<String, Object> one = result.get( 0 );
			result = new HashMap<String, Map<String, Object>>();
			result.put( "0", one );
			return result;
		}
		else
		{
			return result;
		}
	}
	
	public Map<String, Map<String, Object>> getMyAccounts( boolean returnOne, boolean returnString )
	{
		return getMyAccounts( returnOne, returnString, null );
	}
	
	public Map<String, Map<String, Object>> getMyAccounts( boolean returnOne )
	{
		return getMyAccounts( returnOne, false, null );
	}
	
	public Map<String, Map<String, Object>> getMyAccounts()
	{
		return getMyAccounts( false, false, null );
	}
}
