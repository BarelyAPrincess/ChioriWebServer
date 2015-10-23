/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.types;

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
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.datastore.sql.SqlTableColumns;
import com.chiorichan.datastore.sql.bases.SQLDatastore;
import com.chiorichan.datastore.sql.query.SQLQuerySelect;
import com.chiorichan.event.EventHandler;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.tasks.Timings;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Maps;

/**
 * Handles Accounts that are loaded from SQL
 */
public class SqlTypeCreator extends AccountTypeCreator
{
	public static final SqlTypeCreator INSTANCE = new SqlTypeCreator();
	
	final SQLDatastore sql;
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
	public AccountContext createAccount( String acctId, String siteId ) throws AccountException
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
			return sql.table( table ).select().where( "acctId" ).matches( acctId ).execute().rowCount() > 0;
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
			sql.table( table ).update().value( "lastActive", Timings.epoch() ).value( "lastLoginFail", 0 ).value( "numLoginFail", 0 ).where( "acctID" ).matches( meta.getId() ).execute();
			// sql.queryUpdate( "UPDATE `" + table + "` SET `lastActive` = '" + Timings.epoch() + "', `lastLoginFail` = 0, `numLoginFail` = 0 WHERE `acctID` = '" + meta.getId() + "'" );
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
	
	/*
	 * public ResultSet getResultSet( String uid ) throws SQLException
	 * {
	 * if ( uid == null || uid.isEmpty() )
	 * return null;
	 * 
	 * SQLExecute execute = sql.table( "accounts" ).select().where( "acctId" ).equals( uid ).limit( 1 ).execute();
	 * 
	 * if ( execute.rowCount() < 1 )
	 * return null;
	 * 
	 * return execute.resultSet();
	 * }
	 */
	
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
			event.setResult( readAccount( event.getAcctId() ), AccountDescriptiveReason.LOGIN_SUCCESS );
		}
		catch ( SQLException e )
		{
			if ( Versioning.isDevelopment() )
				e.printStackTrace();
			
			event.setResult( null, AccountDescriptiveReason.INTERNAL_ERROR ).setCause( e );
		}
		catch ( AccountException e )
		{
			event.setResult( null, e.getReason() );
		}
	}
	
	@Override
	public void preLogin( AccountMeta meta, AccountPermissible via, String acctId, Object... creds ) throws AccountException
	{
		if ( meta.getInteger( "numloginfail" ) > 5 )
			if ( meta.getInteger( "lastloginfail" ) > ( Timings.epoch() - 1800 ) )
				throw new AccountException( AccountDescriptiveReason.UNDER_ATTACK, meta );
		
		if ( !meta.getString( "actnum" ).equals( "0" ) )
			throw new AccountException( AccountDescriptiveReason.ACCOUNT_NOT_ACTIVATED, meta );
	}
	
	public AccountContext readAccount( String acctId ) throws AccountException, SQLException
	{
		if ( acctId == null || acctId.isEmpty() )
			throw new AccountException( AccountDescriptiveReason.EMPTY_ACCTID, acctId );
		
		Set<String> accountFieldSet = new HashSet<String>( accountFields );
		Set<String> accountColumnSet = new HashSet<String>( sql.table( table ).columnNames() );
		
		accountFieldSet.add( "acctId" );
		accountFieldSet.add( "username" );
		
		SQLQuerySelect select = sql.table( table ).select();
		
		// String additionalAccountFields = "";
		for ( String f : accountFieldSet )
			if ( !f.isEmpty() )
				if ( accountColumnSet.contains( f ) )
					select.or().where( f ).matches( acctId );
				else
					for ( String c : accountColumnSet )
						if ( c.equalsIgnoreCase( f ) )
						{
							select.or().where( c ).matches( acctId );
							break;
						}
		
		select.execute();
		
		// ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE " + additionalAccountFields.substring( 4 ) + ";" );
		
		AccountContextImpl context = new AccountContextImpl( this, AccountType.SQL, acctId, "%" );
		
		if ( select.rowCount() < 1 )
			throw new AccountException( AccountDescriptiveReason.INCORRECT_LOGIN, acctId );
		
		Map<String, String> row = select.stringRow();
		
		context.setAcctId( row.get( "acctId" ) );
		context.setSiteId( row.get( "siteId" ) );
		context.setValues( select.row() );
		
		return context;
	}
	
	@Override
	public void reload( AccountMeta meta ) throws AccountException
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
	public void save( AccountContext context ) throws AccountException
	{
		try
		{
			SqlTableColumns columns = sql.table( table ).columns();
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
						sql.table( table ).addColumn( c.getValue(), c.getKey(), 255 );
					}
					catch ( SQLException e )
					{
						AccountManager.getLogger().severe( "Could not create a new column for key `" + c.getKey() + "` with class `" + c.getValue() + "`", e );
						toSave.remove( c.getKey() );
					}
			
			toSave.put( "acctId", context.getAcctId() );
			toSave.put( "siteId", context.getSiteId() );
			
			SQLQuerySelect select = sql.table( table ).select().where( "acctId" ).matches( context.getAcctId() ).limit( 1 ).execute();
			// ResultSet rs = sql.query( "SELECT * FROM `" + table + "` WHERE `acctId` = '" + context.getAcctId() + "' LIMIT 1;" );
			
			if ( select.rowCount() > 0 )
				sql.table( table ).update().values( toSave ).where( "acctId" ).matches( context.getAcctId() ).limit( 1 ).execute();
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
			sql.table( table ).insert().values( toSave ).execute();
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
	public void successLogin( AccountMeta meta ) throws AccountException
	{
		try
		{
			sql.table( "accounts" ).update().value( "lastActive", Timings.epoch() ).value( "lastLogin", Timings.epoch() ).value( "lastLoginFail", 0 ).value( "numLoginFail", 0 ).where( "acctId" ).matches( meta.getId() ).execute();
		}
		catch ( SQLException e )
		{
			throw new AccountException( e, meta );
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
