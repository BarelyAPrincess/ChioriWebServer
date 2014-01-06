package com.chiorichan.framework;

import com.chiorichan.user.User;

/**
 * User Service Wrapper allows scripts to safely access the User API without threat of getting NullPointers. 
 * @author Chiori Greene
 *
 */
public class UserServiceWrapper
{
	protected User currentUser;
	
	public UserServiceWrapper(User _currentUser)
	{
		currentUser = _currentUser;
	}
	
	public String getUserString( String key )
	{
		return getUserString( key, "" );
	}
	
	public String getUserString( String key, String def )
	{
		if ( currentUser == null )
			return def;
		
		return currentUser.getString( key, def );
	}
}