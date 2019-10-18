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
package io.keikai.model;

import java.util.List;
/**
 * A rich text can have multiple segments which have different fonts for 
 * each one in a cell. However, note that the Segment might return a null Font 
 * if it wants to use the current cell's font. You can use 
 * {@link RichTextHelper#getRichTextRealFont(SFont font, SCell cell)} to get
 * the real font used by the Segment.
 * 
 * @author dennis
 * @since 3.5.0
 */
public interface SRichText {
	/**
	 * @since 3.5.0
	 */
	public interface Segment {
		public String getText();	
		public SFont getFont();
	}
	
	public String getText();
	public SFont getFont();
	
	public List<Segment> getSegments();
	
	public void addSegment(String text, SFont font);
	
	public void clearSegments();
	
	public int getHeightPoints();
	
}
