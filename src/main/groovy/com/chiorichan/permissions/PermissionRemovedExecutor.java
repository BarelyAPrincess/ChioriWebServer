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
 * Represents a class which is to be notified when a {@link PermissionAttachment} is removed from a {@link Permissible}
 */
public interface PermissionRemovedExecutor
{
	/**
	 * Called when a {@link PermissionAttachment} is removed from a {@link Permissible}
	 * 
	 * @param attachment
	 *           Attachment which was removed
	 */
	public void attachmentRemoved( PermissionAttachment attachment );
}
