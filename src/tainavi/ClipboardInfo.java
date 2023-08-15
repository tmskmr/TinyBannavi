package tainavi;

/**
 * <P>クリップボードにコピーする項目名を保持するクラスです。{@link clipboardItem} から移行しました。
 * @since 3.15.4β
 * @see ClipboardInfoList
 */
public class ClipboardInfo {

	private boolean b = false ;	// 有効か無効か
	private String item = "" ;	// 項目名
	private int id = 0 ;		// 順番
	
	public void setB(boolean b) { this.b = b; }
	public boolean getB() { return this.b; }
	
	public void setItem(String item) { this.item = item; }
	public String getItem() { return this.item; }
	
	public void setId(int id) { this.id = id; }
	public int getId() { return this.id; }

}
