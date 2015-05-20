/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account.types;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;
import com.chiorichan.account.AccountContext;
import com.chiorichan.account.AccountManager;
import com.chiorichan.account.AccountMeta;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.event.AccountLookupEvent;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.event.EventHandler;
import com.chiorichan.util.CommonFunc;
import com.google.common.collect.Maps;

/**
 * Handles Accounts that are loaded from SQL
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class SqlTypeCreator extends AccountTypeCreator
{
	public static final SqlTypeCreator INSTANCE = new SqlTypeCreator();
	
	final DatabaseEngine sql;
	final String table;
	final List<String> accountFields;
	private boolean enabled = true;
	
	public SqlTypeCreator()
	{
		sql = Loader.getDatabase();
		
		if ( sql == null )
		{
			AccountManager.getLogger().warning( "We could not enable the `SqlLoginHandler` due to an unconfigured SQL Database, which is required." );
			enabled = false;
		}
		else
			AccountManager.getLogger().info( "The `SqlLoginHandler` was enabled successfully with database '" + Loader.getDatabase() + "'" );
		
		// TODO Check if the database has the right tables.
		// TODO Add the ability to select which database to use for logins
		
		table = Loader.getConfig().getString( "accounts.sqlType.table", "accounts" );
		accountFields = Loader.getConfig().getStringList( "accounts.sqlType.fields", new ArrayList<String>() );
	}
	
	
	@Override
	public void save( AccountMeta meta )
	{
		try
		{
			Set<String> columnSet = new HashSet<String>( sql.getTableColumnNames( table ) );
			Map<String, Object> metaData = meta.getMeta();
			Map<String, Object> toSave = Maps.newTreeMap();
			Map<String, Class<?>> newColumns = Maps.newHashMap();
			
			for ( Entry<String, Object> e : metaData.entrySet() )
			{
				String key = e.getKey();
				
				if ( !"acctId".equalsIgnoreCase( key ) && !"password".equalsIgnoreCase( key ) )
				{
					boolean found = false;
					
					for ( String s : columnSet )
						if ( s.equalsIgnoreCase( key ) )
							found = true;
					
					if ( !found )
					{
						// There is no column for this key
						columnSet.add( key );
						newColumns.put( key, e.getValue().getClass() );
					}
					
					toSave.put( key, e.getValue() );
				}
			}
			
			Loader.getLogger().debug( "To Save: " + toSave );
			
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
		catch ( SQLException e )
		{
			throw new AccountException( e, meta );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
	
	@Override
	public void reload( AccountMeta meta )
	{
		try
		{
			readAccount( meta.getAcctId() );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void preLogin( AccountMeta meta, AccountPermissible via, String acctId, Object... creds )
	{
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( CommonFunc.getEpoch() - 1800 ) )
				throw new AccountException( AccountResult.UNDER_ATTACK );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new AccountException( AccountResult.ACCOUNT_NOT_ACTIVATED );
	}
	
	@Override
	public void successLogin( AccountMeta meta )
	{
		try
		{
			sql.queryUpdate( "UPDATE `accounts` SET `lastActive` = '" + CommonFunc.getEpoch() + "', `lastLogin` = '" + CommonFunc.getEpoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `acctId` = '" + meta.getAcctId() + "'" );
		}
		catch ( SQLException e )
		{
			throw new AccountException( e );
		}
	}
	
	@Override
	public void failedLogin( AccountMeta meta, AccountResult result )
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
	public String getDisplayName( AccountMeta meta )
	{
		if ( meta.getString( "fname" ) != null && !meta.getString( "fname" ).isEmpty() && meta.getString( "name" ) != null && !meta.getString( "name" ).isEmpty() )
			return meta.getString( "fname" ) + " " + meta.getString( "name" );
		
		if ( meta.getString( "name" ) != null && !meta.getString( "name" ).isEmpty() )
			return meta.getString( "name" );
		
		if ( meta.getString( "email" ) != null && !meta.getString( "email" ).isEmpty() )
			return meta.getString( "email" );
		
		return null;
	}
	
	@Override
	public boolean isEnabled()
	{
		return enabled;
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
	
	public AccountContext readAccount( String acctId ) throws AccountException, SQLException
	{
		AccountContextImpl context = new AccountContextImpl( this, AccountType.SQL );
		
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_ACCTID );
		
		Set<String> accountFieldSet = new HashSet<String>( accountFields );
		Set<String> accountColumnSet = new HashSet<String>( sql.getTableColumnNames( table ) );
		
		accountFieldSet.add( "acctId" );
		accountFieldSet.add( "username" );
		
		String additionalAccountFields = "";
		for ( String f : accountFieldSet )
		{
			if ( !f.isEmpty() )
				if ( accountColumnSet.contains( f ) )
					additionalAccountFields += " OR `" + f + "` = '" + acctId + "'";
				else
					for ( String c : accountColumnSet )
					{
						if ( c.equalsIgnoreCase( f ) )
						{
							additionalAccountFields += " OR `" + c + "` = '" + acctId + "'";
							break;
						}
					}
		}
		
		ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			throw new AccountException( AccountResult.INCORRECT_LOGIN, context );
		
		context.setAcctId( rs.getString( "acctId" ) );
		context.setValues( DatabaseEngine.convertRow( rs ) );
		
		return context;
	}
	
	@EventHandler
	public void onAccountLookupEvent( AccountLookupEvent event )
	{
		try
		{
			event.setResult( readAccount( event.getAcctId() ), AccountResult.LOGIN_SUCCESS );
		}
		catch ( SQLException e )
		{
			// e.printStackTrace();
		}
		catch ( AccountException e )
		{
			event.setResult( e.getContext(), e.getResult() );
		}
	}
	
	@Override
	public List<String> getLoginKeys()
	{
		return accountFields;
	}
	
	/*
	 * @Override
	 * public List<AccountMeta> getAccounts()
	 * {
	 * List<AccountMeta> metas = Lists.newArrayList();
	 * 
	 * try
	 * {
	 * ResultSet rs = sql.query( "SELECT * FROM `" + table + "`;" );
	 * 
	 * if ( rs == null || sql.getRowCount( rs ) < 1 )
	 * return Lists.newArrayList();
	 * 
	 * do
	 * {
	 * AccountMeta meta = new AccountMeta();
	 * meta.setAll( DatabaseEngine.convertRow( rs ) );
	 * meta.set( "displayName", ( rs.getString( "fname" ).isEmpty() ) ? rs.getString( "name" ) : rs.getString( "fname" ) + " " + rs.getString( "name" ) );
	 * metas.add( meta );
	 * }
	 * while ( rs.next() );
	 * }
	 * catch ( SQLException e )
	 * {
	 * return metas;
	 * }
	 * 
	 * return metas;
	 * }
	 */
}
