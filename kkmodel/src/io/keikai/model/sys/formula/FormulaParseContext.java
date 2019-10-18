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
package io.keikai.model.sys.formula;

import java.io.Serializable;
import java.util.Locale;

import io.keikai.model.SBook;
import io.keikai.model.SCell;
import io.keikai.model.SSheet;
import io.keikai.model.sys.AbstractContext;
import io.keikai.model.sys.dependency.Ref;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public class FormulaParseContext extends AbstractContext implements Serializable {
	private static final long serialVersionUID = 156103193833822863L;

	private final Ref _dependent;

	private final SBook _book;
	private final SSheet _sheet;
	private final SCell _cell;
	private final String _sheetName;

	public FormulaParseContext(SCell cell,Ref dependent) {
		this(cell,cell.getSheet().getSheetName(),dependent);
	}
	public FormulaParseContext(SCell cell, String sheetName, Ref dependent) {
		this(cell.getSheet().getBook(),cell.getSheet(),cell, sheetName, dependent);
	}
	public FormulaParseContext(SSheet sheet,Ref dependent) {
		this(sheet,sheet.getSheetName(),dependent);
	}
	public FormulaParseContext(SSheet sheet, String sheetName, Ref dependent) {
		this(sheet.getBook(),sheet,null,sheetName,dependent);
	}
	public FormulaParseContext(SBook book,Ref dependent) {
		this(book,null,null,null,dependent);
		
	}
	public FormulaParseContext(SBook book, SSheet sheet, SCell cell, String sheetName,
			Ref dependent) {
		this(book, sheet, cell, sheetName, dependent, Locale.US); //ZSS-565: internal Locale is US
	}
	//ZSS-565: Support input number of Swedish locale into Formula
	public FormulaParseContext(SBook book, SSheet sheet, SCell cell, String sheetName,
			Ref dependent, Locale locale) {
		super(locale);
		this._book = book;
		this._sheet = sheet;
		this._cell = cell;
		this._dependent = dependent;
		this._sheetName = sheetName;
	}

	public Ref getDependent() {
		return _dependent;
	}

	public SBook getBook() {
		return _book;
	}

	public SSheet getSheet() {
		return _sheet;
	}

	public SCell getCell() {
		return _cell;
	}

	public String getSheetName() {
		return _sheetName;
	}
}
