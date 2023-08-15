package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * <P>ツールバーの検索キーワードのリストを保持するクラスです。
 * @since 3.22.18β+1.10
 *
 * @see ListedColumnInfoList
 */

public class SearchWordList {
	/*
	 * 定数
	 */
	private final String filename = "env"+File.separator+"searchwords.xml";

	private final String MSGID = "[検索ワード] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final int MAXWORDS = 128;

	/*
	 * 部品
	 */
	private ArrayList<SearchWordItem> list = new ArrayList<SearchWordItem>();

	public int size() { return list.size(); }

	/*
	 * 検索キーワードを追加する
	 *
	 * @param s 検索キーワード
	 * @param k 検索キー
	 */
	public boolean add(String s, SearchKey k){
		return add(s, s, k, null, null);
	}

	/*
	 * 検索キーワードを追加する
	 *
	 * @param l0 検索キーワードの名称
	 * @param s 検索キーワード
	 * @param k 検索キー
	 * @param f 日付の下限
	 * @param t 日付の上限
	 */
	public boolean add(String l0, String s, SearchKey k, String f, String t){
		if (s == null)
			return false;

		// 同じ内容の検索キーワードがあったら検索回数を増やす
		for (SearchWordItem item : list){

			if (item.equals(s, k, f, t)){
				item.notifyUse();
				sortList();
				return true;
			}
		}

		// 名称未指定の場合
		if (l0.isEmpty())
			l0 = "(キーワードなし)";

		// (n)のサフィックスがある場合は取る
		int count=0;
		Matcher ma = Pattern.compile("^(.*)\\((\\d+)\\)$").matcher(l0);
		if (ma.find()){
			l0 = ma.group(1);
			count = Integer.parseInt(ma.group(2));
		}

		String l = l0;

		// 名称が被らなくなるまでサフィックスを増やす
		while(true){
			SearchWordItem item = getItemFromLabel(l);
			if (item == null)
				break;

			count++;
			l = l0 + "(" + count + ")";
		}

		// 追加する
		SearchWordItem item = new SearchWordItem(l, s, k, f, t);
		list.add(item);
		sortList();

		// 上限を超えたら古いものから削除する
		while (list.size() > MAXWORDS){
			list.remove(list.size()-1);
		}

		return true;
	}


	/**
	 * 検索ワードの一覧を返す
	 */
	public ArrayList<SearchWordItem> getWordList() {
		return list;
	}

	public void clear() {
		list.clear();
	}

	public boolean load() {
		if ( ! new File(filename).exists() ) {
			System.err.println(ERRID+"設定が読み込めませんでした、検索ワードは無効です: "+filename);
			return false;
		}

		@SuppressWarnings("unchecked")
		ArrayList<SearchWordItem> tlist = (ArrayList<SearchWordItem>) CommonUtils.readXML(filename);
		if ( tlist == null ) {
			System.err.println(ERRID+"設定の読み込みに失敗しました、検索ワードは無効です: "+filename);
			return false;
		}

		System.out.println(MSGID+"設定を読み込みました: "+filename);

		list = tlist;
		return true;
	}

	public boolean save() {
		if ( ! CommonUtils.writeXML(filename, list) ) {
			System.err.println(ERRID+"設定の保存に失敗しました： "+filename);
			return false;
		}

		return true;
	}

	/*
	 * 名称から検索キーワードを取得する
	 *
	 * @param label 名称
	 * @return 見つかった検索キーワード
	 */
	public SearchWordItem getItemFromLabel(String label){
		for (int n=0; n<list.size(); n++){
			SearchWordItem item = list.get(n);
			if (item.getLabel().equals(label)){
				return item;
			}
		}

		return null;
	}

	/*
	 * 検索回数、最終検索日時の降順にソートする
	 */
	private class SearchWordComparator implements Comparator<SearchWordItem>{
		@Override
		public int compare(SearchWordItem p1, SearchWordItem p2) {
			int rc = p2.getCount() - p1.getCount();
			if (rc != 0)
				return (rc > 0) ? 1 : -1;

			return p2.getLastUsedTime().compareTo(p1.getLastUsedTime());
		}
	}

	protected void sortList(){
		list.sort(new SearchWordComparator());
	}
}
