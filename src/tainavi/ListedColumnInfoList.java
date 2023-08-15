package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * 「リスト形式」画面の {@link ListColumnInfo} のリストを実現するクラスです.
 * @since 3.22.18β+1.9
 */
public class ListedColumnInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"lcinfolist.xml";

	@Override
	public Object clone(){
		ListedColumnInfoList p = new ListedColumnInfoList();

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
	 * IDを指定して取得する
	 */
	public ListColumnInfo getById(int id){
		for (ListColumnInfo li : this){
			if (li.getId() == id)
				return li;
		}

		return null;
	}

	/*
	 * 名称を指定して表示インデックスを取得する
	 *
	 * @param name	列の名称
	 * @return		列のインデックス
	 */
	public int getVisibleIndexByName(String name){
		int idx = 0;
		for (ListColumnInfo li : this){
			if (!li.getVisible())
				continue;
			if (li.getName().equals(name))
				return idx;
			idx++;
		}

		return -1;
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
		System.out.println("リスト形式の表示項目設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("リスト形式の表示項目設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	public boolean load() {
		System.out.println("リスト形式の表示項目設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (ListedColumnInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("リスト形式の表示項目設定が読み込めなかったのでデフォルト設定で起動します.");

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
			{true, "予約",			idx++},
			{true, "ﾋﾟｯｸ",			idx++},
			{true, "重複",			idx++},
			{true, "チャンネル名",	idx++},
			{true, "オプション",	idx++},
			{true, "番組タイトル",	idx++},
			{true, "番組詳細",		idx++},
			{true, "開始時刻",		idx++},
			{true, "終了",			idx++},
			{true, "長さ",			idx++},
			{true, "ジャンル",		idx++},
			{true, "検索アイテム名",idx++},
			{true, "お気に入り度",	idx++},
			{true, "ｽｺｱ",			idx++},
			{true, "閾値",			idx++},
			{false, "予約１",		idx++},
			{false, "予約２",		idx++},
			{false, "予約３",		idx++},
			{false, "予約４",		idx++},
			{false, "予約５",		idx++},
			{false, "予約６",		idx++},
			{false, "予約７",		idx++},
			{false, "予約８",		idx++},
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
