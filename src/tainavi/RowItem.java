package tainavi;

import java.util.ArrayList;

/**
 * <P>- JTableの実態のデータを統一したインタフェースであつかうようにできればいーんじゃない？ -
 * <P>テーブルの各行のデータを保持するもの
 */
public abstract class RowItem implements Cloneable {

	/*
	 *  抽象メソッド
	 */
	
	abstract protected void myrefresh(RowItem o);
	
	/*
	 * 共用部
	 */
	
	// JTableからの参照用にインデックスを張る
	private ArrayList<Object> data = new ArrayList<Object>();
	
	public int getColumnCount() { return (data == null)?(-1):(data.size()); }
	
	public Object get(int index) { return data.get(index); }
	
	public int size() { return data.size(); }
	
	// メンバを更新したらインデックスを張り替える
	public void fireChanged() {
		data = new ArrayList<Object>();
		myrefresh(this);
		//System.err.println("RowItem#refresh: "+data.size());
	}
	
	protected void addData(Object o) { data.add(o); }
	
	protected void clean() { data = new ArrayList<Object>(); }
	
	@Override
	public RowItem clone() {
		try {
			RowItem o = (RowItem) super.clone();
			o.clean();
			myrefresh(o);
			return o;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
}
