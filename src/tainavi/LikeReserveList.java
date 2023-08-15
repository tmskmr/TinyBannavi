package tainavi;

import java.util.ArrayList;


public class LikeReserveList extends ArrayList<LikeReserveItem> {

	private static final long serialVersionUID = 1L;

	
	/**
	 * 一番開始日時が近い類似予約を選択
	 * @param myself 「すべて」「ピックアップのみ」の場合はnullを返す 
	 */
	public LikeReserveItem getClosest(String myself) {
		
		closest = null;
		closestIndex = -1;
		
		if ( myself == HDDRecorder.SELECTED_ALL || myself == HDDRecorder.SELECTED_PICKUP ) {
			// レコーダの個別指定がなければ
			return closest;
		}
		
		for ( int i=0; i<size(); i++ ) {
			LikeReserveItem lr = super.get(i);
			if ( ! lr.getRec().Myself().equals(myself) ) {
				continue;
			}
			
			if ( closest == null || Math.abs(closest.getDist()) > Math.abs(lr.getDist()) ) {
				closest = lr;
				closestIndex = i;
			}
		}
		
		return closest;
	}
	
	public LikeReserveItem getClosest() { return closest; }
	public int getClosestIndex() { return closestIndex; }

	private LikeReserveItem closest = null;
	private int closestIndex = -1;
	

	/**
	 * 選択された類似予約を取得
	 */
	public LikeReserveItem getSelected() { return selected; }
	private LikeReserveItem selected = null;
	
	/**
	 * get()を行いつつget()の結果の記録もする
	 * <P><I>注意！get()はイテレータの中で使用されるので、get()をoverrideするとループによりselectedの値が変動してしまう！！</I>
	 */
	public LikeReserveItem setSelectedIndex(int index) {
		return selected = super.get(index);
	}
	
	/**
	 * つかってもいいけどselectedが使えないよ。{@link #setSelectedIndex(int)}を使ってね
	 */
	@Deprecated
	@Override
	public LikeReserveItem get(int index) {
		return super.get(index);
	}
	
	/**
	 * 時刻昇順で並べる
	 */
	@Override
	public boolean add(LikeReserveItem element) {
		
		if ( size() < 0 ) {
			return super.add(element);
		}
		
		for ( int i=0; i<size(); i++ ) {
			LikeReserveItem lr = super.get(i);
			if ( lr.getDist() > element.getDist() ) {
				super.add(i, element);
				return true;
			}
		}
		
		return super.add(element);
	}
	
	
	/**
	 * つかっちゃヤーン
	 */
	@Deprecated
	@Override
	public void add(int index,LikeReserveItem element) {
		this.add(element);
	}

	/**
	 * 重複する予約情報を持つ類似予約を削除する
	 */
	public LikeReserveItem removeDup(LikeReserveItem item) {
		int index = indexOfDup(item);
		if ( index < 0 ) {
			return null;
		}
		return super.remove(index);
	}
	
	private int indexOfDup(LikeReserveItem item) {
		for ( int i=0; i<size(); i++ ) {
			if ( item.getRsv() == super.get(i).getRsv() ) {
				return i;
			}
		}
		return -1;
	}
}
