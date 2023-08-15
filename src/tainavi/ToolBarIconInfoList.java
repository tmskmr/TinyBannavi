package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * ツールバーで {@link ListColumnInfo} のリストを実現するクラスです.
 * @since 3.22.18β+1.15.6
 */
public class ToolBarIconInfoList extends ArrayList<ListColumnInfo> implements Cloneable{

	private static final long serialVersionUID = 1L;

	private static final String listFile = "env"+File.separator+"tbiinfolist.xml";

	@Override
	public Object clone(){
		ToolBarIconInfoList p = new ToolBarIconInfoList();

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
		System.out.println("ツールバーの表示アイコン設定を保存します: "+listFile);

		if ( ! CommonUtils.writeXML(listFile, this) ) {
        	System.err.println("ツールバーの表示遺恨設定の保存に失敗しました： "+listFile);
        	return false;
		}

		return true;
	}

	/*
	 * ファイルから読み込む
	 */
	public boolean load() {
		System.out.println("ツールバーの表示アイコン設定を読み込みます: "+listFile);

		ArrayList<ListColumnInfo> cl = null;

		if ( new File(listFile).exists() ) {
			// ファイルがあるならロード
			cl = (ToolBarIconInfoList) CommonUtils.readXML(listFile);
		}

		if ( cl == null || cl.size() == 0 ) {
			System.err.println("ツールバーの表示アイコン設定が読み込めなかったのでデフォルト設定で起動します.");

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
		ToolBarIconInfoList cl = new ToolBarIconInfoList();
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
    	for (ToolBarIconInfo info : ToolBarIconInfo.values()) {
    		ListColumnInfo cb = new ListColumnInfo();
        	cb.setVisible(info.getVisible());
        	cb.setName(info.getName());
        	cb.setId(idx++);

        	this.add(cb);
    	}
	}

	/*
	 * デフォルトの設定かどうかを返す
	 */
	public boolean isDefault(){
		ToolBarIconInfoList cl = new ToolBarIconInfoList();
		cl.setDefault();

		if (cl.size() != this.size())
			return false;

		for (int n=0; n<cl.size(); n++){
			ListColumnInfo c = cl.get(n);
			ListColumnInfo c2 = this.get(n);

			if (!c.getName().equals(c2.getName()) || c.getVisible() != c2.getVisible())
				return false;
		}

		return true;
	}
}
