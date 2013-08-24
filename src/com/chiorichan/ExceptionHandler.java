package com.chiorichan;

import java.lang.Thread.UncaughtExceptionHandler;

public class ExceptionHandler implements UncaughtExceptionHandler
{
	
	@Override
	public void uncaughtException( Thread arg0, Throwable arg1 )
	{
		arg1.printStackTrace();
	}
	
}
