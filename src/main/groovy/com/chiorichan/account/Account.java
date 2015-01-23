/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.Loader;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.permission.PermissibleInteractive;
import com.chiorichan.permission.PermissibleType;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public abstract class Account<T extends AccountLookupAdapter> extends PermissibleInteractive
{
	/**
	 * Cached Account lookup adapter.
	 */
	protected final T lookupAdapter;
	
	/**
	 * Set of AccountHandlers
	 */
	protected volatile Set<AccountHandler> handlers = Sets.newHashSet();
	
	/**
	 * Account MetaData
	 */
	protected final AccountMetaData metaData;
	
	/**
	 * Account Id
	 */
	protected final String acctId;
	
	public Account(String userId, T adapter) throws LoginException
	{
		if ( userId.isEmpty() )
			throw new LoginException( LoginExceptionReason.emptyUsername );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		lookupAdapter = adapter;
		
		metaData = adapter.readAccount( userId );
		acctId = metaData.getAcctId();
	}
	
	public Account(AccountMetaData meta, T adapter) throws LoginException
	{
		if ( meta == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		if ( adapter == null )
			throw new LoginException( LoginExceptionReason.unknownError );
		
		lookupAdapter = adapter;
		
		metaData = meta;
		acctId = meta.getAcctId();
	}
	
	/**
	 * Get the baked registered listeners associated with this handler list
	 * 
	 * @return the array of registered listeners
	 */
	public final AccountHandler[] getHandlers()
	{
		checkHandlers();
		return handlers.toArray( new AccountHandler[0] );
	}
	
	public final void putHandler( AccountHandler handler )
	{
		if ( !handlers.contains( handler ) )
			handlers.add( handler );
	}
	
	public final void removeHandler( AccountHandler handler )
	{
		checkHandlers();
		handlers.remove( handler );
	}
	
	public final void clearHandlers()
	{
		checkHandlers();
		handlers.clear();
	}
	
	public final boolean hasHandler()
	{
		checkHandlers();
		return !handlers.isEmpty();
	}
	
	public final int countHandlers()
	{
		checkHandlers();
		return handlers.size();
	}
	
	private final void checkHandlers()
	{
		for ( AccountHandler h : handlers )
			if ( !h.isValid() )
				handlers.remove( h );
	}
	
	public final AccountMetaData getMetaData()
	{
		return metaData;
	}
	
	public final boolean validatePassword( String _password )
	{
		String password = getPassword();
		return ( password.equals( _password ) || password.equals( DigestUtils.md5Hex( _password ) ) || DigestUtils.md5Hex( password ).equals( _password ) );
	}
	
	public boolean isBanned()
	{
		return Loader.getAccountsManager().isBanned( acctId );
	}
	
	public boolean isWhitelisted()
	{
		return Loader.getAccountsManager().isWhitelisted( acctId );
	}
	
	public abstract String getPassword();
	
	public abstract String getDisplayName();
	
	public abstract String getUsername();
	
	@Override
	public final String getName()
	{
		return getUsername();
	}
	
	public final boolean kick( final String kickMessage )
	{
		for ( AccountHandler h : handlers )
			if ( !h.kick( kickMessage ) )
				return false;
		return true;
		
	}
	
	public final void save()
	{
		lookupAdapter.saveAccount( metaData );
	}
	
	@Override
	public String getId()
	{
		return getAcctId();
	}
	
	public String getAcctId()
	{
		String uid = metaData.getString( "acctId" );
		
		if ( uid == null )
			uid = metaData.getString( "accountId" );
		
		/** TEMP START - MAYBE **/
		if ( uid == null )
			uid = metaData.getString( "userId" );
		
		if ( uid == null )
			uid = metaData.getString( "userID" );
		
		if ( uid == null )
			uid = metaData.getString( "id" );
		/** TEMP END **/
		
		return uid;
	}
	
	public String toString()
	{
		return "User{" + metaData.toString() + ",Handlers{" + Joiner.on( "," ).join( handlers ) + "}}";
	}
	
	/**
	 * 
	 * @param key
	 *             Metadata key.
	 * @return String
	 *         Returns an empty string if no result.
	 */
	public String getString( String key )
	{
		return getString( key, "" );
	}
	
	/**
	 * Get a string from the Metadata with a default value
	 * 
	 * @param key
	 *             Metadata key.
	 * @param def
	 *             Default value to return if no result.
	 * @return String
	 */
	public String getString( String key, String def )
	{
		if ( !metaData.containsKey( key ) )
			return def;
		
		return metaData.getString( key );
	}
	
	public final void sendMessage( String... msgs )
	{
		for ( String msg : msgs )
			sendMessage( msg );
	}
	
	public final void sendMessage( String msg )
	{
		for ( AccountHandler h : handlers )
			h.sendMessage( msg );
	}
	
	public final void reloadAndValidate() throws LoginException
	{
		metaData.mergeData( lookupAdapter.reloadAccount( metaData ) );
	}
	
	public final AccountLookupAdapter getLookupAdapter()
	{
		return lookupAdapter;
	}
	
	@Override
	public final PermissibleType getType()
	{
		if ( handlers.size() == 1 )
			return handlers.toArray( new AccountHandler[0] )[0].getType();
		
		return null;
	}
	
	@Override
	public final String getIpAddr()
	{
		if ( handlers.size() == 1 )
			return handlers.toArray( new AccountHandler[0] )[0].getIpAddr();
		
		return null;
	}
	
	@Override
	public boolean isValid()
	{
		return metaData.hasMinimumData();
	}
	
	/**
	 * Called before the AccountManager makes the login offical.
	 * 
	 * @param account
	 *             The account used in this check
	 * @throws LoginException
	 *              Throw this exception if you wish to interrupt the login
	 */
	public abstract void preLoginCheck() throws LoginException;
	
	/**
	 * Called as the last line before account is returned.
	 * 
	 * @param account
	 *             The account used in this check
	 * @throws LoginException
	 *              Throw this exception if you wish to interrupt the login
	 */
	public abstract void postLoginCheck() throws LoginException;
	
	/**
	 * Called from AccountManager to match the account using an array of fields, e.g., email, phone, name, acctId
	 * 
	 * @param id
	 *             The identifier to match against.
	 * @return Boolean
	 *         true if it matches.
	 */
	public abstract boolean isYou( String id );
}
