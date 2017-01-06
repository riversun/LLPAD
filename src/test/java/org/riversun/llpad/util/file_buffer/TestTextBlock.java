package org.riversun.llpad.util.file_buffer;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TestTextBlock {
	@Rule
	public TestName name = new TestName();

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private TextBlock getTextBlock() {

		byte[] srcBytes = null;

		try {
			srcBytes = "Hello World!\nαβ is alpha/beta.".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
		}

		// 48(H)[000]0
		// 65(e)[001]1
		// 6C(l)[002]2
		// 6C(l)[003]3
		// 6F(o)[004]4
		// 20( )[005]5
		// 57(W)[006]6
		// 6F(o)[007]7
		// 72(r)[008]8
		// 6C(l)[009]9
		// 64(d)[010]10
		// 21(!)[011]11
		// 0A( )[012]12
		// CE( )[013]13
		// B1( )[014]13
		// CE( )[015]14
		// B2( )[016]14
		// 20( )[017]15
		// 69(i)[018]16
		// 73(s)[019]17
		// 20( )[020]18
		// 61(a)[021]19
		// 6C(l)[022]20
		// 70(p)[023]21
		// 68(h)[024]22
		// 61(a)[025]23
		// 2F(/)[026]24
		// 62(b)[027]25
		// 65(e)[028]26
		// 74(t)[029]27
		// 61(a)[030]28
		// 2E(.)[031]29

		final long offsetAddr = 500L;

		String encoding = "UTF-8";
		TextBlock tb = new TextBlock(offsetAddr, srcBytes, encoding);
		return tb;

	}

	@Test
	public void test_getStringIndexFromAddress() {
		TextBlock tb = getTextBlock();

		assertEquals(1, tb.getStringIndexFromAddress(501));

		// alpha at 513,514
		assertEquals(13, tb.getStringIndexFromAddress(513));
		assertEquals(13, tb.getStringIndexFromAddress(514));

		// beta at 515,516
		assertEquals(14, tb.getStringIndexFromAddress(515));
		assertEquals(14, tb.getStringIndexFromAddress(516));

	}

	@Test
	public void test_getAddressFromStringIndex() {
		TextBlock tb = getTextBlock();
		assertEquals(500, tb.getAddressFromStringIndex(0));

		// if the string is multi-byte,
		// then returns starting address of that string.
		assertEquals(513, tb.getAddressFromStringIndex(13));

		// if the string is multi-byte,
		// then returns starting address of that string.
		assertEquals(515, tb.getAddressFromStringIndex(14));

	}

	@Test
	public void test_getRelativeAddressForCache4Index() {
		TextBlock tb = getTextBlock();
		assertEquals(0, tb.getRelativeAddressForCache4Index(500));
		assertEquals(15, tb.getRelativeAddressForCache4Index(515));
	}

	@Test
	public void test_getStringBetweenAdress() {
		TextBlock tb = getTextBlock();

		long startAddr = 500;
		long endAddr = 504;

		assertEquals("Hello", tb.getStringBetweenAdress(startAddr, endAddr));

	}

}