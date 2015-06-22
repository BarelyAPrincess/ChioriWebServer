/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.configuration.serialization;

import java.util.Map;

/**
 * Represents an object that may be serialized.
 * <p />
 * These objects MUST implement one of the following, in addition to the methods as defined by this interface:
 * <ul>
 * <li>A static method "deserialize" that accepts a single {@link Map}&lt;{@link String}, {@link Object}> and returns the class.</li>
 * <li>A static method "valueOf" that accepts a single {@link Map}&lt;{@link String}, {@link Object}> and returns the class.</li>
 * <li>A constructor that accepts a single {@link Map}&lt;{@link String}, {@link Object}>.</li>
 * </ul>
 * In addition to implementing this interface, you must register the class with {@link ConfigurationSerialization#registerClass(Class)}.
 * 
 * @see DelegateDeserialization
 * @see SerializableAs
 */
public interface ConfigurationSerializable
{
	/**
	 * Creates a Map representation of this class.
	 * <p />
	 * This class must provide a method to restore this class, as defined in the {@link ConfigurationSerializable} interface javadocs.
	 *
	 * @return Map containing the current state of this class
	 */
	Map<String, Object> serialize();
}
