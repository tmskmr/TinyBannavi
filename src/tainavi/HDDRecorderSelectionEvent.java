package tainavi;

import java.util.EventObject;


/**
 * ツールバーのレコーダ選択コンボボックスが操作されたよイベントオブジェクト
 */
public class HDDRecorderSelectionEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	
	// この引数の群れはいいのか？
	public HDDRecorderSelectionEvent(Object source) {
		super(source);
	}

}
