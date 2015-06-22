/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.configuration.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies to a {@link ConfigurationSerializable} that will delegate all deserialization to another {@link ConfigurationSerializable}.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface DelegateDeserialization
{
	/**
	 * Which class should be used as a delegate for this classes deserialization
	 *
	 * @return Delegate class
	 */
	Class<? extends ConfigurationSerializable> value();
}
