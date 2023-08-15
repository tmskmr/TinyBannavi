package tainavi;

import java.util.ArrayList;

public interface SearchItem {

	/**
	 * 検索条件のラベル
	 */
	public String toString();
	
	/**
	 * 検索条件にマッチした番組情報のリストのクリア
	 */
	public void clearMatchedList();

	/**
	 * 検索条件にマッチした番組情報の追加
	 */
	public void addMatchedList(ProgDetailList pdl);

	/**
	 * 検索条件にマッチした番組情報のリストの取得
	 */
	public ArrayList<ProgDetailList> getMatchedList();

	/**
	 * 検索にマッチした番組が存在するかどうか
	 */
	public boolean isMatched();

}
