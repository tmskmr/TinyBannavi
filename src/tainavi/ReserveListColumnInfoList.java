package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * 「本体予約一覧」画面の{@link ListColumnInfo} のリストを実現するクラスです.
 * @since 3.22.18β+1.9
 */
public class ReserveListColumnInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"rlcinfolist.xml";

	@Override
	public Object clone(){
		ReserveListColumnInfoList p = new ReserveListColumnInfoList();

		for (ListColumnInfo li : this){
			p.add((ListColumnInfo)li.clone());
		}

		return p;
	}

	public ListColumnInfo getVisibleAt(int column){
		int cno = 0;
		for (ListColumnInfo li : this){
			if ( !li.getVisible() )
				continue;

			if (cno == column)
				return li;

			cno++;
		}

		return null;
	}

	/*
	 * JTableに指定する列名の配列を作成する
	 */
	public String [] getColNames(){
		// カラム名の初期化
		ArrayList<String> cola = new ArrayList<String>();

		for ( ListColumnInfo li : this ) {
			if ( li.getVisible() ) {
				cola.add(li.getName());
			}
		}

		return cola.toArray(new String[0]);
	}

	public boolean save() {
		System.out.println("本体予約一覧の表示項目設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("本体予約一覧の表示項目設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	public boolean load() {
		System.out.println("本体予約一覧の表示項目設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (ReserveListColumnInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("本体予約一覧の表示項目設定が読み込めなかったのでデフォルト設定で起動します.");

	    	// 初期化してみよう
			setDefault();

			return false;
		}

		this.clear();
		for (ListColumnInfo c : cl) {
			this.add(c);
		}

		return true;
	}

	/*
	 * 初期化する
	 */
	public void setDefault(){
    	this.clear();
    	int idx = 1;
    	Object[][] o = {
			{true, "パタン",		idx++},
			{true, "重複",			idx++},
			{true, "実行",			idx++},
			{true, "追跡",			idx++},
			{true, "自動",			idx++},
			{true, "オプション",	idx++},
			{true, "次回実行予定",	idx++},
			{true, "終了",			idx++},
			{true, "長さ",			idx++},
			{true, "ｴﾝｺｰﾀﾞ",		idx++},
			{true, "画質",			idx++},
			{true, "音質",			idx++},
			{true, "番組タイトル",	idx++},
			{true, "チャンネル名",	idx++},
			{true, "デバイス",		idx++},
			{true, "フォルダ",		idx++},
			{true, "レコーダ",		idx++},
    	};
    	for (int i=0; i<o.length; i++) {
    		ListColumnInfo cb = new ListColumnInfo();
        	cb.setVisible((Boolean) o[i][0]);
        	cb.setName((String) o[i][1]);
        	cb.setId((Integer) o[i][2]);
        	this.add(cb);
    	}
	}

}
