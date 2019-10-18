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

import java.io.*;

import io.keikai.model.SBook;
import org.zkoss.lang.Library;
import org.zkoss.poi.POIXMLDocument;
import org.zkoss.poi.poifs.filesystem.POIFSFileSystem;

/**
 * 
 * @author dennis
 * @author Hawk
 * @since 3.5.0
 */
public class ExcelImportAdapter extends AbstractImporter {

	@Override
	public SBook imports(InputStream is, String bookName) throws IOException {
		if(!is.markSupported()) {
			is = new PushbackInputStream(is, 8);
		}
		AbstractExcelImporter importer = null;
		if (POIFSFileSystem.hasPOIFSHeader(is)) {
			importer = new ExcelXlsImporter();
		}else if (POIXMLDocument.hasOOXMLHeader(is)) {
			importer =new ExcelXlsxImporter();
		}
		if (importer != null) {
			importer.setImportCache(this.isImportCache()); //ZSS-873
			return importer.imports(is, bookName);
		}
		throw new IllegalArgumentException("The input stream to be imported is neither an OLE2 stream, nor an OOXML stream");
	}
	
	//ZSS-873
	protected boolean isImportCache() {
		String importCache = Library.getProperty("io.keikai.import.cache", "false");
		return "true".equalsIgnoreCase(importCache.trim());
	}
}
