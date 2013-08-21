package com.chiorichan.server;


public class ServiceBean implements IService
{
	@Override
	public String hello()
	{
		return "Hello, Rainbow Dash!!! :D";
	}

	@Override
	public String reloadScreen( int monitorNo )
	{
		return "An error has occured!";
	}
}
