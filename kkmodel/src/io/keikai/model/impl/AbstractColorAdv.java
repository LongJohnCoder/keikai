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

import java.io.Serializable;

import io.keikai.model.SBook;
import io.keikai.model.SColor;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public abstract class AbstractColorAdv implements SColor,Serializable{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Returns byte array in Alpha, Red, Green, Blue order
	 * @return
	 * @since 3.8.2
	 */
	public abstract byte[] getARGB();
	
	// ZSS-1183
	// @since 3.9.0
	/*package*/ abstract SColor cloneColor(SBook book);
}
