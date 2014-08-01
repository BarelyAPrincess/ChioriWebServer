package com.chiorichan.framework;

import com.chiorichan.account.bases.Account;

/**
 * User Service Wrapper allows scripts to safely access the User API without threat of getting NullPointers. 
 * @author Chiori Greene
 *
 */
public class AccountServiceWrapper
{
	protected Account currentUser;
	
	public AccountServiceWrapper(Account _currentUser)
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