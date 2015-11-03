/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.lang;

import com.chiorichan.permission.Permission;

/**
 * Used to communicate Permission Denied back to a calling EvalFactory.
 */
public class PermissionDeniedException extends PermissionException
{
	public enum PermissionDeniedReason
	{
		LOGIN_PAGE, OP_ONLY, DENIED;
		
		Permission perm = null;
		int httpCode = 401;
		
		public PermissionDeniedReason setPermission( Permission perm )
		{
			this.perm = perm;
			return this;
		}
		
		public String getMessage()
		{
			switch ( this )
			{
				case LOGIN_PAGE:
					return "You must be logged in to view this page!";
				case OP_ONLY:
					return "This page is limited to server operators only!";
				case DENIED:
					if ( perm != null )
						return "This page is limited to logins with access to the \"" + perm.getNamespace() + "\" permission.";
					return "This page is limited to logins with an unspecified permission.";
				default:
					return "<Unknown Reason>";
			}
		}
		
		public PermissionDeniedReason setHttpCode( int httpCode )
		{
			this.httpCode = httpCode;
			return this;
		}
		
		public int getHttpCode()
		{
			return httpCode;
		}
	}
	
	private static final long serialVersionUID = -6010688682541616132L;
	final PermissionDeniedReason reason;
	
	public PermissionDeniedException( PermissionDeniedReason reason )
	{
		super( reason.getMessage() );
		this.reason = reason;
	}
	
	public PermissionDeniedReason getReason()
	{
		return reason;
	}
	
	public int getHttpCode()
	{
		return reason.getHttpCode();
	}
	
	@Override
	public String getMessage()
	{
		return reason.getMessage();
	}
}
