/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.model.impl;

import java.util.Arrays;

import io.keikai.model.SBook;
import io.keikai.model.SColor;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public class ColorImpl extends AbstractColorAdv {
	private static final long serialVersionUID = 1L;
	private byte[] _rgb;
	private byte _alpha = (byte) 0xff;  //ZSS-1140

	public static final AbstractColorAdv WHITE = new ColorImpl("#FFFFFF");
	public static final AbstractColorAdv BLACK = new ColorImpl("#000000");
	public static final AbstractColorAdv RED = new ColorImpl("#FF0000");
	public static final AbstractColorAdv GREEN = new ColorImpl("#00FF00");
	public static final AbstractColorAdv BLUE = new ColorImpl("#0000FF");

	public ColorImpl(byte[] rgb) {
		if (rgb == null) {
			throw new IllegalArgumentException("null rgb array");
		} else if (rgb.length == 4) { // ZSS-1140, argb
			_alpha = rgb[0];
			_rgb = new byte[3];
			_rgb[0] = rgb[1];
			_rgb[1] = rgb[2];
			_rgb[2] = rgb[3];
		} else if (rgb.length != 3) {
			throw new IllegalArgumentException("wrong rgb length");
		} else {
			this._rgb = rgb;
		}
	}

	public ColorImpl(byte r, byte g, byte b) {
		this._rgb = new byte[] { r, g, b };
	}

	public ColorImpl(String htmlColor) {
		final int offset = htmlColor.charAt(0) == '#' ? 1 : 0;
		final short red = Short.parseShort(
				htmlColor.substring(offset + 0, offset + 2), 16); // red
		final short green = Short.parseShort(
				htmlColor.substring(offset + 2, offset + 4), 16); // green
		final short blue = Short.parseShort(
				htmlColor.substring(offset + 4, offset + 6), 16); // blue
		final byte r = (byte) (red & 0xff);
		final byte g = (byte) (green & 0xff);
		final byte b = (byte) (blue & 0xff);
		this._rgb = new byte[] { r, g, b };
	}
	
	private static final char HEX[] = {
		'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' //lower case to compatible with 3.0
	};

	@Override
	public String getHtmlColor() {
		StringBuilder sb = new StringBuilder("#");
		for(byte c:_rgb){
			int n = c & 0xff;
			sb.append(HEX[n/16]);//high
			sb.append(HEX[n%16]);//low
		}
		return sb.toString();
	}

	@Override
	public byte[] getRGB() {
		byte[] c = new byte[_rgb.length];
		System.arraycopy(_rgb, 0, c, 0, _rgb.length);
		return c;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = _alpha;
		result = prime * result + Arrays.hashCode(_rgb);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColorImpl other = (ColorImpl) obj;
		if (!Arrays.equals(_rgb, other._rgb))
			return false;
		return this._alpha == other._alpha;
	}
	
	public String toString(){
		return getHtmlColor();
	}

	//ZSS-1141
	@Override
	public byte[] getARGB() {
		byte[] c = new byte[_rgb.length+1];
		c[0] = _alpha;
		System.arraycopy(_rgb, 0, c, 1, _rgb.length);
		return c;
	}
	
	//ZSS-1183
	//@since 3.9.0
	@Override
	/*package*/ SColor cloneColor(SBook book) {
		return book == null ? this : book.createColor(getHtmlColor());
	}
}
