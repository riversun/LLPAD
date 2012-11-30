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

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.riversun.llpad.fw.Disposable;

/**
 * 
 * Caching part of a big text file
 * <p>
 * TextBlock holds the absolute address for each character
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class TextBlock implements Disposable {

	private static final Logger LOGGER = Logger.getLogger(TextBlock.class.getName());

	private String mText = null;

	// Offset address of this text block (Starting position)
	private long mOffsetAddr = 0;

	// The global address (pointer on the file) of the Nth character string
	private long[] mCahce4PointerAtStrIndex;

	// Hold correspondence between character pointer's global address and
	// character position(this means string index).
	// lookup string index from address(the address is a relative value from the
	// offset address)
	private int[] mCache4StrIndexAtPointer;

	/**
	 * 
	 * @param offsetAddr
	 *            Starting address of this text byte when treated it as a byte
	 * @param srcBytes
	 *            Target text bytes (sequence of bytes of text)
	 * @param encoding
	 *            character encoding like "UTF-8"
	 */
	public TextBlock(long offsetAddr, byte[] srcBytes, String encoding) {

		mOffsetAddr = offsetAddr;

		mCache4StrIndexAtPointer = new int[srcBytes.length];

		try {
			mText = new String(srcBytes, encoding);

			LOGGER.fine("offsetAddr=" + offsetAddr + " srcBytes size=" + srcBytes.length + " encoding=" + encoding + " textLen=" + mText.length());

			mCahce4PointerAtStrIndex = new long[mText.length()];

			long addr = offsetAddr;

			loop: for (int strIndex = 0; strIndex < mText.length(); strIndex++) {

				// Get the byte representation of the Nth(=strIndex) character
				// You know even one character may be multi-byte
				final String sChar = mText.substring(strIndex, strIndex + 1);

				// don't forget getBytes WITH ENCODING!!
				final byte[] bytes = sChar.getBytes(encoding);

				// Store starting address corresponding to Nth character
				mCahce4PointerAtStrIndex[strIndex] = addr;

				for (int j = 0; j < bytes.length; j++) {

					if (srcBytes.length - 1 < (addr - offsetAddr)) {

						// It occurs when you create a character string with new
						// String (srcBytes, encoding),
						// srcBytes is cut off in the middle of one character,
						// and one strange(unexpected) character has to be
						// added.

						// If doing mText.getBytes (encoding),
						// it will be larger than the original srcBytes.length.
						// Therefore, when the accumulated address becomes
						// larger than the original byte array, exit the loop.
						// And cut the text

						mText = mText.substring(0, strIndex);

						LOGGER.warning("Src string (as srcBytes) may be cutted at invalid position. strIndex=" + strIndex + " addr=" + addr);

						break loop;
					}

					final int relativeAddr = getRelativeAddressForCache4Index(addr);

					// Store Nth(=strIndex) character corresponding to the
					// address
					mCache4StrIndexAtPointer[relativeAddr] = strIndex;
					addr++;
				}

			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get string index from address
	 * 
	 * @param addr
	 *            address (global)
	 * @return
	 */
	public int getStringIndexFromAddress(long addr) {

		int strIndex;

		try {
			strIndex = mCache4StrIndexAtPointer[getRelativeAddressForCache4Index(addr)];

			return strIndex;

		} catch (java.lang.ArrayIndexOutOfBoundsException e) {

			LOGGER.severe("error @ #getStringIndexFromAddress address=" + addr + " offset address=" + mOffsetAddr +
					" getRelativePointer(address)=" + getRelativeAddressForCache4Index(addr) + " mStrIndexAtPointer.size="
					+ mCache4StrIndexAtPointer.length);

			throw e;
		}

	}

	/**
	 * Returns a string at a certain area (between start address and end
	 * address)
	 * 
	 * @param startAddr
	 *            starting address
	 * @param endAddr
	 *            ending address
	 * @return
	 */
	public String getStringBetweenAdress(long startAddr, long endAddr) {

		// Get an absolute address to a subscript of strIndexAtPointer array for
		// index subscript conversion)
		final int relStartAddr = getRelativeAddressForCache4Index(startAddr);
		final int relEndAddr = getRelativeAddressForCache4Index(endAddr);

		final int startIndex = mCache4StrIndexAtPointer[relStartAddr];
		final int endIndex = mCache4StrIndexAtPointer[relEndAddr];

		String subStr = null;
		try {
			subStr = mText.substring(startIndex, endIndex + 1);
		} catch (StringIndexOutOfBoundsException e) {
			LOGGER.log(Level.SEVERE, "do substring(" + startIndex + "," + (endIndex + 1) + ") "
					+ "startAddr=" + startAddr + " endAddr=" + endAddr + " mText.leng=" + mText.length(), e);
			throw e;
		}
		return subStr;
	}

	public long getAddressFromStringIndex(int strIndex) {
		return mCahce4PointerAtStrIndex[strIndex];
	}

	public String getText() {
		return mText;
	}

	/**
	 * 
	 * Convert an absolute address to a subscript of strIndexAtPointer array for
	 * index subscript conversion)
	 * 
	 * @param addr
	 *            address(global)
	 * @return
	 */
	int getRelativeAddressForCache4Index(long addr) {
		return (int) (addr - mOffsetAddr);
	}

	/**
	 * Release object references
	 */
	@Override
	public void dispose() {
		// make sure release memory
		mText = null;
		mCahce4PointerAtStrIndex = null;
		mCache4StrIndexAtPointer = null;

	}

}