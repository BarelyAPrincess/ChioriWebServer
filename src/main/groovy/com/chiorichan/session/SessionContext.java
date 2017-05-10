/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.session;

import com.chiorichan.site.Site;

/**
 * Provides a context to when a session is created
 */
public interface SessionContext
{
	String getIpAddress();

	Site getLocation();
}
