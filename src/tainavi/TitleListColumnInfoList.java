package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * タイトル一覧で {@link ListColumnInfo} のリストを実現するクラスです.
 * @since 3.22.18β+1.9
 */
public class TitleListColumnInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"tlcinfolist.xml";

	@Override
	public Object clone(){
		TitleListColumnInfoList p = new TitleListColumnInfoList();

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

	/*
	 * ファイルに保存する
	 */
	public boolean save() {
		System.out.println("タイトル一覧の表示項目設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("タイトル一覧の表示項目設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	/*
	 * ファイルから読み込む
	 */
	public boolean load() {
		System.out.println("タイトル一覧の表示項目設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (TitleListColumnInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("タイトル一覧の表示項目設定が読み込めなかったのでデフォルト設定で起動します.");

	    	// 初期化してみよう
			setDefault();

			return false;
		}

		this.clear();
		for (ListColumnInfo c : cl) {
			this.add(c);
		}

		addNewColumns();

		return true;
	}

	/*
	 * 新しい列を追加する
	 */
	private void addNewColumns(){
		TitleListColumnInfoList cl = new TitleListColumnInfoList();
		cl.setDefault();

		for (int n=0; n<cl.size(); n++){
			ListColumnInfo c = cl.get(n);

			int n2=0;
			for (n2=0; n2<this.size(); n2++){
				ListColumnInfo c2 = this.get(n2);
				if (c.getName().equals(c2.getName()))
					break;
			}

			if (n2 == this.size()){
				this.add((ListColumnInfo)c.clone());
			}
		}
	}

	/*
	 * デフォルトの設定にする
	 */
	public void setDefault(){
    	this.clear();

    	int idx = 1;
    	Object[][] o = {
			{true, "開始",	idx++},
			{true, "終了",	idx++},
			{true, "長さ",	idx++},
			{true, "画質",	idx++},
			{true, "番組タイトル",	idx++},
			{true, "チャンネル名",	idx++},
			{true, "デバイス",		idx++},
			{true, "フォルダ",		idx++},
			{true, "ジャンル",		idx++},
			{true, "レコーダ",		idx++},
			{true, "コピー",		idx++},
			{false, "DLNA OID",		idx++},
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
