/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.LoginException;
import com.chiorichan.account.LoginExceptionReason;
import com.chiorichan.account.LookupAdapterException;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.framework.Site;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

public class SqlAdapter implements AccountLookupAdapter
{
	public final DatabaseEngine sql;
	public final String table;
	public final List<String> accountFields;
	
	public SqlAdapter()
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
	public AccountMetaData readAccount( String id ) throws LoginException
	{
		try
		{
			AccountMetaData meta = new AccountMetaData();
			
			if ( id == null || id.isEmpty() )
				throw new LoginException( LoginExceptionReason.emptyUsername );
			
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
						additionalAccountFields += " OR `" + f + "` = '" + id + "'";
					else
						for ( String c : accountColumnSet )
						{
							if ( c.equalsIgnoreCase( f ) )
							{
								additionalAccountFields += " OR `" + c + "` = '" + id + "'";
								break;
							}
						}
			}
			
			rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginExceptionReason.incorrectLogin );
			
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
	public void failedLoginUpdate( AccountMetaData meta, LoginExceptionReason reason )
	{
		try
		{
			sql.queryUpdate( "UPDATE `" + table + "` SET `lastActive` = '" + Common.getEpoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `accountID` = '" + meta.getAcctId() + "'" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		meta.set( "lastActive", Common.getEpoch() );
		meta.set( "lastLoginFail", 0 );
		meta.set( "numLoginFail", 0 );
	}

	@Override
	public Class<? extends Account<SqlAdapter>> getAccountClass()
	{
		return SqlAccount.class;
	}
}
