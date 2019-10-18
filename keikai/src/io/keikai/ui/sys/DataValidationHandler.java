/* DataValidationLoader.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/8/7 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.ui.sys;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import io.keikai.api.model.Sheet;
import org.zkoss.zk.ui.event.EventListener;

/**
 * @author dennis
 * @since 3.0.0
 */
public interface DataValidationHandler extends Serializable {
	
	public List<Map<String, Object>> loadDataValidtionJASON(Sheet sheet);
	
	public boolean validate(Sheet sheet, final int row, final int col,
			final String editText, final EventListener callback);

}
