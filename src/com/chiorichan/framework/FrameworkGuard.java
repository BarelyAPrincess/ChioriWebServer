package com.chiorichan.framework;

class FrameworkGuard
{
	protected Framework _fw;
	protected long timeout = 0;
	protected int requestCnt = 0;
	
	protected FrameworkGuard(Framework fw)
	{
		_fw = fw;
		timeout = System.currentTimeMillis() + 10000;
	}
	
	protected void rearmTimeout()
	{
		// TODO: Extend timeout even longer if a user is active.
		
		// Grant the timeout an additional 10 seconds per request
		if ( requestCnt < 5 )
			requestCnt++;
		
		timeout = System.currentTimeMillis() + 10000 + (requestCnt * 10000);
	}
}
