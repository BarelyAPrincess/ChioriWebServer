package com.chiorichan.user;

import com.chiorichan.framework.Site;

public interface UserHandler
{
	void kick( String kickMessage );

	void sendMessage( String[] messages );

	Site getSite();
}
