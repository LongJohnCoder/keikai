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
import java.util.Iterator;

import io.keikai.model.SBook;
import io.keikai.model.SCell;
import io.keikai.model.SRow;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public abstract class AbstractRowAdv implements SRow,LinkedModelObject,Serializable{
	private static final long serialVersionUID = 1L;
	
	public abstract AbstractCellAdv getCell(int columnIdx, boolean proxy);

	/*package*/ abstract AbstractCellAdv getOrCreateCell(int columnIdx);
	
//	/*package*/ abstract void onModelInternalEvent(ModelInternalEvent event);

	/*package*/ abstract int getStartCellIndex();
	/*package*/ abstract int getEndCellIndex();
	
	/*package*/ abstract void clearCell(int start, int end);

	/*package*/ abstract void insertCell(int start, int size);

	/*package*/ abstract void deleteCell(int start, int size);
	
	/*package*/ abstract Iterator<SCell> getCellIterator(boolean reverse);

	/*package*/ abstract void setIndex(int newidx);

	/*package*/ abstract void moveCellTo(AbstractRowAdv target, int start, int end, int offset);
	
	//ZSS-688
	/*package*/ abstract AbstractRowAdv cloneRow(AbstractSheetAdv sheet);
	
	public abstract Iterator<SCell> getCellIterator(boolean reverse, int start, int end);
	
	//ZSS-1183
	//@since 3.9.0
	/*package*/ abstract AbstractRowAdv cloneRow(AbstractSheetAdv sheet, SBook book);
}
