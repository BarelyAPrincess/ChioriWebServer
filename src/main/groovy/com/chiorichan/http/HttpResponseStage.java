/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

public enum HttpResponseStage
{
	READING( 0 ), WRITTING( 1 ), WRITTEN( 2 ), CLOSED( 3 ), MULTIPART( 4 );
	
	private final int stageId;
	
	HttpResponseStage( int id )
	{
		stageId = id;
	}
	
	public int getId()
	{
		return stageId;
	}
}
