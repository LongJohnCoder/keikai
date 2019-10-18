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

import io.keikai.model.SName;
import io.keikai.model.SSheet;
import io.keikai.model.sys.formula.FormulaExpression;
/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public abstract class AbstractNameAdv implements SName,LinkedModelObject,Serializable{

	private static final long serialVersionUID = 1L;

	/*package*/ abstract void setName(String newname, String applyToSheetName);

	//ZSS-747
	/**
	 * 
	 * @return
	 * @since 3.6.0
	 */
	abstract public FormulaExpression getRefersToFormulaExpression();
	//ZSS-747
	/**
	 * 
	 * @param refersExpr
	 * @since 3.6.0
	 */
	abstract public void setRefersToFormula(FormulaExpression refersExpr);
	
	//ZSS-1183
	//@since 3.9.0
	/*package*/ abstract AbstractNameAdv cloneName(SSheet sheet);
}
