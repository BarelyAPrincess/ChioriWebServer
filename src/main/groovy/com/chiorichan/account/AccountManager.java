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
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.account.lang.AccountException;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.server.KickEvent;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskCreator;
import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Provides Account Management to the Server
 */
public final class AccountManager extends AccountEvents implements ServerManager, TaskCreator
{
	public static final AccountManager INSTANCE = new AccountManager();
	private static boolean isInitialized = false;
	final AccountList accounts = new AccountList();
	boolean isDebug = false;
	int maxAccounts = -1;
	
	private AccountManager()
	{
		
	}
	
	public static APILogger getLogger()
	{
		return Loader.getLogger( "AcctMgr" );
	}
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Account Manager has already been initialized." );
		
		assert INSTANCE != null;
		
		INSTANCE.init0();
		
		isInitialized = true;
	}
	
	/**
	 * Has this manager already been initialized?
	 * 
	 * @return isInitialized
	 */
	public static boolean isInitialized()
	{
		return isInitialized;
	}
	
	public AccountMeta createAccount( String acctId, String siteId ) throws AccountException
	{
		return createAccount( acctId, siteId, AccountType.getDefaultType() );
	}
	
	public AccountMeta createAccount( String acctId, String siteId, AccountType type ) throws AccountException
	{
		if ( !type.isEnabled() )
			throw new AccountException( AccountDescriptiveReason.FEATURE_DISABLED, acctId );
		
		AccountContext context = type.getCreator().createAccount( acctId, siteId );
		
		return new AccountMeta( context );
	}
	
	private boolean exists( String acctId )
	{
		if ( accounts.keySet().contains( acctId ) )
			return true;
		
		for ( AccountType type : AccountType.getAccountTypes() )
			if ( type.getCreator().exists( acctId ) )
				return true;
		return false;
	}
	
	public String generateAcctId( String seed )
	{
		String acctId = "";
		
		if ( seed == null || seed.isEmpty() )
			acctId = "ab123C";
		else
		{
			seed = seed.replaceAll( "[\\W\\d]", "" );
			
			acctId = StringFunc.randomChars( seed, 2 ).toLowerCase();
			String sum = StringFunc.removeLetters( SecureFunc.md5( seed ) );
			acctId += sum.length() < 3 ? SecureFunc.randomize( "123" ) : sum.substring( 0, 3 );
			acctId += StringFunc.randomChars( seed, 1 ).toUpperCase();
		}
		
		if ( acctId == null || acctId.isEmpty() )
			acctId = "ab123C";
		
		int tries = 1;
		do
		{
			Validate.notEmpty( acctId );
			Validate.validState( acctId.length() == 6 );
			Validate.validState( acctId.matches( "[a-z]{2}[0-9]{3}[A-Z]" ) );
			
			// When our tries are divisible by 25 we attempt to randomize the last letter for more chances.
			if ( tries % 25 == 0 )
				acctId = acctId.substring( 0, 4 ) + SecureFunc.randomize( acctId.substring( 5 ) );
			
			acctId = acctId.substring( 0, 2 ) + SecureFunc.randomize( "123" ) + acctId.substring( 5 );
			
			tries++;
		}
		while ( exists( acctId ) );
		
		return acctId;
	}
	
	public AccountMeta getAccount( String acctId )
	{
		AccountMeta acct = accounts.get( acctId );
		
		if ( acct == null )
		{
			acct = fireAccountLookup( acctId );
			
			if ( acct == null )
				return null;
			
			accounts.put( acct );
		}
		
		return acct;
	}
	
	public AccountMeta getAccountPartial( String partial ) throws AccountException
	{
		Validate.notNull( partial );
		
		AccountMeta found = null;
		String lowerName = partial.toLowerCase();
		int delta = Integer.MAX_VALUE;
		for ( AccountMeta meta : getAccounts() )
			if ( meta.getId().toLowerCase().startsWith( lowerName ) )
			{
				int curDelta = meta.getId().length() - lowerName.length();
				if ( curDelta < delta )
				{
					found = meta;
					delta = curDelta;
				}
				if ( curDelta == 0 )
					break;
			}
		return found;
	}
	
	public Set<AccountMeta> getAccounts()
	{
		return Collections.unmodifiableSet( getAccounts0() );
	}
	
	public Set<AccountMeta> getAccounts( String query )
	{
		Validate.notNull( query );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		if ( query.contains( "|" ) )
		{
			for ( String s : Splitter.on( "|" ).split( query ) )
				if ( s != null && !s.isEmpty() )
					results.addAll( getAccounts( s ) );
			
			return results;
		}
		
		boolean isLower = query.toLowerCase().equals( query ); // Is query string all lower case?
		
		for ( AccountMeta meta : accounts.toSet() )
		{
			String id = ( isLower ) ? meta.getId().toLowerCase() : meta.getId();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			id = ( isLower ) ? meta.getDisplayName().toLowerCase() : meta.getDisplayName();
			
			if ( !id.isEmpty() && id.contains( query ) )
			{
				results.add( meta );
				continue;
			}
			
			// TODO Figure out how to further check these values.
			// Maybe send the check into the Account Creator
		}
		
		return results;
	}
	
	public Set<AccountMeta> getAccounts( String key, String value )
	{
		Validate.notNull( key );
		Validate.notNull( value );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		if ( value.contains( "|" ) )
		{
			for ( String s : Splitter.on( "|" ).split( value ) )
				if ( s != null && !s.isEmpty() )
					results.addAll( getAccounts( key, s ) );
			
			return results;
		}
		
		boolean isLower = value.toLowerCase().equals( value ); // Is query string all lower case?
		
		for ( AccountMeta meta : accounts.toSet() )
		{
			String str = ( isLower ) ? meta.getString( key ).toLowerCase() : meta.getString( key );
			
			if ( str != null && !str.isEmpty() && str.contains( value ) )
			{
				results.add( meta );
				continue;
			}
		}
		
		return results;
	}
	
	Set<AccountMeta> getAccounts0()
	{
		return accounts.toSet();
	}
	
	public Set<AccountMeta> getAccountsBySite( Site site )
	{
		Validate.notNull( site );
		
		Set<AccountMeta> results = Sets.newHashSet();
		
		for ( AccountMeta meta : accounts.toSet() )
			if ( meta.getSite() == site )
				results.add( meta );
		
		return results;
	}
	
	public Set<AccountMeta> getAccountsBySite( String site )
	{
		return getAccountsBySite( SiteManager.INSTANCE.getSiteById( site ) );
	}
	
	public AccountMeta getAccountWithException( String acctId ) throws AccountException
	{
		AccountMeta acct = accounts.get( acctId );
		
		if ( acct == null )
		{
			acct = fireAccountLookupWithException( acctId );
			
			Validate.notNull( acct );
			accounts.put( acct );
		}
		
		return acct;
	}
	
	public Set<Account> getBanned()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.getEntity().isBanned() )
				accts.add( meta );
		return accts;
	}
	
	public Set<Account> getInitializedAccounts()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.add( meta );
		return accts;
	}
	
	@Override
	public String getName()
	{
		return "AccountManager";
	}
	
	public Set<Account> getOperators()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.getEntity().isOp() )
				accts.add( meta );
		return accts;
	}
	
	/**
	 * Gets all Account Permissibles by crawling the {@link AccountMeta} and {@link AccountInstance}
	 * 
	 * @return
	 *         A set of AccountPermissibles
	 */
	public Collection<AccountAttachment> getPermissibles()
	{
		Set<AccountAttachment> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.isInitialized() )
				accts.addAll( meta.instance().getAttachments() );
		return accts;
	}
	
	public Set<Account> getWhitelisted()
	{
		Set<Account> accts = Sets.newHashSet();
		for ( AccountMeta meta : accounts )
			if ( meta.getEntity().isWhitelisted() )
				accts.add( meta );
		return accts;
	}
	
	private void init0()
	{
		isDebug = Loader.getConfig().getBoolean( "accounts.debug" );
		maxAccounts = Loader.getConfig().getInt( "accounts.maxLogins", -1 );
		
		EventBus.INSTANCE.registerEvents( AccountType.MEMORY.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.SQL.getCreator(), this );
		EventBus.INSTANCE.registerEvents( AccountType.FILE.getCreator(), this );
	}
	
	public boolean isDebug()
	{
		return isDebug || Versioning.isDevelopment();
	}
	
	public void reload()
	{
		save();
		accounts.clear();
	}
	
	public void save()
	{
		for ( AccountMeta meta : accounts )
			try
			{
				meta.save();
			}
			catch ( AccountException e )
			{
				e.printStackTrace();
			}
	}
	
	public void shutdown( String reason )
	{
		try
		{
			Set<Kickable> kickables = Sets.newHashSet();
			for ( AccountMeta acct : AccountManager.INSTANCE.getAccounts() )
				if ( acct.isInitialized() )
					for ( AccountAttachment attachment : acct.instance().getAttachments() )
						if ( attachment.getPermissible() instanceof Kickable )
							kickables.add( ( Kickable ) attachment.getPermissible() );
						else if ( attachment instanceof Kickable )
							kickables.add( ( Kickable ) attachment );
			
			KickEvent.kick( AccountType.ACCOUNT_ROOT, kickables ).setReason( reason ).fire();
		}
		catch ( Throwable t )
		{
			// Ignore
		}
		
		save();
	}
}
