package com.chiorichan.user;

import com.chiorichan.framework.Site;

public interface UserHandler
{
	public void kick( String kickMessage );

	public void sendMessage( String[] messages );

	public Site getSite();

	public String getIpAddr();
}
