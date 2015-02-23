/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.permission.structure;

public enum PermissionDefault
{
	DEFAULT( "default" ), EVERYBODY( "" ), OP( "sys.op" ), ADMIN( "sys.admin" ), BANNED( "sys.banned" ), WHITELISTED( "sys.whitelisted" );
	
	private String nameSpace = "";
	
	PermissionDefault( String nameSpace )
	{
		this.nameSpace = nameSpace;
	}
	
	public String toString()
	{
		return this.name() + "(nameSpace=" + nameSpace + ")";
	}
	
	public String getNameSpace()
	{
		return nameSpace;
	}
	
	public String getLocalName()
	{
		return ( nameSpace.contains( "." ) ) ? nameSpace.substring( nameSpace.indexOf( "." ) + 1 ) : nameSpace;
	}
	
	public Permission getNode()
	{
		Permission result = Permission.getNode( nameSpace, false );
		
		if ( result == null )
			switch ( this )
			{
				case DEFAULT:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, false ), "Used as the default permission node if one does not exist. (DO NOT EDIT!)" );
					break;
				case EVERYBODY:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, true ), "The dummy node used for the 'everyone' permission check. (DO NOT EDIT!)" );
					break;
				case OP:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, false ), "Indicates OP entities. (DO NOT EDIT!)" );
					break;
				case ADMIN:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, false ), "Indicates ADMIN entities. (DO NOT EDIT!)" );
					break;
				case BANNED:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, false ), "Indicates BANNED entities. (DO NOT EDIT!)" );
					break;
				case WHITELISTED:
					result = Permission.createNode( getNameSpace(), new PermissionValueBoolean( getLocalName(), true, false ), "Indicates WHITELISTED entities. (DO NOT EDIT!)" );
					break;
			}
		
		return result;
	}
}
