/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.net;

import com.chiorichan.util.Common;
import com.esotericsoftware.kryonet.Connection;

public abstract class Packet
{
	public int creation = 0;
	
	public Packet()
	{
		creation = Common.getEpoch();
	}
	
	/**
	 * Override this method if you would like the packet to be notified when it reaches it's destination.
	 */
	public boolean received( Connection var1 )
	{
		return false;
	}
}