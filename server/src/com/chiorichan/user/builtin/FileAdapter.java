package com.chiorichan.user.builtin;

import com.chiorichan.user.LoginException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;

public class FileAdapter implements UserLookupAdapter
{
	@Override
	public void saveUser( UserMetaData user )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public UserMetaData reloadUser( UserMetaData user )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserMetaData loadUser( String user ) throws LoginException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void preLoginCheck( User user ) throws LoginException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postLoginCheck( User user ) throws LoginException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void failedLoginUpdate( User user )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean matchUser( User user, String username )
	{
		// TODO Auto-generated method stub
		return false;
	}
}
