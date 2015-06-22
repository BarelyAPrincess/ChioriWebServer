/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.account.event;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountPermissible;
import com.chiorichan.account.AccountType;
import com.chiorichan.event.Cancellable;
import com.google.common.collect.Lists;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class AccountMessageEvent extends AccountEvent implements Cancellable
{
	private Set<AccountPermissible> recipients;
	private List<Object> objs;
	private boolean cancelled = false;
	
	public AccountMessageEvent( final Account sender, final Set<AccountPermissible> recipients, final Object... objs )
	{
		super( ( sender == null ) ? AccountType.ACCOUNT_NONE : sender );
		this.recipients = recipients;
		this.objs = Arrays.asList( objs );
	}
	
	public boolean containsRecipient( Account acct )
	{
		for ( Account acct1 : recipients )
			if ( acct1.metadata() == acct.metadata() )
				return true;
		return false;
	}
	
	public boolean removeRecipient( Account acct )
	{
		for ( Account acct1 : recipients )
			if ( acct1.metadata() == acct.metadata() )
				return recipients.remove( acct1 );
		return false;
	}
	
	public void addRecipient( AccountPermissible acct )
	{
		recipients.add( acct );
	}
	
	public void addRecipient( Account acct )
	{
		for ( AccountPermissible perm : acct.instance().getPermissibles() )
			recipients.add( perm );
	}
	
	public Set<AccountPermissible> getRecipients()
	{
		return recipients;
	}
	
	public void setRecipients( Set<AccountPermissible> recipients )
	{
		this.recipients = recipients;
	}
	
	public String[] getMessage()
	{
		return ( String[] ) getObject( String.class );
	}
	
	@SuppressWarnings( "unchecked" )
	public <T> T[] getObject( Class<?> clz )
	{
		List<T> o = Lists.newArrayList();
		for ( Object obj : objs )
			if ( obj.getClass() == clz )
				o.add( ( T ) obj );
		return ( T[] ) o.toArray();
	}
	
	public List<Object> getMessages()
	{
		return objs;
	}
	
	/**
	 * WARNING! This will completely clear and reset the messages.
	 * 
	 * @param objs
	 *            The new messages
	 */
	public void setMessages( Iterable<Object> objs )
	{
		this.objs = Lists.newArrayList( objs );
	}
	
	public void addMessage( Object obj )
	{
		objs.add( obj );
	}
	
	public Object removeMessage( int index )
	{
		return objs.remove( index );
	}
	
	public boolean removeMessage( Object obj )
	{
		return objs.remove( obj );
	}
	
	/**
	 * WARNING! This will completely clear and reset the messages.
	 * 
	 * @param objs
	 *            The new messages
	 */
	public void setMessages( Object... objs )
	{
		this.objs = Arrays.asList( objs );
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		this.cancelled = cancel;
	}
}
