/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.ConsoleLogger;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountsKeeper.AccountsKeeperOptions;
import com.chiorichan.account.adapter.AccountLookupAdapter;
import com.chiorichan.account.adapter.file.FileAdapter;
import com.chiorichan.account.adapter.sql.SqlAdapter;
import com.chiorichan.account.system.SystemAccounts;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.event.account.AccountLoginEvent;
import com.chiorichan.event.account.PreAccountLoginEvent;
import com.chiorichan.event.account.PreAccountLoginEvent.Result;
import com.chiorichan.exception.StartupException;
import com.chiorichan.framework.Site;
import com.chiorichan.http.session.Session;
import com.chiorichan.permission.Permissible;
import com.chiorichan.permission.structure.Permission;
import com.chiorichan.permission.structure.PermissionDefault;
import com.chiorichan.util.Common;
import com.google.common.collect.Lists;

public class AccountManager
{
	protected static AccountManager instance;
	protected static AccountsKeeper accounts = new AccountsKeeper();
	protected AccountLookupAdapter accountLookupAdapter;
	
	protected List<String> banByIp = new ArrayList<String>();
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
		
		banByIp = config.getStringList( "accounts.banByIp", new ArrayList<String>() );
		maxAccounts = config.getInt( "accounts.maxLogins", -1 );
		
		if ( config.getConfigurationSection( "accounts" ) != null )
		{
			try
			{
				switch ( config.getString( "accounts.lookupAdapter.type", null ) )
				{
					case "sql":
						if ( Loader.getDatabase() == null )
							throw new StartupException( "AccountLookupAdapter is configured with a SQL AccountLookupAdapter but the server is missing a valid SQL Database, which is required for this adapter." );
						
						accountLookupAdapter = new SqlAdapter();
						getLogger().info( "Initiated Sql AccountLookupAdapter `" + accountLookupAdapter + "` with sql '" + Loader.getDatabase() + "'" );
						break;
					case "file":
						accountLookupAdapter = new FileAdapter();
						getLogger().info( "Initiated File AccountLookupAdapter `" + accountLookupAdapter + "`" );
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
						getLogger().info( "Initiated AccountLookupAdapter `" + accountLookupAdapter + "` to use '" + config.getString( "accounts.shareWith" ) + "''s database." );
						break;
					default: // TODO Create custom AccountLookupAdapters.
						getLogger().warning( "The Accounts Manager is unconfigured. We will be unable to login any accounts. See config option 'accounts.lookupAdapter.type' in server config file." );
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
		
		// Create instance of System Accounts which loads them into memory, i.e., Root and NoLogin.
		new SystemAccounts();
	}
	
	public void loadAccount( Account<?> acct )
	{
		loadAccount( acct, false, false, false );
	}
	
	public void loadAccount( Account<?> acct, boolean keepInMemory, boolean whitelistOverride, boolean opOverride )
	{
		AccountsKeeperOptions options = ( AccountsKeeperOptions ) accounts.putAccount( acct, keepInMemory );
		
		options.setWhitelisted( whitelistOverride || Loader.getPermissionManager().getEntity( acct.getAcctId() ).isWhitelisted() );
		options.setOp( opOverride || Loader.getPermissionManager().getEntity( acct.getAcctId() ).isOp() );
		options.setBanned( opOverride || Loader.getPermissionManager().getEntity( acct.getAcctId() ).isBanned() );
	}
	
	public boolean isBanned( String id )
	{
		return banByIp.contains( id );
	}
	
	public Account<?> getOfflineAccount( String name )
	{
		return getOfflineAccount( name, true );
	}
	
	public Account<?> getOfflineAccount( String name, boolean search )
	{
		Validate.notNull( name, "Name cannot be null" );
		
		// TOOD: Fix Me
		return null;
	}
	
	public List<String> getIpBans()
	{
		return banByIp;
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
	
	public List<Account<?>> getOnlineAccounts()
	{
		return accounts.getOnlineAccounts();
	}
	
	public List<Account<?>> getOfflineAccounts()
	{
		return accounts.getOfflineAccounts();
	}
	
	public ArrayList<Account<?>> getAccounts()
	{
		return accounts.getAccounts();
	}
	
	public Account<?> getAccountPartial( String partial )
	{
		return accounts.getAccountPartial( partial );
	}
	
	public Account<?> getAccount( String s )
	{
		try
		{
			return accounts.getAccount( s );
		}
		catch ( LoginException e )
		{
			getLogger().warning( "LoginException was thrown in AccountsManager while trying to get account '" + s + "'. Message: '" + e.getMessage() + "'" );
			return null;
		}
	}
	
	public Account<?> getAccountWithException( String s ) throws LoginException
	{
		return accounts.getAccount( s );
	}
	
	public List<Account<?>> getBannedAccounts()
	{
		ArrayList<Account<?>> accts = Lists.newArrayList();
		
		for ( String id : banByIp )
		{
			try
			{
				Account<?> acct = accounts.getAccount( id );
				if ( acct != null )
					accts.add( acct );
			}
			catch ( LoginException e )
			{
				
			}
		}
		
		return accts;
	}
	
	public List<Account<?>> getWhitelisted()
	{
		ArrayList<Account<?>> accts = Lists.newArrayList();
		
		List<Permissible> entities = Loader.getPermissionManager().getEntitiesWithPermission( PermissionDefault.WHITELISTED.getPermissionNode() );
		
		for ( Permissible entity : entities )
			if ( entity instanceof AccountHandler )
				accts.add( ( ( AccountHandler ) entity ).getAccount() );
		
		return accts;
	}
	
	public List<Account<?>> getOperators()
	{
		ArrayList<Account<?>> accts = Lists.newArrayList();
		
		List<Permissible> entities = Loader.getPermissionManager().getEntitiesWithPermission( PermissionDefault.OP.getPermissionNode() );
		
		for ( Permissible entity : entities )
			if ( entity instanceof AccountHandler )
				accts.add( ( ( AccountHandler ) entity ).getAccount() );
		
		return accts;
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
		
		banByIp = Loader.getConfig().getStringList( "accounts.banByIp", new ArrayList<String>() );
		maxAccounts = Loader.getConfig().getInt( "accounts.maxLogins", maxAccounts );
	}
	
	public int getMaxAccounts()
	{
		return maxAccounts;
	}
	
	public ConsoleLogger getLogger()
	{
		return Loader.getLogger( "AcctMgr" );
	}
	
	public Account<?> attemptLogin( Session sess, String username, String password ) throws LoginException
	{
		if ( username == null || username.isEmpty() )
			throw new LoginException( LoginExceptionReason.emptyUsername );
		
		Account<?> acct = accounts.getAccount( username );
		
		acct.putHandler( sess );
		
		try
		{
			if ( password == null || password.isEmpty() )
				throw new LoginException( LoginExceptionReason.emptyPassword );
			
			if ( !acct.validatePassword( password ) )
				throw new LoginException( LoginExceptionReason.incorrectLogin );
			
			if ( Loader.getPermissionManager().hasWhitelist() && !acct.isWhitelisted() )
				throw new LoginException( LoginExceptionReason.notWhiteListed );
			
			if ( acct.isBanned() )
				throw new LoginException( LoginExceptionReason.banned );
			
			PreAccountLoginEvent preLoginEvent = new PreAccountLoginEvent( sess );
			Loader.getEventBus().callEvent( preLoginEvent );
			
			if ( preLoginEvent.getResult() != Result.ALLOWED )
				if ( preLoginEvent.getKickMessage().isEmpty() )
					throw new LoginException( LoginExceptionReason.cancelledByEvent );
				else
					throw new LoginException( LoginExceptionReason.customReason.setReason( preLoginEvent.getKickMessage() ) );
			
			acct.preLoginCheck();
			
			if ( acct.countHandlers() > 1 && Loader.getConfig().getBoolean( "accounts.singleLogin" ) )
			{
				for ( AccountHandler sh : acct.getHandlers() )
				{
					if ( sh instanceof Session && ( ( Session ) sh ).getSite() == sess.getSite() )
						sh.kick( Loader.getConfig().getString( "accounts.singleLoginMessage", "You logged in from another location." ) );
				}
			}
			
			sess.setVariable( "user", acct.getAcctId() );
			sess.setVariable( "pass", DigestUtils.md5Hex( acct.getPassword() ) );
			
			acct.getMetaData().set( "lastLoginTime", Common.getEpoch() );
			acct.getMetaData().set( "lastLoginIp", sess.getIpAddr() );
			
			AccountLoginEvent loginEvent = new AccountLoginEvent( sess, String.format( Loader.getConfig().getString( "accounts.loginMessage", "%s has logged in at site %s" ), acct.getUsername(), sess.getSite().getTitle() ) );
			Loader.getEventBus().callEvent( loginEvent );
			
			return acct;
		}
		catch ( LoginException l )
		{
			l.setAccount( acct );
			accountLookupAdapter.failedLoginUpdate( acct.getMetaData(), l.getReason() );
			throw l;
		}
	}
}
