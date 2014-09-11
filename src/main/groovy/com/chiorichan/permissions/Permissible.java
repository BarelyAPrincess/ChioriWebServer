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

import java.util.Set;

import com.chiorichan.plugin.Plugin;

public abstract class Permissible
{
	public abstract String getId();
	
	/**
	 * Checks if this object contains an override for the specified permission, by fully qualified name
	 * 
	 * @param name
	 *             Name of the permission
	 * @return true if the permission is set, otherwise false
	 */
	public abstract boolean isPermissionSet( String name );
	
	/**
	 * Checks if this object contains an override for the specified {@link Permission}
	 * 
	 * @param perm
	 *             Permission to check
	 * @return true if the permission is set, otherwise false
	 */
	public abstract boolean isPermissionSet( Permission perm );
	
	/**
	 * Gets the value of the specified permission, if set.
	 * <p>
	 * If a permission override is not set on this object, the default value of the permission will be returned.
	 * 
	 * @param name
	 *             Name of the permission
	 * @return Value of the permission
	 */
	public abstract boolean hasPermission( String name );
	
	/**
	 * Gets the value of the specified permission, if set.
	 * <p>
	 * If a permission override is not set on this object, the default value of the permission will be returned
	 * 
	 * @param perm
	 *             Permission to get
	 * @return Value of the permission
	 */
	public abstract boolean hasPermission( Permission perm );
	
	/**
	 * Adds a new {@link PermissionAttachment} with a single permission by name and value
	 * 
	 * @param plugin
	 *             Plugin responsible for this attachment, may not be null or disabled
	 * @param name
	 *             Name of the permission to attach
	 * @param value
	 *             Value of the permission
	 * @return The PermissionAttachment that was just created
	 */
	public abstract PermissionAttachment addAttachment( Plugin plugin, String name, boolean value );
	
	/**
	 * Adds a new empty {@link PermissionAttachment} to this object
	 * 
	 * @param plugin
	 *             Plugin responsible for this attachment, may not be null or disabled
	 * @return The PermissionAttachment that was just created
	 */
	public abstract PermissionAttachment addAttachment( Plugin plugin );
	
	/**
	 * Temporarily adds a new {@link PermissionAttachment} with a single permission by name and value
	 * 
	 * @param plugin
	 *             Plugin responsible for this attachment, may not be null or disabled
	 * @param name
	 *             Name of the permission to attach
	 * @param value
	 *             Value of the permission
	 * @param ticks
	 *             Amount of ticks to automatically remove this attachment after
	 * @return The PermissionAttachment that was just created
	 */
	public abstract PermissionAttachment addAttachment( Plugin plugin, String name, boolean value, int ticks );
	
	/**
	 * Temporarily adds a new empty {@link PermissionAttachment} to this object
	 * 
	 * @param plugin
	 *             Plugin responsible for this attachment, may not be null or disabled
	 * @param ticks
	 *             Amount of ticks to automatically remove this attachment after
	 * @return The PermissionAttachment that was just created
	 */
	public abstract PermissionAttachment addAttachment( Plugin plugin, int ticks );
	
	/**
	 * Removes the given {@link PermissionAttachment} from this object
	 * 
	 * @param attachment
	 *             Attachment to remove
	 * @throws IllegalArgumentException
	 *              Thrown when the specified attachment isn't part of this object
	 */
	public abstract void removeAttachment( PermissionAttachment attachment );
	
	/**
	 * Recalculates the permissions for this object, if the attachments have changed values.
	 * <p>
	 * This should very rarely need to be called from a plugin.
	 */
	public abstract void recalculatePermissions();
	
	/**
	 * Gets a set containing all of the permissions currently in effect by this object
	 * 
	 * @return Set of currently effective permissions
	 */
	public abstract Set<PermissionAttachmentInfo> getEffectivePermissions();
}
