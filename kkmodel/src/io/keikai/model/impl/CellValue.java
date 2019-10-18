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
package io.keikai.model.impl;

import java.io.Serializable;

import io.keikai.model.SCell;

/**
 * 
 * @author Dennis
 * @since 3.5.0
 */
public class CellValue implements Serializable {
	private static final long serialVersionUID = 1L;
	protected SCell.CellType cellType;
	protected Object value;
	public CellValue(String value){
		this(SCell.CellType.STRING,value);
	}
	public CellValue(Double number){
		this(SCell.CellType.NUMBER,number);
	}
	public CellValue(Boolean bool){
		this(SCell.CellType.BOOLEAN,bool);
	}
	public CellValue(){
		this(SCell.CellType.BLANK,null);
	}
	
	protected CellValue(SCell.CellType type, Object value){
		this.cellType = value==null? SCell.CellType.BLANK:type;
		this.value = value;
	}
	
	public SCell.CellType getType() {
		return cellType;
	}
	public Object getValue() {
		return value;
	}
	
	public int hashCode() {
		return cellType.ordinal()*31 + (value == null ? 0 : value.hashCode());
	}
	
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof CellValue))
			return false;
		final CellValue other = (CellValue) o;
		return other.cellType == this.cellType &&
				(this.value == other.value ||
				(this.value != null && this.value.equals(other.value))); 
	}
}
