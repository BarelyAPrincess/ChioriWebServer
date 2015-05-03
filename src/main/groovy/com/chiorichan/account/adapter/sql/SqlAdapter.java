/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.adapter.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMetaData;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.lang.LoginException;
import com.chiorichan.account.lang.LoginExceptionReason;
import com.chiorichan.account.lang.LookupAdapterException;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.site.Site;
import com.chiorichan.util.CommonFunc;
import com.chiorichan.util.StringFunc;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
	
	public SqlAdapter( Site site ) throws LookupAdapterException
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
	public void saveAccount( AccountMetaData meta ) throws Exception
	{
		Set<String> columnSet = new HashSet<String>( sql.getTableColumnNames( table ) );
		Map<String, Object> metaData = meta.getMetaDataMap();
		Map<String, Object> toSave = Maps.newTreeMap();
		Map<String, Class<?>> newColumns = Maps.newHashMap();
		
		for ( Entry<String, Object> e : metaData.entrySet() )
		{
			String key = e.getKey();
			
			if ( "acctId".equalsIgnoreCase( key ) )
				continue;
			
			/*
			 * Temp until all passwords are stored with encryption
			 */
			if ( "password".equalsIgnoreCase( key ) && StringFunc.isValidMD5( ( String ) e.getValue() ) )
				continue;
			
			for ( String s : columnSet )
				if ( s.equalsIgnoreCase( key ) )
				{
					toSave.put( s, e.getValue() );
					continue;
				}
			
			// There is no column for this key
			columnSet.add( key );
			newColumns.put( key, e.getValue().getClass() );
			toSave.put( key, e.getValue() );
		}
		
		if ( newColumns.size() > 0 )
			for ( Entry<String, Class<?>> c : newColumns.entrySet() )
				try
				{
					sql.addColumn( table, c.getKey(), c.getValue() );
				}
				catch ( SQLException e )
				{
					AccountManager.getLogger().severe( "Could not create a new column for key `" + c.getKey() + "` with class `" + c.getValue() + "`", e );
					toSave.remove( c.getKey() );
				}
		
		ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE `acctId` = '" + meta.getAcctId() + "' LIMIT 1;" );
		
		if ( sql.getRowCount( rs ) > 0 )
			sql.update( table, toSave, "`acctId` = '" + meta.getAcctId() + "'", 1 );
		else
			sql.insert( table, toSave );
	}
	
	@Override
	public void reloadAccount( AccountMetaData meta ) throws Exception
	{
		meta.mergeData( readAccount( meta.getAcctId() ) );
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
			Set<String> accountColumnSet = new HashSet<String>( sql.getTableColumnNames( table ) );
			
			accountFieldSet.add( "acctId" );
			accountFieldSet.add( "username" );
			
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
			
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
			
			if ( rs == null || sql.getRowCount( rs ) < 1 )
				throw new LoginException( LoginExceptionReason.incorrectLogin );
			
			meta.setAll( DatabaseEngine.convertRow( rs ) );
			
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
			sql.queryUpdate( "UPDATE `" + table + "` SET `lastActive` = '" + CommonFunc.getEpoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `acctID` = '" + meta.getAcctId() + "'" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		meta.set( "lastActive", CommonFunc.getEpoch() );
		meta.set( "lastLoginFail", 0 );
		meta.set( "numLoginFail", 0 );
	}
	
	@Override
	public Class<? extends Account> getAccountClass()
	{
		return SqlAccount.class;
	}
}
