/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.helpers.AccountMetaData;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.account.helpers.LoginExceptionReasons;
import com.chiorichan.account.helpers.LookupAdapterException;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

public class SqlAdapter implements AccountLookupAdapter
{
	DatabaseEngine sql;
	String table;
	List<String> accountFields;
	
	public SqlAdapter() throws LookupAdapterException
	{
		sql = Loader.getDatabase();
		table = Loader.getConfig().getString( "accounts.lookupAdapter.table", "accounts" );
		accountFields = Loader.getConfig().getStringList( "accounts.lookupAdapter.fields", new ArrayList<String>() );
	}
	
	public SqlAdapter(Site site) throws LookupAdapterException
	{
		Validate.notNull( site );
		
		sql = site.getDatabase();
		table = site.getYaml().getString( "accounts.table", "accounts" );
		accountFields = site.getYaml().getStringList( "accounts.fields", new ArrayList<String>() );
	}
	
	public ResultSet getResultSet( String uid ) throws SQLException
	{
		if ( uid == null || uid.isEmpty() )
			return null;
		
		ResultSet rs = sql.query( "SELECT * FROM `accounts` WHERE `acctID` = '" + uid + "' LIMIT 1;" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			return null;
		
		return rs;
	}
	
	@Override
	public List<AccountMetaData> getAccounts()
	{
		List<AccountMetaData> metas = Lists.newArrayList();
		
		try
		{
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "`;" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				return Lists.newArrayList();
			
			do
			{
				AccountMetaData meta = new AccountMetaData();
				meta.setAll( DatabaseEngine.convertRow( rs ) );
				meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
				metas.add( meta );
			}
			while ( rs.next() );
		}
		catch ( SQLException e )
		{
			return metas;
		}
		
		return metas;
	}
	
	@Override
	public void saveAccount( AccountMetaData account )
	{
		
	}
	
	@Override
	public AccountMetaData reloadAccount( AccountMetaData account )
	{
		return null;
	}
	
	@Override
	public AccountMetaData loadAccount( String accountname ) throws LoginException
	{
		try
		{
			AccountMetaData meta = new AccountMetaData();
			
			if ( accountname == null || accountname.isEmpty() )
				throw new LoginException( LoginExceptionReasons.emptyUsername );
			
			Set<String> accountFieldSet = new HashSet<String>( accountFields );
			Set<String> accountColumnSet = new HashSet<String>();
			
			accountFieldSet.add( "acctId" );
			accountFieldSet.add( "username" );
			
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "` LIMIT 0;" );
			
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			
			do
			{
				for ( int i = 1; i < columnCount + 1; i++ )
				{
					accountColumnSet.add( rsmd.getColumnName( i ) );
				}
			}
			while ( rs.next() );
			
			String additionalAccountFields = "";
			for ( String f : accountFieldSet )
			{
				if ( !f.isEmpty() )
					if ( accountColumnSet.contains( f ) )
						additionalAccountFields += " OR `" + f + "` = '" + accountname + "'";
					else
						for ( String c : accountColumnSet )
						{
							if ( c.equalsIgnoreCase( f ) )
							{
								additionalAccountFields += " OR `" + c + "` = '" + accountname + "'";
								break;
							}
						}
			}
			
			rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginExceptionReasons.incorrectLogin );
			
			meta.setAll( DatabaseEngine.convertRow( rs ) );
			
			meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
			
			return meta;
		}
		catch ( SQLException e )
		{
			throw new LoginException( e );
		}
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
		try
		{
			sql.queryUpdate( "UPDATE `accounts` SET `lastactive` = '" + Common.getEpoch() + "', `lastlogin` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `accountID` = '" + account.getAccountId() + "'" );
		}
		catch ( SQLException e )
		{
			throw new LoginException( e );
		}
	}
	
	@Override
	public void failedLoginUpdate( Account account )
	{
		// TODO Update use as top reflect this failure.
		// sql.queryUpdate( "UPDATE `accounts` SET `lastactive` = '" + Common.getEpoch() + "', `lastloginfail` = 0, `numloginfail` = 0 WHERE `accountID` = '" + account.getAccountId() + "'" );
	}
	
	@Override
	public boolean matchAccount( Account account, String accountname )
	{
		AccountMetaData meta = account.getMetaData();
		
		for ( String f : accountFields )
		{
			if ( meta.getString( f ) != null && meta.getString( f ).equals( accountname ) )
				return true;
		}
		
		return false;
	}
}
