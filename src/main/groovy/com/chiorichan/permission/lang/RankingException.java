/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.lang;

import com.chiorichan.permission.PermissibleBase;

public class RankingException extends PermissionException
{
	private static final long serialVersionUID = -328357153481259189L;
	
	protected PermissibleBase target = null;
	protected PermissibleBase promoter = null;
	
	public RankingException( String message, PermissibleBase target, PermissibleBase promoter )
	{
		super( message );
		this.target = target;
		this.promoter = promoter;
	}
	
	public PermissibleBase getTarget()
	{
		return target;
	}
	
	public PermissibleBase getPromoter()
	{
		return promoter;
	}
}
