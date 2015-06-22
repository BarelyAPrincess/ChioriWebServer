/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.lang;

import java.sql.SQLException;

public class PermissionException extends RuntimeException
{
	private static final long serialVersionUID = -7126640838300697969L;
	
	public PermissionException( SQLException e )
	{
		super( e );
	}
	
	public PermissionException( String message )
	{
		super( message );
	}
}
