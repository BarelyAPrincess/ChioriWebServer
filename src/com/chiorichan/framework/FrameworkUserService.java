package com.chiorichan.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.user.User;

public class FrameworkUserService
{
	protected Framework fw;
	protected User currentUser = null;
	
	public FrameworkUserService(Framework fw0)
	{
		fw = fw0;
	}
	
	public boolean getUserState()
	{
		return false;
	}
	
	public String getString( String key )
	{
		return "";
	}
	
	public boolean hasPermission( String key )
	{
		return false;
	}
	
	public boolean initalize( String reqLevel )
	{
		if ( fw.getCurrentSite().getUserList() == null )
			return true;
		
		HttpServletRequest req = fw.getRequest();
		
		String username = req.getParameter( "user" );
		String password = req.getParameter( "pass" );
		String target = req.getParameter( "target" );
		
		if ( req.getParameter( "logout" ) != null && !req.getParameter( "logout" ).isEmpty() )
		{
			logout();
			
			if ( target.isEmpty() )
			{
				target = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
			}
			
			fw.getServer().dummyRedirect( target );
		}
		
		if ( username != null && password != null && !username.isEmpty() && !password.isEmpty() )
		{
			User user = fw.getCurrentSite().getUserList().validateUser( fw, username, password );
			
			Loader.getConsole().info( "User: " + user );
			
			if ( user != null && user.isValid() )
			{
				currentUser = user;
				
				String loginPost = ( target.isEmpty() ) ? fw.getCurrentSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				Loader.getConsole().info( "Login Success: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginPost );
			}
			else if ( user == null )
			{
				return false;
			}
			else
			{
				String loginForm = fw.getCurrentSite().getYaml().getString( "scripts.login-form", "/login" );
				
				Loader.getConsole().warning( "Login Failed: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginForm + "?msg=" + user.getLastError() + "&target=" + target );
			}
		}
		else
		{
			username = (String) fw.getRequest().getSession().getAttribute( "User" );
			password = (String) fw.getRequest().getSession().getAttribute( "Pass" );
			
			User user = fw.getCurrentSite().getUserList().validateUser( fw, username, password );
			
			if ( user != null && user.isValid() )
			{
				currentUser = user;
				
				String loginPost = ( target.isEmpty() ) ? fw.getCurrentSite().getYaml().getString( "scripts.login-post", "/panel" ) : target;
				
				Loader.getConsole().info( "Login Success: Username \"" + username + "\", Password \"" + password + "\", UserId \"" + user.getUserId() + "\", Display Name \"" + user.getDisplayName() + "\", Display Level \"" + user.getDisplayLevel() + "\"" );
				fw.getServer().dummyRedirect( loginPost );
			}
			else
			{
				Loader.getConsole().warning( "Login Status: No Valid Login Present" );
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
		
		return true;
	}
	
	private void logout()
	{
		fw.getRequest().getSession().invalidate();
		Loader.getConsole().info( "User Logout" );
	}
	
	public boolean GetPermission( String permName, String idenifier )
	{
		return GetPermission( Arrays.asList( permName ), idenifier );
	}
	
	/*
	 * This function checks the users permission level againts the permissions table for if the requested permission is
	 * allowed by Current User.
	 */
	public boolean GetPermission( List<String> permName, String idenifier )
	{
		SqlConnector sql = fw.getCurrentSite().sql;
		
		if ( permName == null || permName.isEmpty() )
			permName = Arrays.asList( "ROOT" );
		
		String userLevel = null;
		
		if ( currentUser != null && ( idenifier == null || idenifier.isEmpty() ) )
		{
			idenifier = currentUser.getUserName();
			userLevel = currentUser.getUserLevel();
		}
		
		if ( idenifier == null || idenifier.isEmpty() )
			return false;
		
		if ( userLevel == null || userLevel.isEmpty() )
		{
			Map<String, Object> result = sql.selectOne( "users", "username", idenifier );
			if ( result == null )
				return false;
			
			userLevel = (String) result.get( "userlevel" );
		}
		
		Map<String, Object> perm = sql.selectOne( "accounts_access", "accessID", userLevel );
		
		if ( perm == null )
			return false;
		
		List<String> permList = Arrays.asList( ( (String) perm.get( "permissions" ) ).split( "|" ) );
		
		// String title = (String) perm.get( "title" );
		
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
			
			if ( granted )
				return true; // Return true if one of the requested permission names exists in users allowed permissions
									// list.
		}
		
		return false;
	}
	
	public User getCurrentUser()
	{
		return currentUser;
	}
}
