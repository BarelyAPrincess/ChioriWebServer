/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.types.AccountTypeCreator;
import com.chiorichan.account.types.FileTypeCreator;
import com.chiorichan.account.types.MemoryTypeCreator;
import com.chiorichan.account.types.SqlTypeCreator;
import com.chiorichan.permission.PermissibleEntity;
import com.google.common.collect.Maps;

/**
 * Tracks AccountsTypes available on this server and their handler classes
 */
public final class AccountType
{
	private static final Map<String, AccountType> types = Maps.newConcurrentMap();
	
	/**
	 * Loads Accounts from a SQL Table
	 */
	public static final AccountType SQL = new AccountType( "sql", SqlTypeCreator.INSTANCE, true );
	
	/**
	 * Loads Accounts from the File System
	 */
	public static final AccountType FILE = new AccountType( "file", FileTypeCreator.INSTANCE, true );
	
	/**
	 * Provides internally builtin accounts<br>
	 * Only exists as a support handler to builtin accounts
	 */
	public static final AccountType MEMORY = new AccountType( "memory", MemoryTypeCreator.INSTANCE, true );
	
	/**
	 * References the builtin no login Account with NO PERMISSIONS
	 */
	public static final AccountMeta ACCOUNT_NONE = new AccountMeta( new AccountContext( MemoryTypeCreator.INSTANCE, MEMORY, "none", "%", true ) );
	
	/**
	 * References the builtin root Account with ALL PERMISSIONS and then some!
	 */
	public static final AccountMeta ACCOUNT_ROOT = new AccountMeta( new AccountContext( MemoryTypeCreator.INSTANCE, MEMORY, "root", "%", true ) );
	
	static
	{
		AccountManager.INSTANCE.accounts.put( ACCOUNT_NONE );
		AccountManager.INSTANCE.accounts.put( ACCOUNT_ROOT );
	}
	
	private final boolean builtin;
	
	private final String name;
	
	private final AccountTypeCreator creator;
	
	/**
	 * Registers a new non-builtin AccountType
	 * 
	 * @param name
	 *            The AccountType name
	 */
	public AccountType( String name, AccountTypeCreator creator )
	{
		this( name, creator, false );
	}
	
	/**
	 * Internal Use Only
	 */
	private AccountType( String name, AccountTypeCreator creator, boolean builtin )
	{
		Validate.notNull( name );
		Validate.notNull( creator );
		
		if ( !name.toLowerCase().equals( name ) )
			throw new IllegalStateException( "AccountType names are expected to be in lowercase and singular" );
		
		if ( types.containsKey( name ) )
			throw new IllegalStateException( "AccountType `" + name + "` is already registered with this server" );
		
		types.put( name, this );
		
		this.name = name;
		this.creator = creator;
		this.builtin = builtin;
	}
	
	public static Collection<AccountType> getAccountTypes()
	{
		return Collections.unmodifiableCollection( types.values() );
	}
	
	public static AccountType getDefaultType()
	{
		for ( AccountType type : getAccountTypes() )
			if ( type.isEnabled() && type.isDefault() )
				return type;
		
		AccountManager.getLogger().warning( "We could not find a default AccountType, please check the server configuration." );
		return MEMORY;
	}
	
	public static Set<AccountType> getEnabledAccountTypes()
	{
		Set<AccountType> typesAll = new HashSet<AccountType>( types.values() );
		for ( AccountType at : typesAll )
			if ( !at.isEnabled() )
				typesAll.remove( at );
		return Collections.unmodifiableSet( typesAll );
	}
	
	/**
	 * Tries to find an AccountType based on name alone<br>
	 * Handy for non-builtin types that register with the AccountPipeline
	 * 
	 * @param name
	 *            The name to find
	 * @return
	 *         The matching AccountType, null if none exist
	 */
	public static AccountType getTypeByName( String name )
	{
		return types.get( name.toLowerCase() );
	}
	
	public static boolean isNoneAccount( Account acct )
	{
		return acct == null || acct.getAcctId().equalsIgnoreCase( "none" );
	}
	
	public static boolean isNoneAccount( PermissibleEntity entity )
	{
		return entity == null || entity.getId().equalsIgnoreCase( "none" );
	}
	
	public AccountTypeCreator getCreator()
	{
		return creator;
	}
	
	/**
	 * Gets the name of this AccountType
	 * 
	 * @return
	 *         The AccountType name
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Is this a builtin AccountType, i.e., SQL, FILE, or MEMORY
	 * 
	 * @return
	 *         Is it builtin?
	 */
	public boolean isBuiltin()
	{
		return builtin;
	}
	
	/**
	 * Checks if this type is default per server configuration
	 * 
	 * @return
	 *         True if it is default
	 */
	public boolean isDefault()
	{
		// Memory accounts are never default
		if ( this == AccountType.MEMORY )
			return false;
		
		boolean def = Loader.getConfig().getBoolean( "accounts." + getName() + "Type.default", true );
		
		if ( def && !isEnabled() )
			AccountManager.getLogger().warning( "Your default Account Type is '" + getName() + "' and it's not enabled, possibly due to failure to start, account creation will most likely fail." );
		
		return def;
	}
	
	/**
	 * Checks if this type was enabled in the server configuration
	 * 
	 * @return
	 *         True if it is enabled
	 */
	public boolean isEnabled()
	{
		// Memory accounts are always enabled
		if ( this == AccountType.MEMORY )
			return true;
		
		/*
		 * Lastly we ask the AccountCreator directly if it's enabled.
		 * Returning false would be the answer if there was a problem enabling the creator.
		 */
		return Loader.getConfig().getBoolean( "accounts." + getName() + "Type.enabled", true ) && getCreator().isEnabled();
	}
}
