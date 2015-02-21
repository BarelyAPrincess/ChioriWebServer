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

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.util.Common;

public class FileAccount extends Account<FileAdapter>
{
	public FileAccount( AccountMetaData meta, FileAdapter adapter ) throws LoginException
	{
		super( meta, adapter );
	}
	
	@Override
	public void preLoginCheck() throws LoginException
	{
		AccountMetaData meta = getMetaData();
		
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Common.getEpoch() - 1800 ) )
				throw new LoginException( LoginExceptionReason.underAttackPleaseWait );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new LoginException( LoginExceptionReason.accountNotActivated );
	}
	
	@Override
	public void postLoginCheck() throws LoginException
	{
		if ( !getMetaData().containsKey( "relPath" ) )
			return;
		
		File relPath = ( File ) getMetaData().getObject( "relPath" );
		
		if ( relPath == null )
			return;
		
		YamlConfiguration yser = YamlConfiguration.loadConfiguration( relPath );
		
		if ( yser == null )
			return;
		
		int lastactive = Common.getEpoch();
		int lastlogin = Common.getEpoch();
		int lastloginfail = 0;
		int numloginfail = 0;
		
		metaData.set( "lastactive", lastactive );
		metaData.set( "lastlogin", lastlogin );
		metaData.set( "lastloginfail", lastloginfail );
		metaData.set( "numloginfail", numloginfail );
		
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
	public boolean isYou( String id )
	{
		for ( String f : lookupAdapter.accountFields )
		{
			if ( metaData.getString( f ).equals( id ) )
				return true;
		}
		
		return false;
	}
	
	@Override
	public String getPassword()
	{
		return getString( "password" );
	}
	
	@Override
	public String getDisplayName()
	{
		return ( getString( "fname" ).isEmpty() ) ? getString( "name" ) : getString( "fname" ) + " " + getString( "name" );
	}
	
	@Override
	public String getUsername()
	{
		return getString( "username" );
	}
	
	@Override
	public boolean isValid()
	{
		return metaData.hasMinimumData();
	}
}
