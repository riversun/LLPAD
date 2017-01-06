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
package org.riversun.llpad.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * Detect encoding and line feed of text file
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 */
public class TextFileInfoHelper {

	/**
	 * Detect encoding of text file
	 * 
	 * @param text
	 *            file
	 * @return
	 */
	public String detectEncoding(File file) {

		final byte[] bufferToRead = new byte[4096];

		String result = null;

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);

			UniversalDetector detector = new UniversalDetector(null);

			int bytesRead;
			while ((bytesRead = fis.read(bufferToRead)) > 0 && !detector.isDone()) {
				detector.handleData(bufferToRead, 0, bytesRead);
			}
			detector.dataEnd();

			result = detector.getDetectedCharset();

			detector.reset();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
		return result;
	}

	/**
	 * Detect line feed chars
	 * 
	 * @param file
	 * @param charset
	 * @return
	 */
	public String detectLineFeed(File file, String charset) {
		String ret = "";
		FileInputStream fis = null;
		InputStreamReader isr = null;
		final int maxRead = 4096;
		try {
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis, charset);

			int count = 0;
			int ch = isr.read();
			StringBuilder sb = new StringBuilder();

			while (ch != -1) {

				ch = isr.read();
				sb.append((char) ch);
				count++;
				if (count == maxRead) {
					break;
				}
			}

			int nIndex = sb.indexOf("\n");
			int rIndex = sb.indexOf("\r");

			if (nIndex > -1 && rIndex > -1) {
				if (nIndex < rIndex) {
					ret = "\n\r";
				} else {
					ret = "\r\n";
				}
			} else if (nIndex > -1) {
				ret = "\n";
			} else if (rIndex > -1) {
				ret = "\r";
			}
			if (nIndex < 0 && rIndex < 0) {
				ret = null;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {

				}
			}
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {

				}
			}
		}
		return ret;
	}

}
