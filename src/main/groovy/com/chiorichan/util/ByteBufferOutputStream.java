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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Wraps a ByteBuffer in an OutputStream for writing
 */
public class ByteBufferOutputStream extends OutputStream
{
	ByteBuffer buf;
	
	public ByteBufferOutputStream( ByteBuffer buf )
	{
		this.buf = buf;
	}
	
	public void write( int b ) throws IOException
	{
		buf.put( ( byte ) b );
	}
	
	public void write( byte[] bytes, int off, int len ) throws IOException
	{
		buf.put( bytes, off, len );
	}
}
