/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.models

import com.chiorichan.datastore.sql.query.SQLQuerySelect
import com.chiorichan.logger.Log
import com.chiorichan.utils.UtilObjects

class SQLModelBuilderChild extends SQLModelBuilder
{
	SQLModelBuilder parent

	SQLModelBuilderChild( SQLModelBuilder parent, SQLQuerySelect sqlBase )
	{
		UtilObjects.notNull( parent )
		UtilObjects.notNull( sqlBase )

		this.parent = parent
		setBase( sqlBase )
	}

	@Override
	String getTable()
	{
		return parent.getTable()
	}

	SQLModelBuilder back()
	{
		return parent
	}

	/* TODO Implement additional overridable methods that pull from the parent */

	@Override
	Object run()
	{
		return null
	}
}
