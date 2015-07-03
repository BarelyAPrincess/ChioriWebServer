/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.RandomFunc;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public final class AccountMeta implements Account, Iterable<Entry<String, Object>>
{
	public static final List<String> IGNORED_KEYS = Arrays.asList( new String[] {"siteId", "acctId"} );
	
	/**
	 * Used to store our Account Metadata besides the required builtin key names.
	 */
	private final Map<String, Object> metadata = Maps.newTreeMap( String.CASE_INSENSITIVE_ORDER );
	
	/**
	 * Provides context into our existence
	 */
	private final AccountContext context;
	
	/**
	 * Used as our reference to the Account Instance.<br>
	 * We use a WeakReference so the account can be logged out automatically when no longer used.
	 */
	private WeakReference<AccountInstance> account = null;
	
	/**
	 * Site Id
	 */
	private final String siteId;
	
	/**
	 * Account Id
	 */
	private final String acctId;
	
	/**
	 * Used to keep the Account Instance loaded in the memory when {@link #keepInMemory} is set to true<br>
	 * This counters our weak reference of the {@link AccountInstance}
	 */
	@SuppressWarnings( "unused" )
	private AccountInstance strongReference = null;
	
	/**
	 * Weak references the {@link PermissibleEntity} over at the Permission Manager.<br>
	 * Again, we use the {@link WeakReference} so it can be garbage collected when unused,<br>
	 * we reload it from the Permission Manager once needed again.
	 */
	private WeakReference<PermissibleEntity> permissibleEntity = null;
	
	/**
	 * Indicates if we should keep the Account Instance loaded in Memory
	 */
	private boolean keepInMemory = false;
	
	AccountMeta( AccountContext context )
	{
		Validate.notNull( context );
		
		context.setAccount( this );
		
		this.context = context;
		acctId = context.getAcctId();
		siteId = context.getSiteId();
		
		metadata.putAll( context.getValues() );
		
		/**
		 * Populate the PermissibleEntity for reasons... and notify the Account Creator
		 */
		context.creator().successInit( this, getEntity() );
	}
	
	public boolean containsKey( String key )
	{
		return metadata.containsKey( key );
	}
	
	/**
	 * Returns the {@link AccountContext} responsible for our existence
	 * 
	 * @return
	 *         Instance of AccountContext
	 */
	public AccountContext context()
	{
		return context;
	}
	
	public Boolean getBoolean( String key )
	{
		try
		{
			return ObjectFunc.castToBoolWithException( metadata.get( key ) );
		}
		catch ( ClassCastException e )
		{
			return false;
		}
	}
	
	@Override
	public String getDisplayName()
	{
		String name = context.creator().getDisplayName( this );
		return ( name == null ) ? getId() : name;
	}
	
	@Override
	public PermissibleEntity getEntity()
	{
		if ( permissibleEntity == null || permissibleEntity.get() == null )
			permissibleEntity = new WeakReference<PermissibleEntity>( PermissionManager.INSTANCE.getEntity( getId() ) );
		
		return permissibleEntity.get();
	}
	
	@Override
	public String getId()
	{
		return acctId;
	}
	
	public Integer getInteger( String key )
	{
		return getInteger( key, 0 );
	}
	
	public Integer getInteger( String key, int def )
	{
		Object obj = metadata.get( key );
		Integer val = ObjectFunc.castToInt( obj );
		
		return ( val == null ) ? def : val;
	}
	
	public Set<String> getKeys()
	{
		return metadata.keySet();
	}
	
	public String getLogoffMessage()
	{
		return getId() + " has logged off the server";
	}
	
	public Map<String, Object> getMeta()
	{
		return Collections.unmodifiableMap( metadata );
	}
	
	public Object getObject( String key )
	{
		return metadata.get( key );
	}
	
	@Override
	public Site getSite()
	{
		return SiteManager.INSTANCE.getSiteById( siteId );
	}
	
	@Override
	public String getSiteId()
	{
		return siteId;
	}
	
	public String getString( String key )
	{
		return getString( key, null );
	}
	
	public String getString( String key, String def )
	{
		String val = ObjectFunc.castToString( metadata.get( key ) );
		return ( val == null ) ? def : val;
	}
	
	private AccountInstance initAccount()
	{
		AccountInstance account = new AccountInstance( this );
		this.account = new WeakReference<AccountInstance>( account );
		
		if ( keepInMemory )
			strongReference = account;
		
		AccountManager.INSTANCE.fireAccountLoad( this );
		
		return account;
	}
	
	@Override
	public AccountInstance instance()
	{
		if ( !isInitialized() )
			initAccount();
		
		return account.get();
	}
	
	@Override
	public boolean isInitialized()
	{
		return account != null;
	}
	
	@Override
	public Iterator<Entry<String, Object>> iterator()
	{
		return Collections.unmodifiableMap( metadata ).entrySet().iterator();
	}
	
	/**
	 * Returns if the Account is will be kept in memory
	 * If you want to know if the Account is currently being kept in memory, See {@link #keptInMemory()}
	 * 
	 * @return
	 *         Will be kept in memory?
	 */
	public boolean keepInMemory()
	{
		return isInitialized() ? keepInMemory : false;
	}
	
	/**
	 * Sets if the Account should stay loaded in the VM memory
	 * 
	 * @param state
	 *            Stay in memory?
	 */
	public void keepInMemory( boolean state )
	{
		strongReference = ( state ) ? account.get() : null;
		keepInMemory = state;
	}
	
	/**
	 * Returns if the Account is being kept in memory
	 * If you want to know if the Account will be kept in memory, See {@link #keepInMemory()}
	 * 
	 * @return
	 *         Is being kept in memory? Will always return false if the Account is not initialized.
	 */
	public boolean keptInMemory()
	{
		return isInitialized() ? keepInMemory : false;
	}
	
	public Set<String> keySet()
	{
		return Collections.unmodifiableSet( metadata.keySet() );
	}
	
	@Override
	public AccountMeta meta()
	{
		return this;
	}
	
	public void reload()
	{
		context.creator().reload( this );
	}
	
	public void requireActivation()
	{
		metadata.put( "actnum", RandomFunc.randomize( "z154f98wfjascvc" ) );
	}
	
	public void save()
	{
		context.creator().save( this );
	}
	
	public void set( String key, Object obj )
	{
		Validate.notNull( key );
		
		if ( obj == null )
			metadata.remove( key );
		else
			metadata.put( key, obj );
	}
	
	@Override
	public String toString()
	{
		return "AccountMeta{acctId=" + acctId + ",siteId=" + siteId + "," + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( metadata ) + "}";
	}
}
