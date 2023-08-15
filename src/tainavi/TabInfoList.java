package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * タイトル一覧で {@link ListColumnInfo} のリストを実現するクラスです.
 * @since 3.22.18β+1.9
 */
public class TabInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"tabinfolist.xml";

	@Override
	public Object clone(){
		TabInfoList p = new TabInfoList();

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

	public boolean getVisibleAt(String name){
		if (name == null)
			return false;

		for (ListColumnInfo li : this){
			if (name.equals(li.getName()))
				return li.getVisible();

		}

		return false;
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
		System.out.println("タブ表示項目設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("タブ表示項目設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	/*
	 * ファイルから読み込む
	 */
	public boolean load() {
		System.out.println("タブ表示項目設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (TabInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("タブ表示項目設定が読み込めなかったのでデフォルト設定で起動します.");

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
	 * デフォルトの設定にする
	 */
	public void setDefault(){
    	this.clear();

    	int idx = 1;
    	Object[][] o = {
			{true, "リスト形式",	idx++},
			{true, "新聞形式",		idx++},
			{true, "本体予約一覧",	idx++},
			{true, "録画結果一覧",	idx++},
			{true, "自動予約一覧",	idx++},
			{true, "タイトル一覧",	idx++},
			{true, "設定一覧",		idx++},
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
