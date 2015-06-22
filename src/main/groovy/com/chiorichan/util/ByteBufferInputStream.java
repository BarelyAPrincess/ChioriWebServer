/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Wraps a ByteBuffer in an InputStream for reading
 */
public class ByteBufferInputStream extends InputStream
{
	ByteBuffer buf;
	
	public ByteBufferInputStream( ByteBuffer buf )
	{
		this.buf = buf;
	}
	
	public int read() throws IOException
	{
		if ( !buf.hasRemaining() )
		{
			return -1;
		}
		return buf.get() & 0xFF;
	}
	
	public int read( byte[] bytes, int off, int len ) throws IOException
	{
		if ( !buf.hasRemaining() )
		{
			return -1;
		}
		
		len = Math.min( len, buf.remaining() );
		buf.get( bytes, off, len );
		return len;
	}
}
