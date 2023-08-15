package tainavi;

import tainavi.SearchKey.TargetId;


/***
 * 
 * キーワード検索の設定のクラス
 * 
 */
abstract class AbsExtensionDialog extends AbsKeywordDialog {

	private static final long serialVersionUID = 1L;

	//JComboBox jComboBox_infection = null;
	
	// コンストラクタ
	@Override
	protected void setItems() {
		setWindowTitle("延長警告キーワードの設定");
		
		clean_target_items();
		add_target_item(TargetId.TITLEANDDETAIL);
		add_target_item(TargetId.TITLE);
		add_target_item(TargetId.DETAIL);
		add_target_item(TargetId.CHANNEL);
		add_target_item(TargetId.GENRE);
		add_target_item(TargetId.SUBGENRE);
		add_target_item(TargetId.NEW);
		add_target_item(TargetId.LAST);
		add_target_item(TargetId.REPEAT);
		add_target_item(TargetId.FIRST);
		add_target_item(TargetId.SPECIAL);
		add_target_item(TargetId.NOSCRUMBLE);
		add_target_item(TargetId.LIVE);
		add_target_item(TargetId.LENGTH);	
		add_target_item(TargetId.STARTA);
		add_target_item(TargetId.STARTZ);
		add_target_item(TargetId.STARTDATETIME);
		
		clean_contain_items();
		add_contain_item("を含む番組");
		add_contain_item("を含む番組を除く");
		
		clean_condition_items();
		add_condition_item("次のすべての条件に一致");
		add_condition_item("次のいずれかの条件に一致");
    	
		clean_infection_items();
    	add_infection_item("延長感染源にする");
    	add_infection_item("延長感染源にしない");
	}
	
	public AbsExtensionDialog() {
		super();
	}
}