/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

/**
 * Method used to keep a Session persistent from request to request.
 * 
 * XXX This is an outdated feature from like version 6, not sure if it's still working as of version 9.
 */
public enum SessionPersistenceMethod
{
	COOKIE, PARAM
}
