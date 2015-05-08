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
import com.chiorichan.util.ObjectFunc;
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
		this.context = context;
		this.acctId = context.getAcctId();
		this.siteId = context.getSiteId();
		
		metadata.putAll( context.getValues() );
		
		/**
		 * Populate the PermissibleEntity for reasons...
		 */
		getPermissibleEntity();
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
		return this.isInitialized() ? keepInMemory : false;
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
		return this.isInitialized() ? keepInMemory : false;
	}
	
	public Map<String, Object> getMeta()
	{
		return Collections.unmodifiableMap( metadata );
	}
	
	public Set<String> keySet()
	{
		return Collections.unmodifiableSet( metadata.keySet() );
	}
	
	public Object getObject( String key )
	{
		return metadata.get( key );
	}
	
	public void set( String key, Object obj )
	{
		Validate.notNull( key );
		
		if ( obj == null )
			metadata.remove( key );
		else
			metadata.put( key, obj );
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
	
	public Boolean getBoolean( String key )
	{
		Object obj = metadata.get( key );
		
		if ( obj instanceof String )
			return Boolean.parseBoolean( ( String ) obj );
		else
			return ( Boolean ) obj;
	}
	
	public String toString()
	{
		return "AccountMeta{acctId=" + acctId + ",siteId=" + siteId + "," + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( metadata ) + "}";
	}
	
	public boolean containsKey( String key )
	{
		return metadata.containsKey( key );
	}
	
	@Override
	public String getAcctId()
	{
		return acctId;
	}
	
	@Override
	public String getHumanReadableName()
	{
		String name = context.creator().getHumanReadableName( this );
		return ( name == null ) ? getAcctId() : name;
	}
	
	public String getSiteId()
	{
		return siteId;
	}
	
	public String getLogoffMessage()
	{
		return this.getAcctId() + " has logged off the server";
	}
	
	public PermissibleEntity getPermissibleEntity()
	{
		if ( permissibleEntity == null || permissibleEntity.get() == null )
			permissibleEntity = new WeakReference<PermissibleEntity>( PermissionManager.INSTANCE.getEntity( getAcctId() ) );
		
		return permissibleEntity.get();
	}
	
	public boolean isInitialized()
	{
		return account != null;
	}
	
	@Override
	public boolean isBanned()
	{
		return getPermissibleEntity().isBanned();
	}
	
	@Override
	public boolean isWhitelisted()
	{
		return getPermissibleEntity().isWhitelisted();
	}
	
	@Override
	public boolean isAdmin()
	{
		return getPermissibleEntity().isAdmin();
	}
	
	@Override
	public boolean isOp()
	{
		return getPermissibleEntity().isOp();
	}
	
	@Override
	public boolean kick( String msg )
	{
		return AccountManager.INSTANCE.kick( this, msg );
	}
	
	@Override
	public Set<String> getIpAddresses()
	{
		return instance().getIpAddresses();
	}
	
	@Override
	public AccountMeta metadata()
	{
		return this;
	}
	
	@Override
	public AccountInstance instance()
	{
		if ( !isInitialized() )
			initAccount();
		
		return account.get();
	}
	
	public void reload()
	{
		context.creator().reload( this );
	}
	
	public void save()
	{
		context.creator().save( this );
	}
	
	@Override
	public Site getSite()
	{
		return null;
	}
	
	public AccountContext getContext()
	{
		return context;
	}
	
	@Override
	public Iterator<Entry<String, Object>> iterator()
	{
		return Collections.unmodifiableMap( metadata ).entrySet().iterator();
	}
	
	public Set<String> getKeys()
	{
		return metadata.keySet();
	}
	
	@Override
	public void send( Object obj )
	{
		instance().send( obj );
	}
	
	@Override
	public void send( Account sender, Object obj )
	{
		instance().send( sender, obj );
	}
}
