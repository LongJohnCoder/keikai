/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.model.impl.sys;

import java.io.Serializable;
import java.text.Format;

import io.keikai.model.SColor;
import io.keikai.model.SRichText;
import io.keikai.model.impl.ColorImpl;
import org.zkoss.poi.ss.format.CellFormatResult;
import io.keikai.model.sys.format.FormatResult;
/**
 * 
 * @author Hawk
 * @since 3.5.0
 */
public class FormatResultImpl implements FormatResult, Serializable {
	private static final long serialVersionUID = -289137517055850524L;
	
	private String _text;
	private SColor _textColor;//it is possible no format result color
	private SRichText _richText;
	private boolean _dateFormatted = false;
	private Format _formater;
	public FormatResultImpl(SRichText richText){
		this._richText = richText;
	}
	public FormatResultImpl(CellFormatResult result, Format formater, boolean dateFormatted){
		this._text = result.text;
		if (result.textColor != null){
			this._textColor = new ColorImpl((byte)result.textColor.getRed(),(byte)result.textColor.getGreen(),
					(byte)result.textColor.getBlue());
		}
		this._formater = formater;
		this._dateFormatted = dateFormatted;
	}
	public FormatResultImpl(String text, SColor color){
		this._text = text;
		this._textColor = color;
	}
	
	@Override
	public Format getFormater(){
		return _formater;
	}
	
	@Override
	public String getText() {
		return _richText==null?_text:_richText.getText();
	}

	@Override
	public SColor getColor() {
		return _richText==null?_textColor:_richText.getFont().getColor();
	}
	@Override
	public boolean isRichText() {
		return _richText!=null;
	}
	@Override
	public SRichText getRichText() {
		return _richText;
	}
	@Override
	public boolean isDateFormatted() {
		return _dateFormatted;
	}


}
