package com.chiorichan.user.builtin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.user.LoginException;
import com.chiorichan.user.LoginExceptionReasons;
import com.chiorichan.user.LookupAdapterException;
import com.chiorichan.user.User;
import com.chiorichan.user.UserMetaData;
import com.chiorichan.util.Common;
import com.chiorichan.util.FileUtil;

public class FileAdapter implements UserLookupAdapter
{
	File usersDirectory = null;
	List<String> userFields;
	YamlConfiguration adapterConfig = new YamlConfiguration();
	
	public FileAdapter(Site site) throws LookupAdapterException
	{
		String filebase = site.getYaml().getString( "users.filebase", "[site].users" );
		
		usersDirectory = FileUtil.calculateFileBase( filebase, site );
		FileUtil.directoryHealthCheck( usersDirectory );
		
		userFields = site.getYaml().getStringList( "users.fields", new ArrayList<String>() );
		
		userFields.add( "userId" );
		userFields.add( "username" );
		
		adapterConfig = YamlConfiguration.loadConfiguration( new File( usersDirectory, "adapter.yaml" ) );
	}
	
	@Override
	public void saveUser( UserMetaData user )
	{
		if ( user == null )
			return;
		
		if ( !user.containsKey( "relPath" ) )
			user.set( "relPath", user.getUserId() + ".yaml" );
		
		YamlConfiguration yser = new YamlConfiguration();
		
		for ( String key : user.getKeys() )
		{
			yser.set( key, user.getObject( key ) );
		}
		
		try
		{
			yser.save( new File( usersDirectory, user.getString( "relPath" ) ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public UserMetaData reloadUser( UserMetaData user )
	{
		if ( user == null || !user.containsKey( "relPath" ) )
			return user;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( new File( usersDirectory, user.getString( "relPath" ) ) );
		
		if ( yser == null )
			return user;
		
		for ( String key : yser.getKeys( false ) )
		{
			user.set( key, yser.get( key ) );
		}
		
		return user;
	}
	
	public UserMetaData createUser( String username, String userId ) throws LoginException
	{
		UserMetaData meta = new UserMetaData();
		
		if ( userId == null || userId.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		for ( String key : adapterConfig.getConfigurationSection( "ref" ).getKeys( false ) )
		{
			ConfigurationSection section = adapterConfig.getConfigurationSection( "ref." + key );
			
			for ( String f : userFields )
			{
				if ( section.getString( f ) != null && ( section.getString( f ).equalsIgnoreCase( userId ) || section.getString( f ).equalsIgnoreCase( username ) ) )
				{
					throw new LoginException( LoginExceptionReasons.userExists );
				}
			}
		}
		
		String relPath = userId + ".yaml";
		String acPath = "ref." + userId + ".";
		
		YamlConfiguration yser = new YamlConfiguration();
		
		yser.set( "userId", userId.toLowerCase() );
		yser.set( "username", username.toLowerCase() );
		yser.set( "actnum", WebUtils.randomNum( 8 ) );
		
		try
		{
			yser.save( new File( usersDirectory, relPath ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new LoginException( e );
		}
		
		adapterConfig.set( acPath + "relPath", relPath );
		adapterConfig.set( acPath + "userId", userId.toLowerCase() );
		adapterConfig.set( acPath + "username", username.toLowerCase() );
		
		return meta;
	}
	
	@Override
	public UserMetaData loadUser( String user ) throws LoginException
	{
		UserMetaData meta = new UserMetaData();
		
		if ( user == null || user.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		ConfigurationSection found = null;
		
		for ( String key : adapterConfig.getConfigurationSection( "ref" ).getKeys( false ) )
		{
			ConfigurationSection section = adapterConfig.getConfigurationSection( "ref." + key );
			
			for ( String f : userFields )
			{
				if ( section.getString( f ) != null && section.getString( f ).equalsIgnoreCase( user ) )
				{
					found = section;
					break;
				}
			}
		}
		
		if ( found == null )
			throw new LoginException( LoginExceptionReasons.incorrectLogin );
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( new File( usersDirectory, found.getString( "relPath" ) ) );
		
		if ( yser == null )
			throw new LoginException( LoginExceptionReasons.incorrectLogin );
		
		// Save the file location for later
		meta.set( "file", new File( usersDirectory, user + ".dat" ) );
		
		for ( String key : found.getKeys( false ) )
		{
			meta.set( key, found.get( key ) );
		}
		
		for ( String key : yser.getKeys( false ) )
		{
			meta.set( key, yser.get( key ) );
		}
		
		// meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
		
		return meta;
	}
	
	@Override
	public void preLoginCheck( User user ) throws LoginException
	{
		UserMetaData meta = user.getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReasons.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReasons.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck( User user ) throws LoginException
	{
		if ( user == null || !user.getMetaData().containsKey( "relPath" ) )
			return;
		
		File relPath = new File( usersDirectory, user.getMetaData().getString( "relPath" ) );
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
		UserMetaData meta = user.getMetaData();
		
		int lastactive = Common.getEpoch();
		int lastlogin = Common.getEpoch();
		int lastloginfail = 0;
		int numloginfail = 0;
		
		meta.set( "lastactive", lastactive );
		meta.set( "lastlogin", lastlogin );
		meta.set( "lastloginfail", lastloginfail );
		meta.set( "numloginfail", numloginfail );
		
		yser.set( "lastactive", lastactive );
		yser.set( "lastlogin", lastlogin );
		yser.set( "lastloginfail", lastloginfail );
		yser.set( "numloginfail", numloginfail );
		
		try
		{
			yser.save( relPath );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void failedLoginUpdate( User user )
	{
		if ( user == null || !user.getMetaData().containsKey( "relPath" ) )
			return;
		
		File relPath = new File( usersDirectory, user.getMetaData().getString( "relPath" ) );
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
		UserMetaData meta = user.getMetaData();
		
		int lastloginfail = Common.getEpoch();
		int numloginfail = meta.getInteger( "numloginfail", 0 ) + 1;
		
		meta.set( "lastloginfail", lastloginfail );
		meta.set( "numloginfail", numloginfail );
		
		yser.set( "lastloginfail", lastloginfail );
		yser.set( "numloginfail", numloginfail );
		
		try
		{
			yser.save( relPath );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean matchUser( User user, String username )
	{
		UserMetaData meta = user.getMetaData();
		
		for ( String f : userFields )
		{
			if ( meta.getString( f ).equals( username ) )
				return true;
		}
		
		return false;
	}
}
