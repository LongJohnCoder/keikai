/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by Hawk
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.range.impl.imexp;

import io.keikai.model.SColumnArray;
import io.keikai.model.SSheet;
import io.keikai.model.impl.AbstractBookAdv;
import org.zkoss.poi.hssf.usermodel.HSSFSheet;
import org.zkoss.poi.hssf.usermodel.HSSFWorkbook;
import org.zkoss.poi.ss.SpreadsheetVersion;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.ss.usermodel.Sheet;
import org.zkoss.poi.ss.usermodel.Workbook;

/**
 * 
 * @author dennis, kuro
 * @since 3.5.0
 */
public class ExcelXlsExporter extends AbstractExcelExporter {
	private static final long serialVersionUID = 4653748601821482540L;

	@Override
	protected void exportColumnArray(
			SSheet sheet, Sheet poiSheet, SColumnArray columnArr) {
		
		CellStyle poiCellStyle = toPOICellStyle(columnArr.getCellStyle());
		boolean hidden = columnArr.isHidden();
		
		//ZSS-1132
		final AbstractBookAdv book = (AbstractBookAdv) sheet.getBook();
		final int charWidth = book.getCharWidth();
		
		for(int i = columnArr.getIndex(); i <= columnArr.getLastIndex() && i <= SpreadsheetVersion.EXCEL97.getMaxColumns(); i++) {
			poiSheet.setColumnWidth(i, UnitUtil.pxToFileChar256(columnArr.getWidth(), charWidth));
			poiSheet.setColumnHidden(i, hidden);
			poiSheet.setDefaultColumnStyle(i, poiCellStyle);
		}
	}

	@Override
	protected Workbook createPoiBook() {
		return new HSSFWorkbook();
	}

	@Override
	protected void exportChart(SSheet sheet, Sheet poiSheet) {
		// not support in XLS
	}
	
	@Override
	protected void exportPicture(SSheet sheet, Sheet poiSheet) {
		// not support in XLS
	}

	@Override
	protected void exportValidation(SSheet sheet, Sheet poiSheet) {
		// not support in XLS
	}

	@Override
	protected void exportAutoFilter(SSheet sheet, Sheet poiSheet) {
		// not support in XLS
	}

	/**
	 * Export hashed password directly to poiSheet.
	 */
	@Override
	protected void exportPassword(SSheet sheet, Sheet poiSheet) {
		short hashpass = sheet.getHashedPassword();
		if (hashpass != 0) {
			((HSSFSheet)poiSheet).setPasswordHash(hashpass);
		}
	}

	@Override
	protected int exportTables(SSheet sheet, Sheet poiSheet, int tbId) {
		// not support in XLS
		return 0;
	}

	@Override
	protected void exportConditionalFormatting(SSheet sheet, Sheet poiSheet) {
		// not support in XLS
	}
}
