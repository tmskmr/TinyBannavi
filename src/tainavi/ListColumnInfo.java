package tainavi;

/**
 * <P>リストの表示項目名を保持するクラスです。
 * @since 3.22.18β+1.9
 * @see ListedColumnInfoList
 */
public class ListColumnInfo implements Cloneable {
	@Override
	public Object clone() {
		try {
			ListColumnInfo p = (ListColumnInfo) super.clone();
			return p;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	private boolean visible = false ;	// 表示対象か否か
	private String name = "" ;	// 項目名
	private int id = 0 ;		// ID

	public void setVisible(boolean b) { this.visible = b; }
	public boolean getVisible() { return this.visible; }

	public void setName(String s) { this.name = s; }
	public String getName() { return this.name; }

	public void setId(int id) { this.id = id; }
	public int getId() { return this.id; }
}
