/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to mark methods as being event handler methods
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface EventHandler
{
	/**
	 * Define the priority of the event.
	 * <p>
	 * First priority to the last priority executed:
	 * <ol>
	 * <li>LOWEST</li>
	 * <li>LOW</li>
	 * <li>NORMAL</li>
	 * <li>HIGH</li>
	 * <li>HIGHEST</li>
	 * <li>MONITOR</li>
	 * </ol>
	 */
	EventPriority priority() default EventPriority.NORMAL;
	
	/**
	 * Define if the handler ignores a cancelled event.
	 * <p>
	 * If ignoreCancelled is true and the event is cancelled, the method is not called. Otherwise, the method is always called.
	 */
	boolean ignoreCancelled() default false;
}
