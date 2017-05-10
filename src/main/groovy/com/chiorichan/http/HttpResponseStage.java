/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http;

public enum HttpResponseStage
{
	READING( 0 ), WRITING( 1 ), WRITTEN( 2 ), CLOSED( 3 ), MULTIPART( 4 );
	
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
