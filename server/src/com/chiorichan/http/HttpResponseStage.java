package com.chiorichan.http;

public enum HttpResponseStage
{
	READING(0),
	WRITTING(1),
	WRITTEN(2),
	CLOSED(3);
	
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