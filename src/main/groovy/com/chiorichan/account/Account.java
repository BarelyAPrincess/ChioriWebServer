/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.account;

import java.util.Set;

import com.chiorichan.permission.Permission;
import com.chiorichan.permission.PermissionResult;
import com.chiorichan.site.Site;

/**
 * 
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public interface Account
{
	/**
	 * Checks is this Account is banned on this server
	 * 
	 * @return is banned?
	 */
	boolean isBanned();
	
	/**
	 * Checks if this Account is whitelisted on this server
	 * 
	 * @return is whitelisted?
	 */
	boolean isWhitelisted();
	
	/**
	 * Check if the Account is an administrator of the server
	 * 
	 * @return is Server Administrator?
	 */
	boolean isAdmin();
	
	/**
	 * Checks if the Account is an operator of the server
	 * 
	 * @return is Server Operator?
	 */
	boolean isOp();
	
	/**
	 * Kicks the Account from the server
	 * If called on {@link AccountPermissible} only that instance will be kicked.
	 * If called on {@link AccountInstance} or {@link AccountMeta} all accounts will be kicked.
	 * 
	 * @param msg
	 *            The kick message you wish to has delivered
	 * @return Was the kick successfully executed
	 */
	boolean kick( String msg );
	
	/**
	 * Gets an array of known IP Addresses currently in use
	 * 
	 * @return an array of IPv4/IPv6 Addresses
	 */
	Set<String> getIpAddresses();
	
	/**
	 * Returns the exact instance of AccountMeta
	 * 
	 * @return {@link AccountMeta} instance of this Account
	 */
	AccountMeta metadata();
	
	/**
	 * Returns the exact instance of AccountMeta
	 * 
	 * @return {@link AccountInstance} instance of this Account
	 */
	AccountInstance instance();
	
	/**
	 * Returns the AcctId for this Account
	 * 
	 * @return The AcctId
	 */
	String getAcctId();
	
	/**
	 * Returns the SiteId associated with this account
	 * 
	 * @return
	 *         The associated SiteId
	 */
	String getSiteId();
	
	/**
	 * Returns the {@link Site} associated with this account
	 * 
	 * @return
	 *         The associated {@link Site}
	 */
	Site getSite();
	
	/**
	 * Produces an Account Display Name, e.g., John Smith
	 * 
	 * @return
	 *         A human readable display name
	 */
	String getDisplayName();
	
	void send( Object obj );
	
	void send( Account sender, Object obj );
	
	/**
	 * Checks the current permission of this {@link AccountPermissible} to the {@link Permission}
	 */
	PermissionResult checkPermission( String perm );
	
	/**
	 * Checks the current permission of this {@link AccountPermissible} to the {@link Permission}
	 */
	PermissionResult checkPermission( Permission perm );
}
