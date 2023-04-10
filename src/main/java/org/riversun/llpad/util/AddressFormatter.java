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
package org.riversun.llpad.util;

/**
 * Address formatter
 * 
 * @author Tom Misawa (riversun.org@gmail.com)
 *
 */
public class AddressFormatter {

	private static final long KILO_BYTE = 1024;
	private static final long MEGA_BYTE = 1024 * KILO_BYTE;
	private static final long GIGA_BYTE = 1024 * MEGA_BYTE;
	private static final long TERA_BYTE = 1024 * GIGA_BYTE;

	public static String getHex10X(long addr) {
		return String.format("0x%010X", addr);
	}

	public static String getFmtAddress(long addr) {
		return getHex10X(addr) + " (" + addr + ")";
	}

	public static String getGiByteF1(long addr) {
		if (addr > TERA_BYTE) {
			double bytes = (double) addr / (double) TERA_BYTE;
			return String.format("%.1fTB", bytes);
		}
		else if (addr > GIGA_BYTE) {
			double bytes = (double) addr / (double) GIGA_BYTE;
			return String.format("%.1fGB", bytes);
		}
		else if (addr > MEGA_BYTE) {
			double bytes = (double) addr / (double) MEGA_BYTE;
			return String.format("%.1fMB", bytes);
		}
		else if (addr > KILO_BYTE) {
			double bytes = (double) addr / (double) KILO_BYTE;
			return String.format("%.1fKB", bytes);
		}
		return String.format("%.1fB", addr);
	}

	public static String getGiByteF2(long addr) {
		if (addr > TERA_BYTE) {
			double bytes = (double) addr / (double) TERA_BYTE;
			return String.format("%.2fTB", bytes);
		}
		else if (addr > GIGA_BYTE) {
			double bytes = (double) addr / (double) GIGA_BYTE;
			return String.format("%.2fGB", bytes);
		}
		else if (addr > MEGA_BYTE) {
			double bytes = (double) addr / (double) MEGA_BYTE;
			return String.format("%.2fMB", bytes);
		}
		else if (addr > KILO_BYTE) {
			double bytes = (double) addr / (double) KILO_BYTE;
			return String.format("%.2fKB", bytes);
		}
		return String.format("%.2fB", addr);
	}

	public static String getGiByteF3(long addr) {
		if (addr > TERA_BYTE) {
			double bytes = (double) addr / (double) TERA_BYTE;
			return String.format("%.3fTB", bytes);
		}
		else if (addr > GIGA_BYTE) {
			double bytes = (double) addr / (double) GIGA_BYTE;
			return String.format("%.3fGB", bytes);
		}
		else if (addr > MEGA_BYTE) {
			double bytes = (double) addr / (double) MEGA_BYTE;
			return String.format("%.3fMB", bytes);
		}
		else if (addr > KILO_BYTE) {
			double bytes = (double) addr / (double) KILO_BYTE;
			return String.format("%.3fKB", bytes);
		}
		return String.format("%.3fB", addr);
	}

}
