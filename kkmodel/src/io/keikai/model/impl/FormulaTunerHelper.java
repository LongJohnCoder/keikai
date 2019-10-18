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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.keikai.model.CellRegion;
import io.keikai.model.SAutoFilter;
import io.keikai.model.SBook;
import io.keikai.model.SBookSeries;
import io.keikai.model.SCFValueObject;
import io.keikai.model.SCell;
import io.keikai.model.SChart;
import io.keikai.model.SConditionalFormatting;
import io.keikai.model.SConditionalFormattingRule;
import io.keikai.model.SDataValidation;
import io.keikai.model.SName;
import io.keikai.model.SSheet;
import io.keikai.model.STable;
import io.keikai.model.SheetRegion;
import io.keikai.model.chart.SChartData;
import io.keikai.model.chart.SGeneralChartData;
import io.keikai.model.chart.SSeries;
import io.keikai.model.impl.chart.AbstractGeneralChartDataAdv;
import io.keikai.model.sys.EngineFactory;
import io.keikai.model.sys.dependency.DependencyTable;
import io.keikai.model.sys.dependency.NameRef;
import io.keikai.model.sys.dependency.ObjectRef;
import io.keikai.model.sys.dependency.Ref;
import io.keikai.model.sys.dependency.ObjectRef.ObjectType;
import io.keikai.model.sys.dependency.ConditionalRef;
import io.keikai.model.sys.dependency.Ref.RefType;
import io.keikai.model.sys.formula.FormulaEngine;
import io.keikai.model.sys.formula.FormulaExpression;
import io.keikai.model.sys.formula.FormulaParseContext;
/**
 * 
 * @author Dennis
 * @since 3.5.0
 */
/*package*/ class FormulaTunerHelper implements Serializable {
	private static final long serialVersionUID = 1102626197326199285L;
	
	private final SBookSeries _bookSeries;

	public FormulaTunerHelper(SBookSeries bookSeries) {
		this._bookSeries = bookSeries;
	}

	public void move(SheetRegion sheetRegion,Set<Ref> dependents,int rowOffset, int columnOffset) {
		//because of the chart shifting is for all chart, but the input dependent is on series,
		//so we need to collect the dependent for only shift chart once
		Map<String,Ref> chartDependents  = new LinkedHashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new LinkedHashMap<String, Ref>();
		Set<Ref> cellDependents = new LinkedHashSet<Ref>(); //ZSS-649
		Map<String,Ref> nameDependents = new LinkedHashMap<String, Ref>(); //ZSS-649
		Set<Ref> filterDependents = new LinkedHashSet<Ref>(); //ZSS-555
		Map<Integer,Ref> conditionalDependents  = new LinkedHashMap<Integer, Ref>(); //ZSS-1251
		
		splitDependents(dependents, cellDependents, chartDependents, validationDependents, nameDependents, filterDependents, conditionalDependents); //ZSS-1251
		
		for (Ref dependent : cellDependents) {
			moveCellRef(sheetRegion,dependent,rowOffset,columnOffset);
		}
		for (Ref dependent : chartDependents.values()) {
			moveChartRef(sheetRegion,(ObjectRef)dependent,rowOffset,columnOffset);
		}
		for (Ref dependent : validationDependents.values()) {
			moveDataValidationRef(sheetRegion,(ObjectRef)dependent,rowOffset,columnOffset);
		}
		for (Ref dependent : nameDependents.values()) {
			moveNameRef(sheetRegion,(NameRef)dependent,rowOffset,columnOffset);
		}
		//ZSS-555
		for (Ref dependent : filterDependents) {
			moveFilterRef(sheetRegion,(ObjectRef)dependent,rowOffset,columnOffset);
		}
		//ZSS-1251
		for (Ref dependent : conditionalDependents.values()) {
			moveConditionalRef(sheetRegion,(ConditionalRef)dependent,rowOffset,columnOffset);
		}
	}
	private void moveChartRef(SheetRegion sheetRegion,ObjectRef dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		SChart chart =  sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart==null) return;
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) return;
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter;
		SGeneralChartData data = (SGeneralChartData)d;

		FormulaExpression catExpr = ((AbstractGeneralChartDataAdv)data).getCategoriesFormulaExpression();
		if(catExpr!=null){
			exprAfter = engine.movePtgs(catExpr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprAfter.hasError() && !catExpr.getFormulaString().equals(exprAfter.getFormulaString())){
				((AbstractGeneralChartDataAdv)data).setCategoriesFormula(exprAfter);
			}else{
				//zss-626, has to clear cache and notify ref update
				data.clearFormulaResultCache();
			}
		}
		
		for(int i=0;i<data.getNumOfSeries();i++){
			SSeries series = data.getSeries(i);
			FormulaExpression nameExpr = ((AbstractSeriesAdv)series).getNameFormulaExpression();
			FormulaExpression xvalExpr = ((AbstractSeriesAdv)series).getXValuesFormulaExpression();
			FormulaExpression yvalExpr = ((AbstractSeriesAdv)series).getYValuesFormulaExpression();
			FormulaExpression zvalExpr = ((AbstractSeriesAdv)series).getZValuesFormulaExpression();
			
			boolean dirty = false;
			
			if(nameExpr!=null){
				exprAfter = engine.movePtgs(nameExpr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					dirty |= !nameExpr.getFormulaString().equals(exprAfter.getFormulaString()); 
					nameExpr = exprAfter;
					
				}
			}
			if(xvalExpr!=null){
				exprAfter = engine.movePtgs(xvalExpr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					dirty |= !xvalExpr.getFormulaString().equals(exprAfter.getFormulaString());
					xvalExpr = exprAfter;
					
				}
			}
			if(yvalExpr!=null){
				exprAfter = engine.movePtgs(yvalExpr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					dirty |= !yvalExpr.getFormulaString().equals(exprAfter.getFormulaString());
					yvalExpr = exprAfter;
					
				}
			}
			if(zvalExpr!=null){
				exprAfter = engine.movePtgs(zvalExpr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					dirty |= !zvalExpr.getFormulaString().equals(exprAfter.getFormulaString());
					zvalExpr = exprAfter;
				}
			}
			if(dirty){
				((AbstractSeriesAdv)series).setXYZFormula(nameExpr, xvalExpr, yvalExpr, zvalExpr);
			}else{
				//zss-626, has to clear cache and notify ref update
				series.clearFormulaResultCache();
			}
		}
		
		ModelUpdateUtil.addRefUpdate(dependent);
		
	}
	//ZSS-555
	private void moveFilterRef(SheetRegion sheetRegion,ObjectRef dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SAutoFilter filter = sheet.getAutoFilter();
		if(filter == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update AutoFilter's region
		final CellRegion region = filter.getRegion();
		final String area = region.getReferenceString();
		FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
		FormulaExpression fexpr = engine.parse(area, context);
		FormulaExpression expr2 = engine.movePtgs(fexpr, sheetRegion, rowOffset, columnOffset, context);
		if(!expr2.hasError() && !area.equals(expr2.getFormulaString())) {
			sheet.deleteAutoFilter();
			if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
				//delete
			} else { //delete then add
				final CellRegion region2 = new CellRegion(expr2.getFormulaString());
				sheet.createAutoFilter(region2);
			}
		}

		// notify filter change
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	
	//ZSS-648
	private void moveDataValidationRef(SheetRegion sheetRegion,ObjectRef dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SDataValidation validation = sheet.getDataValidation(dependent.getObjectIdPath()[0]);
		if(validation == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Validation's formula if any
		FormulaExpression f1 = ((AbstractDataValidationAdv)validation).getFormulaExpression1();
		FormulaExpression f2 = ((AbstractDataValidationAdv)validation).getFormulaExpression2();

		boolean changed = false;
		if (f1 != null) {
			FormulaExpression exprf1 = engine.movePtgs(f1, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
				f1 = exprf1;
				changed = true;
			}
		}
		if (f2 != null) {
			FormulaExpression exprf2 = engine.movePtgs(f2, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
				f2 = exprf2;
				changed = true;
			}
		}
		if (changed) {
			((AbstractDataValidationAdv)validation).setFormulas(f1, f2);
		} else {
			validation.clearFormulaResultCache();
		}
		
		// update Validation's region (sqref)
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(validation.getRegions()); //ZSS-1280 avoid Comodification...
		for (CellRegion region : regions) { //ZSS-1280
			String sqref = region.getReferenceString();
			FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.movePtgs(fexpr, sheetRegion, rowOffset, columnOffset, context); //null ref, no trace dependence here
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					((AbstractDataValidationAdv)validation).removeRegion(region);
					if (validation.getRegions() == null) {
						sheet.deleteDataValidation(validation);
					}
				} else {
					((AbstractDataValidationAdv)validation).removeRegion(region); //ZSS-1280
					region = new CellRegion(expr2.getFormulaString());
					((AbstractDataValidationAdv)validation).addRegion(region);
				}
			}
		}

		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-1251
	private void moveConditionalRef(SheetRegion sheetRegion,ConditionalRef dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		final AbstractSheetAdv sheet = (AbstractSheetAdv)book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		ConditionalFormattingImpl cfmt = (ConditionalFormattingImpl)
				((SheetImpl)sheet).getDataValidation(dependent.getConditionalId());
		if(cfmt == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Conditional's formula if any
		for (SConditionalFormattingRule rule0 : cfmt.getRules()) {
			final ConditionalFormattingRuleImpl rule = (ConditionalFormattingRuleImpl) rule0;
			FormulaExpression f1 = rule.getFormulaExpression1();
			FormulaExpression f2 = rule.getFormulaExpression2();
			FormulaExpression f3 = rule.getFormulaExpression3();
	
			boolean changed = false;
			if (f1 != null) {
				FormulaExpression exprf1 = engine.movePtgs(f1, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
					f1 = exprf1;
					changed = true;
				}
			}
			if (f2 != null) {
				FormulaExpression exprf2 = engine.movePtgs(f2, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
					f2 = exprf2;
					changed = true;
				}
			}
			if (f3 != null) {
				FormulaExpression exprf3 = engine.movePtgs(f3, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf3.hasError() && !f3.getFormulaString().equals(exprf3.getFormulaString())) {
					f3 = exprf3;
					changed = true;
				}
			}
			if (changed) {
				rule.setFormulas(f1, f2, f3);
			} else {
				cfmt.clearFormulaResultCache();
			}
			
			changed = false;
			if (rule.getColorScale() != null) {
				for (SCFValueObject cvo0 : rule.getColorScale().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.movePtgs(f, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getDataBar() != null) {
				for (SCFValueObject cvo0 : rule.getDataBar().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.movePtgs(f, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getIconSet() != null) {
				for (SCFValueObject cvo0 : rule.getIconSet().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.movePtgs(f, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				rule.clearFormulaResultCache();
			}
		}
		
		// update Validation's region (sqref)
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(cfmt.getRegions()); //ZSS-1280 avoid Comodification...
		for (CellRegion region : regions) { // ZSS-1280
			String sqref = region.getReferenceString();
			FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.movePtgs(fexpr, sheetRegion, rowOffset, columnOffset, context); //null ref, no trace dependence here
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					cfmt.removeRegion(region);
					if (cfmt.getRegions() == null) {
						sheet.deleteConditionalFormatting(cfmt);
					}
				} else {
					cfmt.removeRegion(region); //ZSS-1280
					region = new CellRegion(expr2.getFormulaString());
					cfmt.addRegion(region);
				}
			}
		}

		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	private void moveCellRef(SheetRegion sheetRegion,Ref dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		SCell cell = sheet.getCell(dependent.getRow(),
				dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.movePtgs(expr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(cell, null));//null ref, no trace dependence here
		
		if(!expr.getFormulaString().equals(exprAfter.getFormulaString())){
			cell.setValue(exprAfter);
			//don't need to notify cell change, cell will do
		}else{
			//zss-626, has to clear cache and notify ref update
			cell.clearFormulaResultCache();
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}
	//ZSS-649
	private void moveNameRef(SheetRegion sheetRegion,NameRef dependent,int rowOffset, int columnOffset) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		// ZSS-1337 must consider the NameRef's sheet scope
		final SName name = book.getNameByName(dependent.getNameName(), dependent.getSheetName());
		if(name==null) return;
		SSheet sheet = book.getSheetByName(name.getRefersToSheetName());
		if(sheet==null) return;
		FormulaExpression expr = ((AbstractNameAdv)name).getRefersToFormulaExpression();
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.movePtgs(expr, sheetRegion, rowOffset, columnOffset, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
		
		if(!expr.getFormulaString().equals(exprAfter.getFormulaString())){
			((AbstractNameAdv)name).setRefersToFormula(exprAfter);
			//don't need to notify name change, name will do
		}else{
			//zss-687, clear dependents's cache of NameRef
			clearFormulaCache(dependent);
			
			//zss-626, has to clear cache and notify ref update
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}

	private FormulaEngine engine;
	private FormulaEngine getFormulaEngine() {
		if(engine==null){
			engine = EngineFactory.getInstance().createFormulaEngine();
		}
		return engine;
	}
	
	public void extend(SheetRegion sheetRegion,Set<Ref> dependents, boolean horizontal) {
		//because of the chart shifting is for all chart, but the input dependent is on series,
		//so we need to collect the dependent for only shift chart once
		Map<String,Ref> chartDependents  = new LinkedHashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new LinkedHashMap<String, Ref>();
		Set<Ref> cellDependents = new LinkedHashSet<Ref>(); //ZSS-649
		Map<String,Ref> nameDependents = new LinkedHashMap<String, Ref>(); //ZSS-649
		Set<Ref> filterDependents = new LinkedHashSet<Ref>(); //ZSS-555
		Map<Integer,Ref> conditionalDependents = new LinkedHashMap<Integer, Ref>(); //ZSS-1251
		
		splitDependents(dependents, cellDependents, chartDependents, validationDependents, nameDependents, filterDependents, conditionalDependents);//ZSS-1251
		
		for (Ref dependent : cellDependents) {
			extendCellRef(sheetRegion,dependent,horizontal);
		}
		for (Ref dependent : chartDependents.values()) {
			extendChartRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}
		for (Ref dependent : validationDependents.values()) {
			extendDataValidationRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}		
		for (Ref dependent : nameDependents.values()) {
			extendNameRef(sheetRegion,(NameRef)dependent,horizontal);
		}
		//ZSS-555
		for (Ref dependent : filterDependents) {
			extendFilterRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}
		//ZSS-1251
		for (Ref dependent : conditionalDependents.values()) {
			extendConditionalRef(sheetRegion, (ConditionalRef)dependent,horizontal);
		}
	}

	private void extendChartRef(SheetRegion sheetRegion, ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SChart chart = sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart == null) {
			return;
		}
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) {
			return;
		}
		SGeneralChartData data = (SGeneralChartData)d;
		FormulaEngine engine = getFormulaEngine();
		
		// extend series formula
		for(int i = 0; i < data.getNumOfSeries(); ++i) {
			SSeries series = data.getSeries(i);
			if(series != null) {
				boolean changed = false;
				series.clearFormulaResultCache();
				FormulaExpression nf = ((AbstractSeriesAdv)series).getNameFormulaExpression();
				FormulaExpression xf = ((AbstractSeriesAdv)series).getXValuesFormulaExpression();
				FormulaExpression yf = ((AbstractSeriesAdv)series).getYValuesFormulaExpression();
				FormulaExpression zf = ((AbstractSeriesAdv)series).getZValuesFormulaExpression();
				if(nf != null) {
					FormulaExpression expr2 = engine.extendPtgs(nf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !nf.getFormulaString().equals(expr2.getFormulaString())) {
						nf = expr2;
						changed = true;
					}
				}
				if(xf != null) {
					FormulaExpression expr2 = engine.extendPtgs(xf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !xf.getFormulaString().equals(expr2.getFormulaString())) {
						xf = expr2;
						changed = true;
					}
				}
				if(yf != null) {
					FormulaExpression expr2 = engine.extendPtgs(yf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !yf.getFormulaString().equals(expr2.getFormulaString())) {
						yf = expr2;
						changed = true;
					}
				}
				if(zf != null) {
					FormulaExpression expr2 = engine.extendPtgs(zf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !zf.getFormulaString().equals(expr2.getFormulaString())) {
						zf = expr2;
						changed = true;
					}
				}
				if(changed) {
					((AbstractSeriesAdv)series).setXYZFormula(nf, xf, yf, zf);
				}else{
					//zss-626, has to clear cache and notify ref update
					series.clearFormulaResultCache();
				}
			}
		}
		
		// extend categories formula
		FormulaExpression expr = ((AbstractGeneralChartDataAdv)data).getCategoriesFormulaExpression();
		if(expr != null) {
			FormulaExpression exprAfter = engine.extendPtgs(expr, sheetRegion,horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprAfter.hasError() && !expr.getFormulaString().equals(exprAfter.getFormulaString())) {
				((AbstractGeneralChartDataAdv)data).setCategoriesFormula(exprAfter);
			}else{
				//zss-626, has to clear cache and notify ref update
				data.clearFormulaResultCache();
			}
		}
		
		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	//ZSS-648
	private void extendDataValidationRef(SheetRegion sheetRegion,ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SDataValidation validation = sheet.getDataValidation(dependent.getObjectIdPath()[0]);
		if(validation == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Validation's formula if any
		FormulaExpression f1 = ((AbstractDataValidationAdv)validation).getFormulaExpression1();
		FormulaExpression f2 = ((AbstractDataValidationAdv)validation).getFormulaExpression2();
		boolean changed = false;
		if (f1 != null) {
			FormulaExpression exprf1 = engine.extendPtgs(f1, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
				f1 = exprf1;
				changed = true;
			}
		}
		if (f2 != null) {
			FormulaExpression exprf2 = engine.extendPtgs(f2, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
				f2 = exprf2;
				changed = true;
			}
		}
		if (changed) {
			((AbstractDataValidationAdv)validation).setFormulas(f1, f2);
		} else {
			validation.clearFormulaResultCache();
		}
		
		// update Validation's region (sqref)
		FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(validation.getRegions()); //ZSS-1047 avoid Comodification...
		for (CellRegion region : regions) {
			String sqref = region.getReferenceString();
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.extendPtgs(fexpr, sheetRegion, horizontal, context);
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					((AbstractDataValidationAdv)validation).removeRegion(region);
					if (validation.getRegions() == null) {
						sheet.deleteDataValidation(validation);
					}
				} else {
					((AbstractDataValidationAdv)validation).removeRegion(region); //ZSS-1047
					final CellRegion region0 = new CellRegion(expr2.getFormulaString());
					((AbstractDataValidationAdv)validation).addRegion(region0);
				}
			}
		}

		// notify validation change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-1251
	private void extendConditionalRef(SheetRegion sheetRegion,ConditionalRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		final AbstractSheetAdv sheet = (AbstractSheetAdv) book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SConditionalFormatting cfmt =
				((AbstractSheetAdv)sheet).getConditionalFormatting(dependent.getConditionalId());
		if(cfmt == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update ConditionalFormatting's formula if any
		for (SConditionalFormattingRule rule0 : cfmt.getRules()) {
			ConditionalFormattingRuleImpl rule = (ConditionalFormattingRuleImpl) rule0;
			FormulaExpression f1 = ((ConditionalFormattingRuleImpl)rule).getFormulaExpression1();
			FormulaExpression f2 = ((ConditionalFormattingRuleImpl)rule).getFormulaExpression2();
			FormulaExpression f3 = ((ConditionalFormattingRuleImpl)rule).getFormulaExpression3();
			
			boolean changed = false;
			if (f1 != null) {
				FormulaExpression exprf1 = engine.extendPtgs(f1, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
					f1 = exprf1;
					changed = true;
				}
			}
			if (f2 != null) {
				FormulaExpression exprf2 = engine.extendPtgs(f2, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
					f2 = exprf2;
					changed = true;
				}
			}
			if (f3 != null) {
				FormulaExpression exprf3 = engine.extendPtgs(f3, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf3.hasError() && !f3.getFormulaString().equals(exprf3.getFormulaString())) {
					f3 = exprf3;
					changed = true;
				}
			}
			if (changed) {
				rule.setFormulas(f1, f2, f3);
			} else {
				rule.clearFormulaResultCache();
			}
			
			changed = false;
			if (rule.getColorScale() != null) {
				for (SCFValueObject cvo0 : rule.getColorScale().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.extendPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getDataBar() != null) {
				for (SCFValueObject cvo0 : rule.getDataBar().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.extendPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getIconSet() != null) {
				for (SCFValueObject cvo0 : rule.getIconSet().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.extendPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				rule.clearFormulaResultCache();
			}
		}
		
		// update ConditionalFormatting's region (sqref)
		FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(cfmt.getRegions()); //ZSS-1047 avoid Comodification...
		for (CellRegion region : regions) {
			String sqref = region.getReferenceString();
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.extendPtgs(fexpr, sheetRegion, horizontal, context);
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					cfmt.removeRegion(region);
					if (cfmt.getRegions() == null) {
						sheet.deleteConditionalFormatting(cfmt);
					}
				} else {
					cfmt.removeRegion(region); //ZSS-1047
					final CellRegion region0 = new CellRegion(expr2.getFormulaString());
					cfmt.addRegion(region0);
				}
			}
		}

		// notify conditional change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	private void extendFilterRef(SheetRegion sheetRegion,ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SAutoFilter filter = sheet.getAutoFilter();
		if(filter == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update AutoFilter's region
		CellRegion region = filter.getRegion();
		String area = region.getReferenceString();
		FormulaParseContext context = new FormulaParseContext(sheet, null);//null ref, no trace dependence here
		FormulaExpression fexpr = engine.parse(area, context);
		FormulaExpression expr2 = engine.extendPtgs(fexpr, sheetRegion, horizontal, context);//null ref, no trace dependence here
		if(!expr2.hasError() && !area.equals(expr2.getFormulaString())) {
			final Collection<SAutoFilter.NFilterColumn> fcols = horizontal ? filter.getFilterColumns() : null; //ZSS-1230
			sheet.deleteAutoFilter();
			if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
				// delete
			} else { // delete than add
				final CellRegion region2 = new CellRegion(expr2.getFormulaString());
				final AutoFilterImpl nfilter = (AutoFilterImpl) sheet.createAutoFilter(region2); //ZSS-1230
				//ZSS-1230
				if (fcols != null) {
					// start == 0 means whole filter push to right; no change on filterColumn indexes
					// offset is sheetRegion.getColumnCount() if filterColumn index >= start
					int start = sheetRegion.getColumn() - region.getColumn();
					int offset = start > 0 ? sheetRegion.getColumnCount() : 0;
					for (SAutoFilter.NFilterColumn fcol : fcols) {
						final int index0 = 
							fcol.getIndex() + (fcol.getIndex() >= start ? offset : 0);
						((AbstractAutoFilterAdv.FilterColumnImpl)fcol).setIndex(index0);
						nfilter.putFilterColumn(fcol.getIndex(), fcol);
					}
				}
			}
		}

		// notify filter change
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	
	private void extendCellRef(SheetRegion sheetRegion,Ref dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		SCell cell = sheet.getCell(dependent.getRow(),
				dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		FormulaExpression fexpr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false); //ZSS-747
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.extendPtgs(fexpr, sheetRegion,horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here //ZSS-747
		if(!fexpr.getFormulaString().equals(exprAfter.getFormulaString())){			
			cell.setValue(exprAfter);
			//don't need to notify cell change, cell will do
		}else{
			//zss-626, has to clear cache and notify ref update
			cell.clearFormulaResultCache();
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}

	//ZSS-649
	private void extendNameRef(SheetRegion sheetRegion, NameRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		// ZSS-1337 must consider the NameRef's sheet scope
		final SName name = book.getNameByName(dependent.getNameName(), dependent.getSheetName());
		if (name == null) return;
		SSheet sheet = book.getSheetByName(name.getRefersToSheetName());
		if(sheet==null) return;
		FormulaExpression expr = ((AbstractNameAdv)name).getRefersToFormulaExpression();
		FormulaEngine engine = getFormulaEngine();
		
		FormulaExpression exprAfter = engine.extendPtgs(expr, sheetRegion,horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
		if(!expr.getFormulaString().equals(exprAfter.getFormulaString())){
			((AbstractNameAdv)name).setRefersToFormula(exprAfter);
			//don't need to notify cell change, cell will do
		}else{
			//zss-687, clear dependents's cache of NameRef
			clearFormulaCache(dependent);
			
			//zss-626, has to clear cache and notify ref update
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}
	
	public void shrink(SheetRegion sheetRegion, Set<Ref> dependents, boolean horizontal) {
		//because of the chart shifting is for all chart, but the input dependent is on series,
		//so we need to collect the dependent for only shift chart once
		Map<String,Ref> chartDependents  = new LinkedHashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new LinkedHashMap<String, Ref>();
		Set<Ref> cellDependents = new LinkedHashSet<Ref>(); //ZSS-649
		Map<String,Ref> nameDependents = new LinkedHashMap<String, Ref>(); //ZSS-649
		Set<Ref> filterDependents = new LinkedHashSet<Ref>(); //ZSS-555
		Map<Integer,Ref> conditionalDependents  = new LinkedHashMap<Integer, Ref>(); //ZSS-1251
		
		splitDependents(dependents, cellDependents, chartDependents, validationDependents, nameDependents, filterDependents, conditionalDependents);//ZSS-1251
		
		for (Ref dependent : cellDependents) {
			shrinkCellRef(sheetRegion,dependent,horizontal);
		}
		for (Ref dependent : chartDependents.values()) {
			shrinkChartRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}
		for (Ref dependent : validationDependents.values()) {
			shrinkDataValidationRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}
		//ZSS-649
		for (Ref dependent : nameDependents.values()) {
			shrinkNameRef(sheetRegion, (NameRef) dependent, horizontal);
		}
		//ZSS-555
		for (Ref dependent : filterDependents) {
			shrinkFilterRef(sheetRegion,(ObjectRef)dependent,horizontal);
		}
		//ZSS-1251
		for (Ref dependent : conditionalDependents.values()) {
			shrinkConditionalRef(sheetRegion,(ConditionalRef)dependent,horizontal);
		}
	}
	private void shrinkChartRef(SheetRegion sheetRegion,ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SChart chart = sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart == null) {
			return;
		}
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) {
			return;
		}
		SGeneralChartData data = (SGeneralChartData)d;
		FormulaEngine engine = getFormulaEngine();
		
		// shrink series formula
		for(int i = 0; i < data.getNumOfSeries(); ++i) {
			SSeries series = data.getSeries(i);
			if(series != null) {
				boolean changed = false;
				series.clearFormulaResultCache();
				FormulaExpression nf = ((AbstractSeriesAdv)series).getNameFormulaExpression();
				FormulaExpression xf = ((AbstractSeriesAdv)series).getXValuesFormulaExpression();
				FormulaExpression yf = ((AbstractSeriesAdv)series).getYValuesFormulaExpression();
				FormulaExpression zf = ((AbstractSeriesAdv)series).getZValuesFormulaExpression();
				if(nf != null) {
					FormulaExpression expr2 = engine.shrinkPtgs(nf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !nf.getFormulaString().equals(expr2.getFormulaString())) {
						nf = expr2;
						changed = true;
					}
				}
				if(xf != null) {
					FormulaExpression expr2 = engine.shrinkPtgs(xf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !xf.getFormulaString().equals(expr2.getFormulaString())) {
						xf = expr2;
						changed = true;
					}
				}
				if(yf != null) {
					FormulaExpression expr2 = engine.shrinkPtgs(yf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !yf.getFormulaString().equals(expr2.getFormulaString())) {
						yf = expr2;
						changed = true;
					}
				}
				if(zf != null) {
					FormulaExpression expr2 = engine.shrinkPtgs(zf, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
					if(!expr2.hasError() && !zf.getFormulaString().equals(expr2.getFormulaString())) {
						zf = expr2;
						changed = true;
					}
				}
				if(changed) {
					((AbstractSeriesAdv)series).setXYZFormula(nf, xf, yf, zf);
				}else{
					//zss-626, has to clear cache and notify ref update
					series.clearFormulaResultCache();
				}
			}
		}
		
		// shrink categories formula
		FormulaExpression expr = ((AbstractGeneralChartDataAdv)data).getCategoriesFormulaExpression();
		if(expr != null) {
			FormulaExpression exprAfter = engine.shrinkPtgs(expr, sheetRegion,horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprAfter.hasError() && !expr.getFormulaString().equals(exprAfter.getFormulaString())) {
				((AbstractGeneralChartDataAdv)data).setCategoriesFormula(exprAfter);
			}else{
				//zss-626, has to clear cache and notify ref update
				data.clearFormulaResultCache();
			}
		}
		
		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	//ZSS-648
	private void shrinkDataValidationRef(SheetRegion sheetRegion,ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SDataValidation validation = sheet.getDataValidation(dependent.getObjectIdPath()[0]);
		if(validation == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Validation's formula if any
		FormulaExpression f1 = ((AbstractDataValidationAdv)validation).getFormulaExpression1();
		FormulaExpression f2 = ((AbstractDataValidationAdv)validation).getFormulaExpression2();
		boolean changed = false;
		if (f1 != null) {
			FormulaExpression exprf1 = engine.shrinkPtgs(f1, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
				f1 = exprf1;
				changed = true;
			}
		}
		if (f2 != null) {
			FormulaExpression exprf2 = engine.shrinkPtgs(f2, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
				f2 = exprf2;
				changed = true;
			}
		}
		if (changed) {
			((AbstractDataValidationAdv)validation).setFormulas(f1, f2);
		} else {
			validation.clearFormulaResultCache();
		}
		
		// update Validation's region (sqref)
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(validation.getRegions()); //ZSS-1280 avoid Comodification...
		for (CellRegion region : regions) { //ZSS-1280
			String sqref = region.getReferenceString();
			FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.shrinkPtgs(fexpr, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					((AbstractDataValidationAdv)validation).removeRegion(region);
					if (validation.getRegions() == null) {
						sheet.deleteDataValidation(validation);
					}
				} else {
					((AbstractDataValidationAdv)validation).removeRegion(region); //ZSS-1280
					region = new CellRegion(expr2.getFormulaString());
					((AbstractDataValidationAdv)validation).addRegion(region);
				}
			}
		}

		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-1251
	private void shrinkConditionalRef(SheetRegion sheetRegion,ConditionalRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		final AbstractSheetAdv sheet = (AbstractSheetAdv) book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		ConditionalFormattingImpl cfmt = (ConditionalFormattingImpl) 
				((AbstractSheetAdv)sheet).getConditionalFormatting(dependent.getConditionalId());
		if(cfmt == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Conditional's formula if any
		for (SConditionalFormattingRule rule0 : cfmt.getRules()) {
			final ConditionalFormattingRuleImpl rule = (ConditionalFormattingRuleImpl) rule0; 
		
			FormulaExpression f1 = rule.getFormulaExpression1();
			FormulaExpression f2 = rule.getFormulaExpression2();
			FormulaExpression f3 = rule.getFormulaExpression3();
			boolean changed = false;
			if (f1 != null) {
				FormulaExpression exprf1 = engine.shrinkPtgs(f1, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
					f1 = exprf1;
					changed = true;
				}
			}
			if (f2 != null) {
				FormulaExpression exprf2 = engine.shrinkPtgs(f2, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
					f2 = exprf2;
					changed = true;
				}
			}
			if (f3 != null) {
				FormulaExpression exprf3 = engine.shrinkPtgs(f3, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
				if(!exprf3.hasError() && !f3.getFormulaString().equals(exprf3.getFormulaString())) {
					f3 = exprf3;
					changed = true;
				}
			}
			if (changed) {
				rule.setFormulas(f1, f2, f3);
			} else {
				rule.clearFormulaResultCache();
			}
			
			changed = false;
			if (rule.getColorScale() != null) {
				for (SCFValueObject cvo0 : rule.getColorScale().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.shrinkPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getDataBar() != null) {
				for (SCFValueObject cvo0 : rule.getDataBar().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.shrinkPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getIconSet() != null) {
				for (SCFValueObject cvo0 : rule.getIconSet().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.shrinkPtgs(f, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				rule.clearFormulaResultCache();
			}
		}
		
		// update Conditional's region (sqref)
		final Collection<CellRegion> regions = new ArrayList<CellRegion>(cfmt.getRegions()); //ZSS-1280 avoid Comodification...		
		for (CellRegion region : regions) { // ZSS-1280
			String sqref = region.getReferenceString();
			FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
			FormulaExpression fexpr = engine.parse(sqref, context);
			FormulaExpression expr2 = engine.shrinkPtgs(fexpr, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
			if(!expr2.hasError() && !sqref.equals(expr2.getFormulaString())) {
				if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
					cfmt.removeRegion(region);
					if (cfmt.getRegions() == null) {
						sheet.deleteConditionalFormatting(cfmt);
					}
				} else {
					cfmt.removeRegion(region); // ZSS-1280
					region = new CellRegion(expr2.getFormulaString());
					cfmt.addRegion(region);
				}
			}
		}

		// notify conditional change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-555
	private void shrinkFilterRef(SheetRegion sheetRegion,ObjectRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SAutoFilter filter = sheet.getAutoFilter();
		if(filter == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update AutoFilter's region
		CellRegion region = filter.getRegion();
		String area = region.getReferenceString();
		FormulaParseContext context = new FormulaParseContext(sheet, null); //null ref, no trace dependence here
		FormulaExpression fexpr = engine.parse(area, context);
		FormulaExpression expr2 = engine.shrinkPtgs(fexpr, sheetRegion, horizontal, context);//null ref, no trace dependence here
		if(!expr2.hasError() && !area.equals(expr2.getFormulaString())) {
			final Collection<SAutoFilter.NFilterColumn> fcols = horizontal ? filter.getFilterColumns() : null; //ZSS-1230
			sheet.deleteAutoFilter();
			if ("#REF!".equals(expr2.getFormulaString())) { // should delete the region
				//delete
			} else { // delete then add
				final CellRegion region2 = new CellRegion(expr2.getFormulaString());
				final AutoFilterImpl nfilter = (AutoFilterImpl) sheet.createAutoFilter(region2); //ZSS-1230
				//ZSS-1230
				if (fcols != null) {
					// start < 0 means whole filter push to right; no change on filterColumn indexes
					// offset is -(start + 1) if filterColumn index > start
					int start = sheetRegion.getColumn() - region.getColumn();
					int end = sheetRegion.getLastColumn() - region.getColumn();
					int offset = end >= 0 ? Math.min(sheetRegion.getColumnCount(), end + 1) : 0;
					for (SAutoFilter.NFilterColumn fcol : fcols) {
						final int fcolj = fcol.getIndex();
						if (start <= fcolj && fcolj <= end) { // deleted column
							continue;
						}
						final int index0 = fcol.getIndex() - (fcolj < start ? 0 : offset);
						((AbstractAutoFilterAdv.FilterColumnImpl)fcol).setIndex(index0);
						nfilter.putFilterColumn(fcol.getIndex(), fcol);
					}
				}
			}
		}

		// notify filter change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	private void shrinkCellRef(SheetRegion sheetRegion,Ref dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		SCell cell = sheet.getCell(dependent.getRow(),
				dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		FormulaExpression fexpr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false); //ZSS-747
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.shrinkPtgs(fexpr, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
		if(!fexpr.getFormulaString().equals(exprAfter.getFormulaString())){
			cell.setValue(exprAfter);
			//don't need to notify cell change, cell will do
		}else{
			//zss-626, has to clear cache and notify ref update
			cell.clearFormulaResultCache();
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}

	//ZSS-649
	private void shrinkNameRef(SheetRegion sheetRegion, NameRef dependent, boolean horizontal) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		// ZSS-1337must consider the NameRef's sheet scope
		final SName name = book.getNameByName(dependent.getNameName(), dependent.getSheetName());
		if(name==null) return;
		SSheet sheet = book.getSheetByName(name.getRefersToSheetName());
		if(sheet==null) return;
		FormulaExpression expr = ((AbstractNameAdv)name).getRefersToFormulaExpression();
		FormulaEngine engine = getFormulaEngine();
		
		FormulaExpression exprAfter = engine.shrinkPtgs(expr, sheetRegion, horizontal, new FormulaParseContext(sheet, null));//null ref, no trace dependence here
		if(!expr.getFormulaString().equals(exprAfter.getFormulaString())){
			((AbstractNameAdv)name).setRefersToFormula(exprAfter);
			//don't need to notify name change, setRefersToFormula will do
		}else{
			//zss-687, clear dependents's cache of NameRef
			clearFormulaCache(dependent);
			
			ModelUpdateUtil.addRefUpdate(dependent);
		}
	}
	
	public void renameSheet(SBook book, String oldName, String newName,
			Set<Ref> dependents) {
		//because of the chart shifting is for all chart, but the input dependent is on series,
		//so we need to collect the dependent for only shift chart once
		Map<String,Ref> chartDependents  = new LinkedHashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new LinkedHashMap<String, Ref>();
		Set<Ref> cellDependents = new LinkedHashSet<Ref>(); //ZSS-649
		Map<String,Ref> nameDependents = new LinkedHashMap<String, Ref>(); //ZSS-649
		Set<Ref> filterDependents = new LinkedHashSet<Ref>(); //ZSS-555
		Map<Integer,Ref> conditionalDependents  = new LinkedHashMap<Integer, Ref>(); //ZSS-1251
		
		splitDependents(dependents, cellDependents, chartDependents, validationDependents, nameDependents, filterDependents, conditionalDependents);//ZSS-1251
		
		for (Ref dependent : cellDependents) {
			renameSheetCellRef(book,oldName,newName,dependent);
		}
		for (Ref dependent : chartDependents.values()) {
			renameSheetChartRef(book,oldName,newName,(ObjectRef)dependent);
		}
		for (Ref dependent : validationDependents.values()) {
			renameSheetDataValidationRef(book,oldName,newName,(ObjectRef)dependent);
		}	
		//ZSS-649
		for (Ref dependent : nameDependents.values()) {
			renameSheetNameRef(book,oldName,newName,(NameRef)dependent);
		}
		//ZSS-555
		for (Ref dependent : filterDependents) {
			renameSheetFilterRef(book,oldName,newName,(ObjectRef)dependent);
		}
		//ZSS-1251
		for (Ref dependent : conditionalDependents.values()) {
			renameSheetConditionalRef(book,oldName,newName,(ConditionalRef)dependent);
		}	
	}	
	
	private void renameSheetChartRef(SBook bookOfSheet, String oldName, String newName,ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		String sheetName = null;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(dependent.getSheetName())){
				sheet = book.getSheetByName(newName);
				sheetName = oldName;
			}
		} else {
			sheetName = sheet.getSheetName();
		}
		if(sheet==null) return;
		SChart chart =  sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart==null) return;
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) return;
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter;
		SGeneralChartData data = (SGeneralChartData)d;
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression catExpr = ((AbstractGeneralChartDataAdv)data).getCategoriesFormulaExpression();
		if(catExpr!=null){
			exprAfter = engine.renameSheetPtgs(catExpr, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
			if(!exprAfter.hasError()){
				((AbstractGeneralChartDataAdv)data).setCategoriesFormula(exprAfter);
			}
		}
		
		for(int i=0;i<data.getNumOfSeries();i++){
			SSeries series = data.getSeries(i);
			FormulaExpression nameExpr = ((AbstractSeriesAdv)series).getNameFormulaExpression();
			FormulaExpression xvalExpr = ((AbstractSeriesAdv)series).getXValuesFormulaExpression();
			FormulaExpression yvalExpr = ((AbstractSeriesAdv)series).getYValuesFormulaExpression();
			FormulaExpression zvalExpr = ((AbstractSeriesAdv)series).getZValuesFormulaExpression();
			if(nameExpr!=null){
				exprAfter = engine.renameSheetPtgs(nameExpr, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					nameExpr = exprAfter;
				}
			}
			if(xvalExpr!=null){
				exprAfter = engine.renameSheetPtgs(xvalExpr, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					xvalExpr = exprAfter;
				}
			}
			if(yvalExpr!=null){
				exprAfter = engine.renameSheetPtgs(yvalExpr, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					yvalExpr = exprAfter;
				}
			}
			if(zvalExpr!=null){
				exprAfter = engine.renameSheetPtgs(zvalExpr, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					zvalExpr = exprAfter;
				}
			}
			((AbstractSeriesAdv)series).setXYZFormula(nameExpr, xvalExpr, yvalExpr, zvalExpr);
		}
		
		ModelUpdateUtil.addRefUpdate(dependent);
		
	}	
	
	private void renameSheetDataValidationRef(SBook bookOfSheet, String oldName, String newName,ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		String sheetName = null;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(dependent.getSheetName())){
				sheet = book.getSheetByName(newName);
				sheetName = oldName;
			}
		} else {
			sheetName = sheet.getSheetName();
		}
		if(sheet == null) {
			return;
		}
		SDataValidation validation = sheet.getDataValidation(dependent.getObjectIdPath()[0]);
		if(validation == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Validation's formula if any
		FormulaExpression f1 = ((AbstractDataValidationAdv)validation).getFormulaExpression1();
		FormulaExpression f2 = ((AbstractDataValidationAdv)validation).getFormulaExpression2();
		boolean changed = false;
		if (f1 != null) {
			FormulaExpression exprf1 = engine.renameSheetPtgs(f1, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
			if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
				f1 = exprf1;
				changed = true;
			}
		}
		if (f2 != null) {
			FormulaExpression exprf2 = engine.renameSheetPtgs(f2, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
			if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
				f2 = exprf2;
				changed = true;
			}
		}
		if (changed) {
			((AbstractDataValidationAdv)validation).setFormulas(f1, f2);
		} else {
			validation.clearFormulaResultCache();
		}
		
		// update Validation's region (sqref)
		((AbstractDataValidationAdv)validation).renameSheet(oldName, newName);

		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-1251
	private void renameSheetConditionalRef(SBook bookOfSheet, String oldName, String newName,ConditionalRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		String sheetName = null;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(dependent.getSheetName())){
				sheet = book.getSheetByName(newName);
				sheetName = oldName;
			}
		} else {
			sheetName = sheet.getSheetName();
		}
		if(sheet == null) {
			return;
		}
		final ConditionalFormattingImpl cfmt = (ConditionalFormattingImpl) 
				((SheetImpl)sheet).getConditionalFormatting(dependent.getConditionalId());
		if(cfmt == null) {
			return;
		}
		FormulaEngine engine = getFormulaEngine();
		
		// update Condtional's formula if any
		for (SConditionalFormattingRule rule0 : cfmt.getRules()) {
			final ConditionalFormattingRuleImpl rule = (ConditionalFormattingRuleImpl) rule0;
			FormulaExpression f1 = rule.getFormulaExpression1();
			FormulaExpression f2 = rule.getFormulaExpression2();
			FormulaExpression f3 = rule.getFormulaExpression3();
			boolean changed = false;
			if (f1 != null) {
				FormulaExpression exprf1 = engine.renameSheetPtgs(f1, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprf1.hasError() && !f1.getFormulaString().equals(exprf1.getFormulaString())) {
					f1 = exprf1;
					changed = true;
				}
			}
			if (f2 != null) {
				FormulaExpression exprf2 = engine.renameSheetPtgs(f2, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprf2.hasError() && !f2.getFormulaString().equals(exprf2.getFormulaString())) {
					f2 = exprf2;
					changed = true;
				}
			}
			if (f3 != null) {
				FormulaExpression exprf3 = engine.renameSheetPtgs(f3, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprf3.hasError() && !f3.getFormulaString().equals(exprf3.getFormulaString())) {
					f3 = exprf3;
					changed = true;
				}
			}
			if (changed) {
				rule.setFormulas(f1, f2, f3);
			} else {
				rule.clearFormulaResultCache();
			}
			
			changed = false;
			if (rule.getColorScale() != null) {
				for (SCFValueObject cvo0 : rule.getColorScale().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.renameSheetPtgs(f, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getDataBar() != null) {
				for (SCFValueObject cvo0 : rule.getDataBar().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.renameSheetPtgs(f, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (rule.getIconSet() != null) {
				for (SCFValueObject cvo0 : rule.getIconSet().getCFValueObjects()) {
					final CFValueObjectImpl cvo = (CFValueObjectImpl) cvo0;
					final FormulaExpression f = cvo.getFormulaExpression(); 
					if (f != null) {
						FormulaExpression exprf = engine.renameSheetPtgs(f, bookOfSheet, oldName, newName,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
						if(!exprf.hasError() && !f.getFormulaString().equals(exprf.getFormulaString())) {
							cvo.setFormulaExpression(exprf);
							changed = true;
						}
					}
				}
			}
			if (changed) {
				rule.clearFormulaResultCache();
			}
		}
		// update Validation's region (sqref)
		cfmt.renameSheet(oldName, newName);

		// notify chart change
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	//ZSS-555
	private void renameSheetFilterRef(SBook bookOfSheet, String oldName, String newName,ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(dependent.getSheetName())){
				sheet = book.getSheetByName(newName);
			}
		}
		if(sheet == null) {
			return;
		}
		SAutoFilter filter = sheet.getAutoFilter();
		if(filter == null) {
			return;
		}
		
		// update AutoFilter's region
		((AbstractAutoFilterAdv)filter).renameSheet(book, oldName, newName);

		// notify filter change
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	
	private void renameSheetCellRef(SBook bookOfSheet, String oldName, String newName,Ref dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		String sheetName = null;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(dependent.getSheetName())){
				sheet = book.getSheetByName(newName);
				sheetName = oldName;
			}
		} else {
			sheetName = sheet.getSheetName();
		}
		if(sheet==null) return;
		SCell cell = sheet.getCell(dependent.getRow(),
				dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */

		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.renameSheetPtgs(expr, bookOfSheet, oldName, newName,new FormulaParseContext(cell, sheetName, null));//null ref, no trace dependence here
		
		cell.setValue(exprAfter);
		//don't need to notify cell change, cell will do
	}	

	//ZSS-649
	private void renameSheetNameRef(SBook bookOfSheet, String oldName, String newName, NameRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		// ZSS-1337 must consider the NameRef's sheet scope
		final SName name = book.getNameByName(dependent.getNameName(), dependent.getSheetName());
		if(name==null) return;
		String sheetName = null;
		SSheet sheet = book.getSheetByName(name.getRefersToSheetName());
		if(sheet==null){//the sheet was renamed., get form newname if possible
			if(oldName.equals(name.getRefersToSheetName())){
				sheet = book.getSheetByName(newName);
				sheetName = oldName;
			}
		} else {
			sheetName = sheet.getSheetName();
		}
		if(sheet==null) return;
		
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression expr = ((AbstractNameAdv)name).getRefersToFormulaExpression();
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.renameSheetPtgs(expr, bookOfSheet, oldName, newName, new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
		
		((AbstractNameAdv)name).setRefersToFormula(exprAfter);
		//don't need to notify cell change, cell will do
	}	
	
	// ZSS-661
	public void renameName(SBook book, String oldName, String newName,
			Set<Ref> dependents, int sheetIndex) {
		for (Ref dependent : dependents) {
			if (dependent.getType() == RefType.CELL|| dependent.getType() == RefType.TABLE) {//ZSS-983
				renameNameCellRef(book, oldName, newName, dependent, sheetIndex);
			}
		}
	}	

	private void renameNameCellRef(SBook bookOfSheet, String oldName, String newName, Ref dependent, int sheetIndex) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if (book == null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if (sheet == null) return;
		SCell cell = sheet.getCell(dependent.getRow(), dependent.getColumn());
		if(cell.getType() != SCell.CellType.FORMULA)
			return;//impossible
		
		/*
		 * for Name rename case, we should always update formula to make new 
		 * dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = 
				engine.renameNamePtgs(expr, bookOfSheet, sheetIndex, oldName, newName, new FormulaParseContext(cell, null));
		
		cell.setValue(exprAfter);
		//don't need to notify cell change, cell will do
	}

	private void splitDependents(final Set<Ref> dependents,
			final Set<Ref> cellDependents,
			final Map<String, Ref> chartDependents,
			final Map<String, Ref> validationDependents,
			final Map<String, Ref> nameDependents,
			final Set<Ref> filterDependents,
			final Map<Integer, Ref> conditionalDependents) { //ZSS-1251
		
		for (Ref dependent : dependents) {
			RefType type = dependent.getType();
			if (type == RefType.CELL || dependent.getType() == RefType.TABLE) {//ZSS-983
				cellDependents.add(dependent);
			} else if (type == RefType.OBJECT) {
				if(((ObjectRef)dependent).getObjectType()==ObjectType.CHART){
					chartDependents.put(((ObjectRef)dependent).getObjectIdPath()[0], dependent);
				}else if(((ObjectRef)dependent).getObjectType()==ObjectType.DATA_VALIDATION){
					validationDependents.put(((ObjectRef)dependent).getObjectIdPath()[0], dependent);
				}else if(((ObjectRef)dependent).getObjectType()==ObjectType.AUTO_FILTER){
					filterDependents.add(dependent);
				}
			} else if (type == RefType.NAME) { //ZSS-649
				nameDependents.put(((NameRef)dependent).toString(), dependent);
			} else if (type == RefType.CONDITIONAL) { //ZSS-1251
				conditionalDependents.put(((ConditionalRef)dependent).getConditionalId(), dependent);
			} else {// TODO another

			}
		}
	}

	//ZSS-687
	private void clearFormulaCache(NameRef precedent) {
		Map<String,Ref> chartDependents  = new HashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new HashMap<String, Ref>();
		Set<Ref> nameDependents  = new HashSet<Ref>();
		AbstractBookSeriesAdv bs = (AbstractBookSeriesAdv) _bookSeries;
		DependencyTable dt = bs.getDependencyTable();
		
		clearFormulaCache(precedent, dt, chartDependents, validationDependents, nameDependents);

		for (Ref dependent : chartDependents.values()) {
			clearFormulaCacheChartRef((ObjectRef)dependent);
		}
		for (Ref dependent : validationDependents.values()) {
			clearFormulaCacheDataValidationRef((ObjectRef)dependent);
		}		
	}

	// ZSS-687
	private void clearFormulaCache(NameRef precedent,
			DependencyTable dt, 
			Map<String,Ref> chartDependents,
			Map<String,Ref> validationDependents,
			Set<Ref> nameDependents) {
		
		Set<Ref> dependents = dt.getDependents(precedent);
		
		for (Ref dependent : dependents) {
			RefType type = dependent.getType(); 
			if (type == RefType.CELL || dependent.getType() == RefType.TABLE) {//ZSS-983
				clearFormulaCacheCellRef(dependent);
			} else if (type == RefType.OBJECT) {
				if(((ObjectRef)dependent).getObjectType()==ObjectType.CHART){
					chartDependents.put(((ObjectRef)dependent).getObjectIdPath()[0], dependent);
				}else if(((ObjectRef)dependent).getObjectType()==ObjectType.DATA_VALIDATION){
					validationDependents.put(((ObjectRef)dependent).getObjectIdPath()[0], dependent);
				}
			} else if (type == RefType.NAME) {
				if (!nameDependents.contains(dependent)) {
					nameDependents.add(dependent);
					// recursive back when NameRef depends on NameRef
					clearFormulaCache((NameRef)dependent, dt, 
						chartDependents, validationDependents, nameDependents); 
				}
			} else {// TODO another

			}
		}
	}

	// ZSS-687, ZSS-648
	private void clearFormulaCacheDataValidationRef(ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SDataValidation validation = sheet.getDataValidation(dependent.getObjectIdPath()[0]);
		if(validation == null) {
			return;
		}
		
		validation.clearFormulaResultCache();
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	
	// ZSS-687
	private void clearFormulaCacheChartRef(ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book == null) {
			return;
		}
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet == null) {
			return;
		}
		SChart chart = sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart == null) {
			return;
		}
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) {
			return;
		}
		SGeneralChartData data = (SGeneralChartData)d;

		data.clearFormulaResultCache();
		ModelUpdateUtil.addRefUpdate(dependent);
	}

	// ZSS-687
	private void clearFormulaCacheCellRef(Ref dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		SCell cell = sheet.getCell(dependent.getRow(),
				dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		//zss-626, has to clear cache and notify ref update
		cell.clearFormulaResultCache();
		ModelUpdateUtil.addRefUpdate(dependent);
	}
	
	// ZSS-820
	public void reorderSheet(SBook book, int oldIndex, int newIndex, Set<Ref> dependents) {
		//because of the chart shifting is for all chart, but the input dependent is on series,
		//so we need to collect the dependent for only shift chart once
		Map<String,Ref> chartDependents  = new LinkedHashMap<String, Ref>();
		Map<String,Ref> validationDependents  = new LinkedHashMap<String, Ref>();
		Set<Ref> cellDependents = new LinkedHashSet<Ref>(); //ZSS-649
		Map<String,Ref> nameDependents = new LinkedHashMap<String, Ref>(); //ZSS-649
		Set<Ref> filterDependents = new LinkedHashSet<Ref>(); //ZSS-555
		Map<Integer,Ref> conditionalDependents  = new LinkedHashMap<Integer, Ref>(); //ZSS-1251
		
		splitDependents(dependents, cellDependents, chartDependents, validationDependents, nameDependents, filterDependents, conditionalDependents);//ZSS-1251
		
		for (Ref dependent : cellDependents) {
			reorderSheetCellRef(book,oldIndex,newIndex,dependent);
		}
		for (Ref dependent : chartDependents.values()) {
			reorderSheetChartRef(book,oldIndex,newIndex,(ObjectRef)dependent);
		}
//20141029, henrichen: validation and filter are associated with single value, will not be affected by sheet index reording
//  thus we don't have to handle them.
//		for (Ref dependent : validationDependents.values()) {
//			reorderSheetDataValidationRef(book,oldIndex,newIndex,(ObjectRef)dependent);
//		}	
		//ZSS-649
		for (Ref dependent : nameDependents.values()) {
			reorderSheetNameRef(book,oldIndex,newIndex,(NameRef)dependent);
		}
//		//ZSS-555
//		for (Ref dependent : filterDependents) {
//			reorderSheetFilterRef(book,oldIndex,newIndex,(ObjectRef)dependent);
//		}
		
		//ZSS-1251
//		for (Ref dependent : conditionalDependents.values()) {
//			reorderSheetDataValidationRef(book,oldIndex,newIndex,(ConditionalRef)dependent);
//		}	
	}	
	
	private void reorderSheetChartRef(SBook bookOfSheet, int oldIndex, int newIndex,ObjectRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		final String sheetName = sheet.getSheetName();
		SChart chart =  sheet.getChart(dependent.getObjectIdPath()[0]);
		if(chart==null) return;
		SChartData d = chart.getData();
		if(!(d instanceof SGeneralChartData)) return;
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter;
		SGeneralChartData data = (SGeneralChartData)d;
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression catExpr = ((AbstractGeneralChartDataAdv)data).getCategoriesFormulaExpression();
		if(catExpr!=null){
			exprAfter = engine.reorderSheetPtgs(catExpr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
			if(!exprAfter.hasError()){
				((AbstractGeneralChartDataAdv)data).setCategoriesFormula(exprAfter);
			}
		}
		
		for(int i=0;i<data.getNumOfSeries();i++){
			SSeries series = data.getSeries(i);
			FormulaExpression nameExpr = ((AbstractSeriesAdv)series).getNameFormulaExpression();
			FormulaExpression xvalExpr = ((AbstractSeriesAdv)series).getXValuesFormulaExpression();
			FormulaExpression yvalExpr = ((AbstractSeriesAdv)series).getYValuesFormulaExpression();
			FormulaExpression zvalExpr = ((AbstractSeriesAdv)series).getZValuesFormulaExpression();
			if(nameExpr!=null){
				exprAfter = engine.reorderSheetPtgs(nameExpr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					nameExpr = exprAfter;
				}
			}
			if(xvalExpr!=null){
				exprAfter = engine.reorderSheetPtgs(xvalExpr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					xvalExpr = exprAfter;
				}
			}
			if(yvalExpr!=null){
				exprAfter = engine.reorderSheetPtgs(yvalExpr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					yvalExpr = exprAfter;
				}
			}
			if(zvalExpr!=null){
				exprAfter = engine.reorderSheetPtgs(zvalExpr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
				if(!exprAfter.hasError()){
					zvalExpr = exprAfter;
				}
			}
			((AbstractSeriesAdv)series).setXYZFormula(nameExpr, xvalExpr, yvalExpr, zvalExpr);
		}
		
		ModelUpdateUtil.addRefUpdate(dependent);
		
	}	
	
	private void reorderSheetCellRef(SBook bookOfSheet, int oldIndex, int newIndex,Ref dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if(sheet==null) return;
		String sheetName = sheet.getSheetName();
		SCell cell = sheet.getCell(dependent.getRow(), dependent.getColumn());
		if(cell.getType()!= SCell.CellType.FORMULA)
			return;//impossible
		
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */

		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.reorderSheetPtgs(expr, bookOfSheet, oldIndex, newIndex,new FormulaParseContext(cell, sheetName, null));//null ref, no trace dependence here
		
		cell.setValue(exprAfter);
		//don't need to notify cell change, cell will do
	}	

	//ZSS-649
	private void reorderSheetNameRef(SBook bookOfSheet, int oldIndex, int newIndex, NameRef dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if(book==null) return;
		// ZSS-1337 must consider the NameRef's sheet scope
		final SName name = book.getNameByName(dependent.getNameName(), dependent.getSheetName());
		if(name==null) return;
		SSheet sheet = book.getSheetByName(name.getRefersToSheetName());
		if(sheet==null) return;
		String sheetName = sheet.getSheetName();
		
		/*
		 * for sheet rename case, we should always update formula to make new dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression expr = ((AbstractNameAdv)name).getRefersToFormulaExpression();
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = engine.reorderSheetPtgs(expr, bookOfSheet, oldIndex, newIndex, new FormulaParseContext(sheet, sheetName, null));//null ref, no trace dependence here
		
		((AbstractNameAdv)name).setRefersToFormula(exprAfter);
		//don't need to notify cell change, cell will do
	}	
	
	// ZSS-966
	public void renameTableName(SBook book, String oldName, String newName,
			Set<Ref> dependents) {
		for (Ref dependent : dependents) {
			if (dependent.getType() == RefType.CELL || dependent.getType() == RefType.TABLE) { //ZSS-983
				renameTableNameCellRef(book, oldName, newName, dependent);
			}
		}
	}	

	//ZSS-966
	private void renameTableNameCellRef(SBook bookOfSheet, String oldName, String newName, Ref dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if (book == null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if (sheet == null) return;
		SCell cell = sheet.getCell(dependent.getRow(), dependent.getColumn());
		if(cell.getType() != SCell.CellType.FORMULA)
			return;//impossible
		
		/*
		 * for Name rename case, we should always update formula to make new 
		 * dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = 
				engine.renameTableNameTablePtgs(expr, bookOfSheet, oldName, newName, new FormulaParseContext(cell, null));
		
		cell.setValue(exprAfter);
		//don't need to notify cell change, cell will do
	}


	// ZSS-967
	public void renameColumnName(STable table, String oldName, String newName,
			Set<Ref> dependents) {
		for (Ref dependent : dependents) {
			if (dependent.getType() == RefType.CELL || dependent.getType() == RefType.TABLE) {
				renameColumnNameCellRef(table, oldName, newName, dependent);
			}
		}
	}	

	//ZSS-967
	private void renameColumnNameCellRef(STable table, String oldName, String newName, Ref dependent) {
		SBook book = _bookSeries.getBook(dependent.getBookName());
		if (book == null) return;
		SSheet sheet = book.getSheetByName(dependent.getSheetName());
		if (sheet == null) return;
		SCell cell = sheet.getCell(dependent.getRow(), dependent.getColumn());
		if(cell.getType() != SCell.CellType.FORMULA)
			return;//impossible
		
		/*
		 * for table's column rename case, we should always update formula to make new 
		 * dependency, shouln't ignore if the formula string is the same
		 * Note, in other move cell case, we could ignore to set same formula string
		 */
		FormulaExpression expr = (FormulaExpression) ((AbstractCellAdv)cell).getValue(false);
		
		FormulaEngine engine = getFormulaEngine();
		FormulaExpression exprAfter = 
				engine.renameColumnNameTablePtgs(expr, table, oldName, newName, new FormulaParseContext(cell, null));
		
		cell.setValue(exprAfter);
		//don't need to notify cell change, cell will do
	}

}
