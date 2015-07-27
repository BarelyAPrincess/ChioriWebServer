/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.account.AccountManager;
import com.chiorichan.account.Kickable;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.Cancellable;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.SelfHandling;
import com.google.common.collect.Sets;

/**
 * Called when a User gets kicked from the server
 */
public class KickEvent extends ServerEvent implements Cancellable, SelfHandling
{
	private String leaveMessage;
	private String kickReason;
	private final Set<Kickable> kickables = Sets.newHashSet();
	private AccountResult result = null;
	private Boolean cancel = false;
	
	private KickEvent()
	{
		
	}
	
	public static KickEvent kick( Collection<Kickable> kickables )
	{
		Validate.notNull( kickables );
		return new KickEvent().setKickables( kickables );
	}
	
	public static KickEvent kick( Kickable... kickables )
	{
		return new KickEvent().setKickables( Arrays.asList( kickables ) );
	}
	
	public KickEvent addKickables( Collection<Kickable> kickables )
	{
		Validate.notNull( kickables );
		this.kickables.addAll( kickables );
		return this;
	}
	
	public AccountResult fire()
	{
		EventBus.INSTANCE.callEvent( this );
		return result;
	}
	
	/**
	 * Gets the leave message send to all online Users
	 * 
	 * @return string kick reason
	 */
	public String getLeaveMessage()
	{
		return leaveMessage;
	}
	
	/**
	 * Gets the reason why the User is getting kicked
	 * 
	 * @return string kick reason
	 */
	public String getReason()
	{
		return kickReason;
	}
	
	public AccountResult getResult()
	{
		return result;
	}
	
	@Override
	public void handle()
	{
		if ( isCancelled() )
			return;
		
		Set<String> kicked = Sets.newHashSet();
		
		for ( Kickable kickable : kickables )
			if ( !kicked.contains( kickable.getId() ) )
			{
				kicked.add( kickable.getId() );
				AccountResult outcome = kickable.kick( kickReason );
				if ( outcome != AccountResult.SUCCESS )
					AccountManager.getLogger().warning( String.format( "We failed to kick `%s` with reason `%s`", kickable.getId(), outcome.getMessage() ) );
			}
		
		result = AccountResult.SUCCESS;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancel;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		this.cancel = cancel;
		if ( cancel )
			result = AccountResult.CANCELLED_BY_EVENT;
	}
	
	public KickEvent setKickables( Collection<Kickable> kickables )
	{
		Validate.notNull( kickables );
		this.kickables.clear();
		addKickables( kickables );
		return this;
	}
	
	/**
	 * Sets the leave message send to all online Users
	 * 
	 * @param leaveMessage
	 *            leave message
	 */
	public KickEvent setLeaveMessage( String leaveMessage )
	{
		this.leaveMessage = leaveMessage;
		return this;
	}
	
	/**
	 * Sets the reason why the User is getting kicked
	 * 
	 * @param kickReason
	 *            kick reason
	 */
	public KickEvent setReason( String kickReason )
	{
		this.kickReason = kickReason;
		return this;
	}
}
