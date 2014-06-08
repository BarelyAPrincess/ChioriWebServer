package com.chiorichan.user.builtin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.FileFilterUtils;

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
import com.google.common.collect.Maps;

public class FileAdapter implements UserLookupAdapter
{
	File usersDirectory = null;
	List<String> userFields;
	Map<String, UserMetaData> preloaded = Maps.newConcurrentMap();
	
	public FileAdapter(Site site) throws LookupAdapterException
	{
		String filebase = site.getYaml().getString( "users.filebase", "[site].users" );
		
		usersDirectory = FileUtil.calculateFileBase( filebase, site );
		FileUtil.directoryHealthCheck( usersDirectory );
		
		userFields = site.getYaml().getStringList( "users.fields", new ArrayList<String>() );
		
		userFields.add( "userId" );
		userFields.add( "username" );
		
		checkForFiles();
	}
	
	public void checkForFiles()
	{
		for ( File f : usersDirectory.listFiles() )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
				if ( !preloaded.containsKey( f.getName() ) )
					preloaded.put( f.getName(), loadFromFile( f ) );
	}
	
	public UserMetaData loadFromFile( File absoluteFilePath )
	{
		UserMetaData meta = new UserMetaData();
		
		if ( !absoluteFilePath.exists() )
			return null;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( absoluteFilePath );
		
		if ( yser == null )
			return null;
		
		// Save the file location for later
		meta.set( "file", absoluteFilePath );
		
		for ( String key : yser.getKeys( false ) )
		{
			meta.set( key, yser.get( key ) );
		}
		
		// meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
		
		return meta;
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
			yser.save( (File) user.getObject( "relPath" ) );
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
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( (File) user.getObject( "relPath" ) );
		
		if ( yser == null )
			return user;
		
		for ( String key : yser.getKeys( false ) )
		{
			user.set( key, yser.get( key ) );
		}
		
		// meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
		
		return user;
	}
	
	public UserMetaData createUser( String username, String userId ) throws LoginException
	{
		if ( userId == null || userId.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		checkForFiles();
		
		for ( Entry<String, UserMetaData> e : preloaded.entrySet() )
		{
			UserMetaData meta = e.getValue();
			
			for ( String f : userFields )
				if ( meta.getObject( f ) != null && ( meta.getString( f ).equalsIgnoreCase( userId ) || meta.getString( f ).equalsIgnoreCase( username ) ) )
					throw new LoginException( LoginExceptionReasons.userExists );
		}
		
		UserMetaData meta = new UserMetaData();
		
		String relPath = userId + ".yaml";
		
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
		
		for ( String key : yser.getKeys( false ) )
		{
			meta.set( key, yser.get( key ) );
		}
		
		return meta;
	}
	
	@Override
	public UserMetaData loadUser( String user ) throws LoginException
	{
		if ( user == null || user.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		checkForFiles();
		
		UserMetaData meta = null;
		
		for ( Entry<String, UserMetaData> e : preloaded.entrySet() )
		{
			UserMetaData meta1 = e.getValue();
			
			for ( String f : userFields )
				if ( meta1.getObject( f ) != null && meta1.getString( f ).equalsIgnoreCase( user ) )
				{
					meta = meta1;
					break;
				}
		}
		
		if ( meta == null )
			throw new LoginException( LoginExceptionReasons.incorrectLogin );
		
		meta = reloadUser( meta );
		
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
		
		File relPath = (File) user.getMetaData().getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
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
		
		File relPath = (File) user.getMetaData().getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
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
