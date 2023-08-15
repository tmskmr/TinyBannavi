package tainavi;

import javax.swing.event.ChangeEvent;


/**
 * ツールバーのレコーダ選択コンボボックスが操作されたよイベントオブジェクト
 */
public class HDDRecorderChangeEvent extends ChangeEvent {

	private static final long serialVersionUID = 1L;

	public HDDRecorderChangeEvent(HDDRecorderSelectable source) {
		super(source);
	}

}
