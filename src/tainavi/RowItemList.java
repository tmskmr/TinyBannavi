package tainavi;

import java.util.ArrayList;

/**
 * <P>- JTableの実態のデータを統一したインタフェースであつかうようにできればいーんじゃない？ -
 * <P>テーブルの操作をまとめたもの
 * <P>
 * <P> ソートが必要な場合は、RowItemListの操作はTableModelのサブクラスで実装する。ただし、その場合Viewのrowがわからないので行の入れ替えが行えない
 * <P> ソートが必要がないならRowItemListの操作はJTableのサブクラスを作ればよい
 */
public class RowItemList<T> extends ArrayList<T> {

	private static final long serialVersionUID = 1L;

	public boolean up(int top, int length) {
		if ( top < 1 ) {
			// ２行目より上ならもうなにもできない
			return false;
		}
		T a = this.get(top-1);
		for ( int i=0; i<length; i++ ) {
			this.set(top+i-1,this.get(top+i));
		}
		this.set(top+length-1,a);
		
		return true;
	}
	
	public boolean down(int top, int length) {
		if ( (top+length) > (this.size()-1) ) {
			// 最終行までいってるならもうなにもできない
			return false;
		}
			
		T a = this.get(top+length);
		for ( int i=length-1; i>=0; i-- ) {
			this.set(top+i+1,this.get(top+i));
		}
		this.set(top,a);
		
		return true;
	}
}
