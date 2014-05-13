package com.chiorichan.user.builtin;

import java.io.File;

import com.chiorichan.framework.Site;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.LookupAdapterException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;
import com.chiorichan.util.FileUtil;

public class FileAdapter implements UserLookupAdapter
{
	File usersDirectory = null;
	
	public FileAdapter(String filebase, Site site) throws LookupAdapterException
	{
		usersDirectory = FileUtil.calculateFileBase( filebase, site );
		FileUtil.directoryHealthCheck( usersDirectory );
		
		
	}
	
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
