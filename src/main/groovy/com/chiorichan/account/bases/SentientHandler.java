/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.bases;

public interface SentientHandler
{
	public boolean kick( String kickMessage );

	public void sendMessage( String... msg );

	public void attachSentient( Sentient sentient );
	
	public void removeSentient();
	
	public boolean isValid();

	public Sentient getSentient();
	
	public String getIpAddr();

	public String getName();
}
