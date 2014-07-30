package com.chiorichan.account.adapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.account.helpers.LoginExceptionReasons;
import com.chiorichan.account.helpers.LookupAdapterException;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.util.Common;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class FileAdapter implements AccountLookupAdapter
{
	File accountsDirectory = null;
	List<String> accountFields;
	Map<String, AccountMetaData> preloaded = Maps.newConcurrentMap();
	
	public FileAdapter() throws LookupAdapterException
	{
		String fileBase = Loader.getConfig().getString( "accounts.lookupAdapter.filebase", "accounts" );
		
		if ( fileBase.startsWith( "/" ) )
			accountsDirectory = new File( fileBase );
		else
			accountsDirectory = new File( Loader.getRoot(), fileBase );
		
		FileUtil.directoryHealthCheck( accountsDirectory );
		
		accountFields = Loader.getConfig().getStringList( "accounts.lookupAdapter.fields", new ArrayList<String>() );
		
		accountFields.add( "accountId" );
		accountFields.add( "accountname" );
		
		checkForFiles();
	}
	
	@Override
	public List<AccountMetaData> getAccounts()
	{
		checkForFiles();
		return new ArrayList<AccountMetaData>( preloaded.values() );
	}
	
	public void checkForFiles()
	{
		for ( File f : accountsDirectory.listFiles() )
			if ( FileFilterUtils.and( FileFilterUtils.suffixFileFilter( "yaml" ), FileFilterUtils.fileFileFilter() ).accept( f ) )
				if ( !preloaded.containsKey( f.getName() ) )
					preloaded.put( f.getName(), loadFromFile( f ) );
	}
	
	public AccountMetaData loadFromFile( File absoluteFilePath )
	{
		AccountMetaData meta = new AccountMetaData();
		
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
	public void saveAccount( AccountMetaData account )
	{
		if ( account == null )
			return;
		
		if ( !account.containsKey( "relPath" ) )
			account.set( "relPath", account.getAccountId() + ".yaml" );
		
		YamlConfiguration yser = new YamlConfiguration();
		
		for ( String key : account.getKeys() )
		{
			yser.set( key, account.getObject( key ) );
		}
		
		try
		{
			yser.save( (File) account.getObject( "relPath" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public AccountMetaData reloadAccount( AccountMetaData account )
	{
		if ( account == null || !account.containsKey( "relPath" ) )
			return account;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( (File) account.getObject( "relPath" ) );
		
		if ( yser == null )
			return account;
		
		for ( String key : yser.getKeys( false ) )
		{
			account.set( key, yser.get( key ) );
		}
		
		// meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
		
		return account;
	}
	
	public AccountMetaData createAccount( String accountname, String accountId ) throws LoginException
	{
		if ( accountId == null || accountId.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		checkForFiles();
		
		for ( Entry<String, AccountMetaData> e : preloaded.entrySet() )
		{
			AccountMetaData meta = e.getValue();
			
			for ( String f : accountFields )
				if ( meta.getObject( f ) != null && ( meta.getString( f ).equalsIgnoreCase( accountId ) || meta.getString( f ).equalsIgnoreCase( accountname ) ) )
					throw new LoginException( LoginExceptionReasons.accountExists );
		}
		
		AccountMetaData meta = new AccountMetaData();
		
		String relPath = accountId + ".yaml";
		
		YamlConfiguration yser = new YamlConfiguration();
		
		yser.set( "accountId", accountId.toLowerCase() );
		yser.set( "accountname", accountname );
		yser.set( "actnum", WebUtils.randomNum( 8 ) );
		
		try
		{
			yser.save( new File( accountsDirectory, relPath ) );
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
	public AccountMetaData loadAccount( String account ) throws LoginException
	{
		if ( account == null || account.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		checkForFiles();
		
		AccountMetaData meta = null;
		
		for ( Entry<String, AccountMetaData> e : preloaded.entrySet() )
		{
			AccountMetaData meta1 = e.getValue();
			
			for ( String f : accountFields )
				if ( meta1.getObject( f ) != null && meta1.getString( f ).equalsIgnoreCase( account ) )
				{
					meta = meta1;
					break;
				}
		}
		
		if ( meta == null )
			throw new LoginException( LoginExceptionReasons.incorrectLogin );
		
		meta = reloadAccount( meta );
		
		return meta;
	}
	
	@Override
	public void preLoginCheck( Account account ) throws LoginException
	{
		AccountMetaData meta = account.getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReasons.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReasons.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck( Account account ) throws LoginException
	{
		if ( account == null || !account.getMetaData().containsKey( "relPath" ) )
			return;
		
		File relPath = (File) account.getMetaData().getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
		AccountMetaData meta = account.getMetaData();
		
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
	public void failedLoginUpdate( Account account )
	{
		if ( account == null || !account.getMetaData().containsKey( "relPath" ) )
			return;
		
		File relPath = (File) account.getMetaData().getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
		AccountMetaData meta = account.getMetaData();
		
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
	public boolean matchAccount( Account account, String accountname )
	{
		AccountMetaData meta = account.getMetaData();
		
		for ( String f : accountFields )
		{
			if ( meta.getString( f ).equals( accountname ) )
				return true;
		}
		
		return false;
	}
}
