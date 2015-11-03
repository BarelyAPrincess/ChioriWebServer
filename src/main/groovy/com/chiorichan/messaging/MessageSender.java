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
 * Represents entities with the ability to send messages thru the {@link MessageDispatch}
 */
public interface MessageSender
{
	String getDisplayName();
	
	String getId();
	
	PermissibleEntity getEntity();
}
