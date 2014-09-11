/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.adapter.FileAdapter;
import com.chiorichan.account.adapter.SqlAdapter;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.account.helpers.AccountsKeeper;
import com.chiorichan.account.helpers.AccountsKeeper.AccountsKeeperOptions;
import com.chiorichan.account.helpers.LoginException;
import com.chiorichan.account.helpers.LoginExceptionReasons;
import com.chiorichan.account.helpers.LookupAdapterException;
import com.chiorichan.account.system.SystemAccounts;
import com.chiorichan.bus.events.account.AccountLoginEvent;
import com.chiorichan.bus.events.account.PreAccountLoginEvent;
import com.chiorichan.bus.events.account.PreAccountLoginEvent.Result;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistentSession;
import com.google.common.collect.Lists;

public class AccountManager
{
	protected static AccountManager instance;
	protected static AccountsKeeper accounts = new AccountsKeeper();
	protected AccountLookupAdapter accountLookupAdapter;
	
	protected List<String> banById = new ArrayList<String>();
	protected List<String> banByIp = new ArrayList<String>();
	protected List<String> operators = new ArrayList<String>();
	protected List<String> whitelist = new ArrayList<String>();
	protected boolean hasWhitelist = false;
	
	protected int maxAccounts = -1;
	
	public AccountManager()
	{
		instance = this;
	}
	
	public static AccountManager getInstance()
	{
		return instance;
	}
	
	public void init()
	{
		YamlConfiguration config = Loader.getConfig();
		
		banById = config.getStringList( "accounts.banById", new ArrayList<String>() );
		banByIp = config.getStringList( "accounts.banByIp", new ArrayList<String>() );
		operators = config.getStringList( "accounts.operators", new ArrayList<String>() );
		whitelist = config.getStringList( "accounts.whitelisted", new ArrayList<String>() );
		hasWhitelist = config.getBoolean( "settings.whitelist" );
		maxAccounts = config.getInt( "accounts.maxLogins", -1 );
		
		if ( config.getConfigurationSection( "accounts" ) != null )
		{
			try
			{
				switch ( config.getString( "accounts.lookupAdapter.type", null ) )
				{
					case "sql":
						if ( Loader.getPersistenceManager().getDatabase() == null )
							throw new StartupException( "AccountLookupAdapter is configured with a SQL AccountLookupAdapter but the server is missing a valid SQL Database, which is required for this adapter." );
						
						accountLookupAdapter = new SqlAdapter();
						Loader.getLogger().info( "Initiated Sql AccountLookupAdapter `" + accountLookupAdapter + "` with sql '" + Loader.getPersistenceManager().getDatabase() + "'" );
						break;
					case "file":
						accountLookupAdapter = new FileAdapter();
						Loader.getLogger().info( "Initiated FileBase AccountLookupAdapter `" + accountLookupAdapter + "`" );
						break;
					case "shared":
						if ( config.getString( "accounts.lookupAdapter.shareWith", null ) == null )
							throw new StartupException( "The AccountLookupAdapter is configured to use another site's database, but the config section 'accounts.lookupAdapter.shareWith' is missing." );
						
						Site shared = Loader.getSiteManager().getSiteById( config.getString( "accounts.lookupAdapter.shareWith" ) );
						
						if ( shared == null )
							throw new StartupException( "The AccountLookupAdapter is configured to use '" + config.getString( "accounts.shareWith" ) + "''s database, but there was no sites found by that id." );
						
						if ( shared.getDatabase() == null )
							throw new StartupException( "The AccountLookupAdapter is configured to use '" + config.getString( "accounts.shareWith" ) + "''s database, but the found site has no Database configured." );
						
						accountLookupAdapter = new SqlAdapter( shared );
						Loader.getLogger().info( "Initiated AccountLookupAdapter `" + accountLookupAdapter + "` to use '" + config.getString( "accounts.shareWith" ) + "''s database." );
						break;
					default: // TODO Create custom AccountLookupAdapters.
						Loader.getLogger().warning( "The Accounts Bus is not configured with a AccountLookupAdapter. We will be unable to login any accounts." );
				}
			}
			catch ( LookupAdapterException e )
			{
				throw new StartupException( "There was an exception encoutered when attempting to create the AccountLookupAdapter. Please check and retry to start the server.", e );
			}
		}
		else
		{
			throw new StartupException( "Your configuration seems to be missing the `accounts` section. Please check and retry to start the server." );
		}
		
		accounts.setAdapter( accountLookupAdapter );
		
		// Create instance of System Accounts (which in turn loads them into memory.)
		new SystemAccounts();
	}
	
	public void LoadAccount( Account acct )
	{
		LoadAccount( acct, false, false, false );
	}
	
	public void LoadAccount( Account acct, boolean keepInMemory, boolean whitelistOverride, boolean opOverride )
	{
		AccountsKeeperOptions options = (AccountsKeeperOptions) accounts.putAccount( acct, keepInMemory );
		
		options.setWhitelisted( whitelistOverride || isWhitelisted( acct.getAccountId() ) );
		options.setOp( opOverride || isOp( acct.getAccountId() ) );
	}
	
	public boolean isWhitelisted( String id )
	{
		return !hasWhitelist || operators.contains( id ) || whitelist.contains( id );
	}
	
	public boolean isOp( String id )
	{
		return operators.contains( id );
	}
	
	public boolean isBanned( String id )
	{
		return banById.contains( id );
	}
	
	public Account getOfflineAccount( String name )
	{
		return getOfflineAccount( name, true );
	}
	
	public Account getOfflineAccount( String name, boolean search )
	{
		Validate.notNull( name, "Name cannot be null" );
		
		// TOOD: Fix Me
		return null;
	}
	
	public List<String> getIpBans()
	{
		return banByIp;
	}
	
	public List<String> getIdBans()
	{
		return banById;
	}
	
	public void banId( Account acct )
	{
		banId( acct.getAccountId() );
	}
	
	public void banId( String id )
	{
		Validate.notNull( id, "Account Id cannot be null." );
		
		banById.add( id );
		Loader.getConfig().set( "accounts.banById", banById );
	}
	
	public void unbanId( Account acct )
	{
		unbanId( acct.getAccountId() );
	}
	
	public void unbanId( String id )
	{
		Validate.notNull( id, "Account Id cannot be null." );
		
		if ( banById.contains( id ) )
		{
			banById.remove( id );
			Loader.getConfig().set( "accounts.banById", banById );
		}
	}
	
	public void banIp( String addr )
	{
		Validate.notNull( addr, "Ip Address cannot be null." );
		
		banByIp.add( addr );
		Loader.getConfig().set( "accounts.banByIp", banByIp );
	}
	
	public void unbanIp( String addr )
	{
		Validate.notNull( addr, "Ip Address cannot be null." );
		
		if ( banByIp.contains( addr ) )
		{
			banByIp.remove( addr );
			Loader.getConfig().set( "accounts.banByIp", banByIp );
		}
	}
	
	public void addWhitelist( String id )
	{
		addWhitelist( getAccount( id ) );
	}
	
	public void addWhitelist( Account acct )
	{
		if ( acct == null )
			return;
		
		whitelist.add( acct.getAccountId() );
		Loader.getConfig().set( "accounts.whitelisted", whitelist );
		
		( (AccountsKeeperOptions) accounts.getAccountOptions( acct ) ).setWhitelisted( true );
	}
	
	public void removeWhitelist( String id )
	{
		removeWhitelist( getAccount( id ) );
	}
	
	public void removeWhitelist( Account acct )
	{
		if ( acct == null )
			return;
		
		if ( whitelist.contains( acct.getAccountId() ) )
		{
			whitelist.remove( acct.getAccountId() );
			Loader.getConfig().set( "accounts.whitelisted", whitelist );
			
			( (AccountsKeeperOptions) accounts.getAccountOptions( acct ) ).setWhitelisted( false );
		}
	}
	
	public Account op( String id )
	{
		return op( getAccount( id ) );
	}
	
	public Account op( Account acct )
	{
		if ( acct == null )
			return null;
		
		operators.add( acct.getAccountId() );
		Loader.getConfig().set( "accounts.operators", operators );
		
		( (AccountsKeeperOptions) accounts.getAccountOptions( acct ) ).setOp( true );
		
		return acct;
	}
	
	public Account deop( String id )
	{
		return deop( getAccount( id ) );
	}
	
	public Account deop( Account acct )
	{
		if ( acct == null )
			return null;
		
		if ( operators.contains( acct.getAccountId() ) )
		{
			operators.remove( acct.getAccountId() );
			Loader.getConfig().set( "accounts.operators", operators );
			
			( (AccountsKeeperOptions) accounts.getAccountOptions( acct ) ).setOp( false );
		}
		
		return acct;
	}
	
	public List<Account> getOnlineAccounts()
	{
		return accounts.getOnlineAccounts();
	}
	
	public List<Account> getOfflineAccounts()
	{
		return accounts.getOfflineAccounts();
	}
	
	public ArrayList<Account> getAccounts()
	{
		return accounts.getAccounts();
	}
	
	public Account getAccountPartial( String partial )
	{
		return accounts.getAccountPartial( partial );
	}
	
	public Account getAccount( String s )
	{
		try
		{
			return accounts.getAccount( s );
		}
		catch ( LoginException e )
		{
			Loader.getLogger().warning( "An exception was thrown in AccountsBus while trying to get an account.", e );
			return null;
		}
	}
	
	public Account getAccountWithException( String s ) throws LoginException
	{
		return accounts.getAccount( s );
	}
	
	public List<Account> getBannedAccounts()
	{
		ArrayList<Account> accts = Lists.newArrayList();
		
		for ( String id : banById )
		{
			try
			{
				Account acct = accounts.getAccount( id );
				if ( acct != null )
					accts.add( acct );
			}
			catch ( LoginException e )
			{	
				
			}
		}
		
		return accts;
	}
	
	public List<Account> getWhitelisted()
	{
		ArrayList<Account> accts = Lists.newArrayList();
		
		for ( String id : whitelist )
		{
			try
			{
				Account acct = accounts.getAccount( id );
				if ( acct != null )
					accts.add( acct );
			}
			catch ( LoginException e )
			{	
				
			}
		}
		
		return accts;
	}
	
	public List<Account> getOperators()
	{
		ArrayList<Account> accts = Lists.newArrayList();
		
		for ( String id : operators )
		{
			try
			{
				Account acct = accounts.getAccount( id );
				if ( acct != null )
					accts.add( acct );
			}
			catch ( LoginException e )
			{	
				
			}
		}
		
		return accts;
	}
	
	public void setWhitelist( boolean value )
	{
		hasWhitelist = value;
		Loader.getConfig().set( "settings.whitelist", value );
	}
	
	public void shutdown()
	{
		accounts.saveAccounts();
		accounts.clearAll();
	}
	
	public void saveAccounts()
	{
		accounts.saveAccounts();
	}
	
	public void reload()
	{
		accounts.saveAccounts();
		accounts.clearAll();
		
		banById = Loader.getConfig().getStringList( "accounts.banById", new ArrayList<String>() );
		banByIp = Loader.getConfig().getStringList( "accounts.banByIp", new ArrayList<String>() );
		operators = Loader.getConfig().getStringList( "accounts.operators", new ArrayList<String>() );
		
		maxAccounts = Loader.getConfig().getInt( "accounts.maxLogins", maxAccounts );
		
		reloadWhitelist();
	}
	
	public void reloadWhitelist()
	{
		whitelist = Loader.getConfig().getStringList( "accounts.whitelisted", new ArrayList<String>() );
		hasWhitelist = Loader.getConfig().getBoolean( "settings.whitelist" );
	}
	
	public int getMaxAccounts()
	{
		return maxAccounts;
	}
	
	public Account attemptLogin( PersistentSession sess, String username, String password ) throws LoginException
	{
		if ( username == null || username.isEmpty() )
			throw new LoginException( LoginExceptionReasons.emptyUsername );
		
		Account acct = accounts.getAccount( username );
		
		acct.putHandler( sess );
		
		try
		{
			if ( password == null || password.isEmpty() )
				throw new LoginException( LoginExceptionReasons.emptyPassword );
			
			if ( !acct.validatePassword( password ) )
				throw new LoginException( LoginExceptionReasons.incorrectLogin );
			
			if ( !acct.isWhitelisted() )
				throw new LoginException( LoginExceptionReasons.notWhiteListed );
			
			if ( acct.isBanned() )
				throw new LoginException( LoginExceptionReasons.banned );
			
			PreAccountLoginEvent preLoginEvent = new PreAccountLoginEvent( acct );
			Loader.getEventBus().callEvent( preLoginEvent );
			
			if ( preLoginEvent.getResult() != Result.ALLOWED )
				if ( preLoginEvent.getKickMessage().isEmpty() )
					throw new LoginException( LoginExceptionReasons.cancelledByEvent );
				else
					throw new LoginException( LoginExceptionReasons.customReason.setReason( preLoginEvent.getKickMessage() ) );
			
			accountLookupAdapter.preLoginCheck( acct );
			
			if ( acct.countHandlers() > 1 && Loader.getConfig().getBoolean( "accounts.singleLogin" ) )
			{
				for ( SentientHandler sh : acct.getHandlers() )
				{
					if ( sh instanceof PersistentSession && ( (PersistentSession) sh ).getSite() == sess.getSite() )
						sh.kick( Loader.getConfig().getString( "accounts.singleLoginMessage", "You logged in from another location." ) );
				}
			}
			
			sess.setArgument( "user", acct.getAccountId() );
			sess.setArgument( "pass", DigestUtils.md5Hex( acct.getPassword() ) );
			
			AccountLoginEvent loginEvent = new AccountLoginEvent( acct, String.format( Loader.getConfig().getString( "accounts.loginMessage", "%s has logged in at site %s" ), acct.getUsername(), sess.getSite().getTitle() ) );
			Loader.getEventBus().callEvent( loginEvent );
			
			return acct;
		}
		catch ( LoginException l )
		{
			accountLookupAdapter.failedLoginUpdate( acct );
			throw l.setAccount( acct );
		}
	}
}
