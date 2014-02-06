package com.chiorichan.user.builtin;

import com.chiorichan.framework.Site;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;

/**
 * Provided so that any site can have custom places to store login information.
 * mySql, sqLite, file, etc.
 * 
 * @author Chiori Greene
 */
public interface UserLookupAdapter
{
	/**
	 * Method should check if everything is functioning correctly.
	 * 
	 * @param site
	 * @return isAdapterValid
	 */
	public boolean isAdapterValid( Site site );
	
	/**
	 * Attempt to serialize provided user.
	 * Use of the user instance may continue.
	 */
	public void saveUser( UserMetaData user );
	
	/**
	 * Attempt to reload details regarding this user.
	 * @return 
	 */
	public UserMetaData reloadUser( UserMetaData user );
	
	/**
	 * Attempt to load a user.
	 * 
	 * @throws LoginException
	 */
	public UserMetaData loadUser( String user ) throws LoginException;
	
	/**
	 * Called before the UserManager makes the login offical.
	 */
	public void preLoginCheck( User user ) throws LoginException ;
	
	/**
	 * Called as the last line before user returned to scripts.
	 */
	public void postLoginCheck( User user ) throws LoginException;
	
	/**
	 * Update any security mechs of failed login
	 */
	public void failedLoginUpdate( User user );
	
	/**
	 * Called from UserManager to determine if User matches Username. Usually used to search the users array.
	 */
	public boolean matchUser( User user, String username );
}
