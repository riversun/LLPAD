/*
 * "LLPAD" You can open a very big document (more than 10GBytes) with ease
 * 
 * Copyright 2016-2017 Tom Misawa, riversun.org@gmail.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in the 
 * Software without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the 
 * Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package org.riversun.llpad.util.file_buffer;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * A class to read a part of a file
 *
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class FilePartialReader {

	private static final Logger LOGGER = Logger.getLogger(FilePartialReader.class.getName());

	/**
	 * Read specified range and return byte sequence
	 * 
	 * @param file
	 *            target file
	 * @param startAddr
	 *            starting address
	 * @param endAddr
	 *            ending address
	 * @return
	 */
	public byte[] read(File file, long startAddr, long endAddr) {

		if (endAddr < 0 || startAddr < 0 || startAddr > endAddr) {
			throw new RuntimeException("Specified address is incorrect.");
		}

		FileChannel readChannel = null;

		try {
			readChannel = FileChannel.open(Paths.get(file.getAbsolutePath()), StandardOpenOption.READ);

			final int readLen = (int) (endAddr - startAddr + 1);

			final byte[] result = new byte[readLen];

			MappedByteBuffer mappedByteBuffer = readChannel.map(FileChannel.MapMode.READ_ONLY, startAddr, readLen);
			mappedByteBuffer.get(result, 0, readLen);

			LOGGER.fine("file=" + file.getAbsolutePath() + " read addr range=(" + startAddr + "-" + endAddr + ") readLen=" + result.length);

			return result;

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read", e);
		}
		return null;
	}
}
