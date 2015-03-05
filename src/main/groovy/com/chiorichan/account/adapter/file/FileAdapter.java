/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.filefilter.FileFilterUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.util.Common;
import com.chiorichan.util.FileUtil;
import com.google.common.collect.Maps;

public class FileAdapter implements AccountLookupAdapter
{
	File accountsDirectory = null;
	List<String> accountFields;
	Map<String, AccountMetaData> preloaded = Maps.newConcurrentMap();
	
	public FileAdapter()
	{
		String fileBase = Loader.getConfig().getString( "accounts.lookupAdapter.filebase", "accounts" );
		accountsDirectory = new File( fileBase );
		
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
		File[] files = accountsDirectory.listFiles();
		
		if ( files == null )
			return;
		
		for ( File f : files )
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
		
		return meta;
	}
	
	@Override
	public void saveAccount( AccountMetaData meta )
	{
		if ( meta == null )
			return;
		
		if ( !meta.containsKey( "relPath" ) )
			meta.set( "relPath", meta.getAcctId() + ".yaml" );
		
		YamlConfiguration yser = new YamlConfiguration();
		
		for ( String key : meta.getKeys() )
			yser.set( key, meta.getObject( key ) );
		
		try
		{
			yser.save( ( File ) meta.getObject( "relPath" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public AccountMetaData reloadAccount( AccountMetaData meta )
	{
		if ( meta == null || !meta.containsKey( "relPath" ) )
			return meta;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( ( File ) meta.getObject( "relPath" ) );
		
		if ( yser == null )
			return meta;
		
		for ( String key : yser.getKeys( false ) )
		{
			meta.set( key, yser.get( key ) );
		}
		
		return meta;
	}
	
	public AccountMetaData createAccount( String accountname, String acctId ) throws LoginException
	{
		if ( acctId == null || acctId.isEmpty() )
			throw new LoginException( LoginExceptionReason.emptyUsername );
		
		checkForFiles();
		
		for ( Entry<String, AccountMetaData> e : preloaded.entrySet() )
		{
			AccountMetaData meta = e.getValue();
			
			for ( String f : accountFields )
				if ( meta.getObject( f ) != null && ( meta.getString( f ).equalsIgnoreCase( acctId ) || meta.getString( f ).equalsIgnoreCase( accountname ) ) )
					throw new LoginException( LoginExceptionReason.accountExists );
		}
		
		AccountMetaData meta = new AccountMetaData();
		
		String relPath = acctId + ".yaml";
		
		YamlConfiguration yser = new YamlConfiguration();
		
		yser.set( "accountId", acctId.toLowerCase() );
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
	public AccountMetaData readAccount( String account ) throws LoginException
	{
		if ( account == null || account.isEmpty() )
			throw new LoginException( LoginExceptionReason.emptyUsername );
		
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
			throw new LoginException( LoginExceptionReason.incorrectLogin );
		
		meta = reloadAccount( meta );
		
		return meta;
	}
	
	@Override
	public void failedLoginUpdate( AccountMetaData meta, LoginExceptionReason reason )
	{
		if ( meta != null && meta.containsKey( "relPath" ) )
			return;
		
		File relPath = ( File ) meta.getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
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
	public Class<? extends Account> getAccountClass()
	{
		return FileAccount.class;
	}
}
