/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.messaging;

import com.chiorichan.permission.PermissibleEntity;

/**
 * Interfaces classes that can receive incoming messages from the {@link MessageDispatch}
 */
public interface MessageReceiver
{
	/**
	 * Sends message/objects to any and all CommandSenders currently logged in, referencing to this Account
	 * 
	 * @param objs
	 *            The objects to dispatch
	 */
	void sendMessage( Object... objs );
	
	/**
	 * Sends message/objects to any and all CommandSenders currently logged in, referencing to this Account
	 * Addresses the message from CommandSender
	 * 
	 * @param sender
	 *            The CommandSender
	 * @param objs
	 *            The message/objects to deliver
	 */
	void sendMessage( MessageSender sender, Object... objs );
	
	String getDisplayName();
	
	String getId();
	
	PermissibleEntity getEntity();
}
