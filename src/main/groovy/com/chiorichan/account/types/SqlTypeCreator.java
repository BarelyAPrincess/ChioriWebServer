/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.chiorichan.database.SqlTableColumns;
import com.chiorichan.event.EventHandler;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.tasks.Timings;
import com.google.common.collect.Maps;

/**
 * Handles Accounts that are loaded from SQL
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
	public AccountContext createAccount( String acctId, String siteId )
	{
		AccountContext context = new AccountContextImpl( this, AccountType.SQL, acctId, siteId );
		
		context.setValue( "date", Timings.epoch() );
		context.setValue( "numloginfailed", 0 );
		context.setValue( "lastloginfail", 0 );
		context.setValue( "actnum", "0" );
		
		save( context );
		return context;
	}
	
	@Override
	public boolean exists( String acctId )
	{
		try
		{
			return sql.getRowCount( sql.query( "SELECT * FROM `" + table + "` WHERE `acctId`='" + acctId + "';" ) ) > 0;
		}
		catch ( SQLException e )
		{
			return false;
		}
	}
	
	@Override
	public void failedLogin( AccountMeta meta, AccountResult result )
	{
		try
		{
			sql.queryUpdate( "UPDATE `" + table + "` SET `lastActive` = '" + Timings.epoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `acctID` = '" + meta.getId() + "'" );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		meta.set( "lastActive", Timings.epoch() );
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
	public List<String> getLoginKeys()
	{
		return accountFields;
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
	public boolean isEnabled()
	{
		return enabled;
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
	public void preLogin( AccountMeta meta, AccountPermissible via, String acctId, Object... creds )
	{
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Timings.epoch() - 1800 ) )
				throw new AccountException( AccountResult.UNDER_ATTACK );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new AccountException( AccountResult.ACCOUNT_NOT_ACTIVATED );
	}
	
	public AccountContext readAccount( String acctId ) throws AccountException, SQLException
	{
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountResult.EMPTY_ACCTID );
		
		Set<String> accountFieldSet = new HashSet<String>( accountFields );
		Set<String> accountColumnSet = new HashSet<String>( sql.getTableColumnNames( table ) );
		
		accountFieldSet.add( "acctId" );
		accountFieldSet.add( "username" );
		
		String additionalAccountFields = "";
		for ( String f : accountFieldSet )
			if ( !f.isEmpty() )
				if ( accountColumnSet.contains( f ) )
					additionalAccountFields += " OR `" + f + "` = '" + acctId + "'";
				else
					for ( String c : accountColumnSet )
						if ( c.equalsIgnoreCase( f ) )
						{
							additionalAccountFields += " OR `" + c + "` = '" + acctId + "'";
							break;
						}
		
		ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
		
		AccountContextImpl context = new AccountContextImpl( this, AccountType.SQL, acctId, "%" );
		
		if ( rs == null || sql.getRowCount( rs ) < 1 )
			throw new AccountException( AccountResult.INCORRECT_LOGIN, context );
		
		context.setAcctId( rs.getString( "acctId" ) );
		context.setSiteId( rs.getString( "siteId" ) );
		context.setValues( DatabaseEngine.convertRow( rs ) );
		
		return context;
	}
	
	@Override
	public void reload( AccountMeta meta )
	{
		try
		{
			readAccount( meta.getId() );
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void save( AccountContext context )
	{
		try
		{
			SqlTableColumns columns = sql.getTableColumns( table );
			Map<String, Object> metaData = context.meta() == null ? context.getValues() : context.meta().getMeta();
			Map<String, Object> toSave = Maps.newTreeMap();
			Map<String, Class<?>> newColumns = Maps.newHashMap();
			
			for ( Entry<String, Object> e : metaData.entrySet() )
			{
				String key = e.getKey();
				
				if ( !"acctId".equalsIgnoreCase( key ) && !"siteId".equalsIgnoreCase( key ) && !"password".equalsIgnoreCase( key ) )
				{
					boolean found = false;
					
					for ( String s : columns )
						if ( s.equalsIgnoreCase( key ) || newColumns.containsKey( key ) )
							found = true;
					
					// There is no column for this key
					if ( !found )
						newColumns.put( key, e.getValue().getClass() );
					
					toSave.put( key, e.getValue() );
				}
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
			
			toSave.put( "acctId", context.getAcctId() );
			toSave.put( "siteId", context.getSiteId() );
			
			ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE `acctId` = '" + context.getAcctId() + "' LIMIT 1;" );
			
			if ( sql.getRowCount( rs ) > 0 )
				sql.update( table, toSave, "`acctId` = '" + context.getAcctId() + "'", 1 );
			else
			{
				SQLException exp;
				while ( ( exp = save0( toSave ) ) != null )
				{
					Pattern p = Pattern.compile( "Field '(.*)' doesn't have a default value" );
					Matcher m = p.matcher( exp.getMessage() );
					
					if ( m.matches() )
					{
						boolean found = false;
						for ( String s : columns )
							if ( s.equals( m.group( 1 ) ) )
							{
								toSave.put( s, columns.get( s ).newType() );
								found = true;
							}
						if ( !found )
						{
							AccountManager.getLogger().warning( "We could not save AccountContext (" + context.getAcctId() + ") because " + exp.getMessage().toLowerCase() );
							break;
						}
					}
					else
						throw exp;
				}
			}
		}
		catch ( SQLException e )
		{
			throw new AccountException( e, context.meta() );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}
	
	private SQLException save0( Map<String, Object> toSave )
	{
		try
		{
			sql.insert( table, toSave );
			return null;
		}
		catch ( SQLException e )
		{
			return e;
		}
	}
	
	
	@Override
	public void successInit( AccountMeta meta, PermissibleEntity entity )
	{
		// Do Nothing
	}
	
	@Override
	public void successLogin( AccountMeta meta )
	{
		try
		{
			sql.queryUpdate( "UPDATE `accounts` SET `lastActive` = '" + Timings.epoch() + "', `lastLogin` = '" + Timings.epoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `acctId` = '" + meta.getId() + "'" );
		}
		catch ( SQLException e )
		{
			throw new AccountException( e );
		}
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
