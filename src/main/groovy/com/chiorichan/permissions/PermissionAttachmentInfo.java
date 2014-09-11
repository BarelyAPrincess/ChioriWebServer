/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permissions;

/**
 * Holds information on a permission and which {@link PermissionAttachment} provides it
 */
public class PermissionAttachmentInfo
{
	private final Permissible permissible;
	private final String permission;
	private final PermissionAttachment attachment;
	private final boolean value;
	
	public PermissionAttachmentInfo(Permissible permissible, String permission, PermissionAttachment attachment, boolean value)
	{
		if ( permissible == null )
		{
			throw new IllegalArgumentException( "Permissible may not be null" );
		}
		else if ( permission == null )
		{
			throw new IllegalArgumentException( "Permissions may not be null" );
		}
		
		this.permissible = permissible;
		this.permission = permission;
		this.attachment = attachment;
		this.value = value;
	}
	
	/**
	 * Gets the permissible this is attached to
	 * 
	 * @return Permissible this permission is for
	 */
	public Permissible getPermissible()
	{
		return permissible;
	}
	
	/**
	 * Gets the permission being set
	 * 
	 * @return Name of the permission
	 */
	public String getPermission()
	{
		return permission;
	}
	
	/**
	 * Gets the attachment providing this permission. This may be null for default permissions (usually parent
	 * permissions).
	 * 
	 * @return Attachment
	 */
	public PermissionAttachment getAttachment()
	{
		return attachment;
	}
	
	/**
	 * Gets the value of this permission
	 * 
	 * @return Value of the permission
	 */
	public boolean getValue()
	{
		return value;
	}
}
