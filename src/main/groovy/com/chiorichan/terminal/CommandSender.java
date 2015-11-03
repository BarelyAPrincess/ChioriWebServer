/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal;

/**
 * Represents entities with the ability to execute commands thru the {@link CommandDispatch}
 */
public interface CommandSender
{
	String getVariable( String key );
	
	String getVariable( String key, String def );
	
	void setVariable( String key, String val );
}
