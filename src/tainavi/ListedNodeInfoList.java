package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * リスト形式で {@link ListColumnInfo} のノードのリストを実現するクラスです.
 * @since 3.22.18β+1.9
 */
public class ListedNodeInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"lninfolist.xml";

	@Override
	public Object clone(){
		ListedNodeInfoList p = new ListedNodeInfoList();

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
	 * JTreeViewに指定するノード名の配列を作成する
	 */
	public String [] getNodeNames(){
		// カラム名の初期化
		ArrayList<String> cola = new ArrayList<String>();

		for ( ListColumnInfo li : this ) {
			if ( li.getVisible() ) {
				cola.add(li.getName());
			}
		}

		return cola.toArray(new String[0]);
	}

	/*
	 * ファイルに保存する
	 */
	public boolean save() {
		System.out.println("リスト形式のツリーの表示項目設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("リスト形式のツリーの表示項目設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	/*
	 * ファイルから読み込む
	 */
	public boolean load() {
		System.out.println("リスト形式のツリーの表示項目設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (ListedNodeInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("リスト形式のツリーの表示項目設定が読み込めなかったのでデフォルト設定で起動します.");

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
			{true, "過去ログ検索履歴", idx++},
			{true, "新番組一覧",		idx++},
			{true, "最終回一覧",		idx++},
			{true, "現在放送中",		idx++},
			{true, "予約待機",		idx++},
			{true, "番組追跡",		idx++},
			{true, "キーワード検索",	idx++},
			{true, "キーワードグループ",idx++},
			{true, "ジャンル別",		idx++},
			{true, "放送局別",		idx++},
			{true, "延長警告管理",	idx++},
			{true, "しょぼかる",	idx++},
   			{false, "ピックアップ",		idx++},
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
