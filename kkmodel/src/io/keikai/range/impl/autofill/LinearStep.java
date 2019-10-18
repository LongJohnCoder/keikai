/* LinearStep.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Mar 29, 2011 2:29:38 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2011 Potix Corporation. All Rights Reserved.
*/


package io.keikai.range.impl.autofill;

import java.io.Serializable;

import io.keikai.model.SCell;

/**
 * Linear incremental Step.
 * @author henrichen
 * @since 2.1.0
 */
public class LinearStep implements Step, Serializable {
	private static final long serialVersionUID = -4640478082136298919L;
	
	private double _current;
	private final double _step;
	private final int _type;
	/*package*/ LinearStep(double initial, double initStep, double step, int type) {
		_current = initial + initStep;
		_step = step;
		_type = type;
	}
	
	@Override
	public int getDataType() {
		return _type;
	}

	@Override
	public Object next(SCell cell) {
		if (cell.getType() != SCell.CellType.NUMBER) {
			return null;
		}
		final double current = _current;
		_current += _step;
		return current;
	}
}
