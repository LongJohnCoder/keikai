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
import io.keikai.model.SCellStyle;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public abstract class AbstractCellStyleAdv implements SCellStyle,Serializable{
	private static final long serialVersionUID = 1L;
	
	/**
	 * gets the string key of this style, the key should combine all the style value in short string as possible
	 */
	abstract String getStyleKey();

	/**
	 * gets the fillpattern background-image style for this style.
	 * @since 3.7.0
	 */
	abstract public String getFillPatternHtml();
	
	//ZSS-1183
	//@since 3.9.0
	/*package*/ abstract SCellStyle cloneCellStyle(SBook book);
	
	//ZSS-1183
	//@since 3.9.0
	/*package*/ abstract SCellStyle createCellStyle(SBook book);
}
