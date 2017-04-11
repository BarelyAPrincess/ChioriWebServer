/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory;

/**
 * Provides an interface for which the Scripting Engine to notify Scripts of events, such as exception or before execution.
 */
public interface ScriptingEvents
{
	void onBeforeExecute( ScriptingContext context );

	void onAfterExecute( ScriptingContext context );

	void onException( ScriptingContext context, Throwable throwable );
}
