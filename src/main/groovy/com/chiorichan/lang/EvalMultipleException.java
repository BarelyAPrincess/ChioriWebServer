/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Used for when multiple exceptions were thrown
 */
public class EvalMultipleException extends Exception
{
	private static final long serialVersionUID = -659541886519281396L;
	
	private final List<EvalException> exceptions = Lists.newArrayList();
	
	public EvalMultipleException( List<EvalException> exceptions )
	{
		this.exceptions.addAll( exceptions );
	}
	
	public List<EvalException> getExceptions()
	{
		return Collections.unmodifiableList( exceptions );
	}
}
