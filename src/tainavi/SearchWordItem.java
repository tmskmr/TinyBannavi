package tainavi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <P>ツールバーの検索キーワードを保持するクラスです。
 * @since 3.22.18β+1.10
 * @see SearchWordList
 */
public class SearchWordItem implements Cloneable {
	@Override
	public Object clone() {
		try {
			SearchWordItem swi = (SearchWordItem)super.clone();
			if (search != null)
				swi.search = (SearchKey)this.search.clone();

			return swi;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	private String keyword;
	private SearchKey search;
	private String label;
	private String from;
	private String to;
	private int count;
	private String last_used_time;

	public SearchWordItem(){
		this.keyword = null;
		this.search = null;
		this.label = null;
		this.from = null;
		this.to = null;
		this.count = 0;
		this.last_used_time = null;
	}

	public SearchWordItem(String keyword) {
		this(keyword, keyword, null, null, null);
	}

	public SearchWordItem(String label, String keyword, SearchKey search, String from, String to) {
		this.label = label;
		this.keyword = keyword;
		this.search = search;
		this.from = from;
		this.to = to;
		this.count = 1;
		this.last_used_time = CommonUtils.getDateTimeYMD(0);
	}

	/*
	 * 内容を比較する
	 *
	 * @param si 検索キーワード
	 * @param fi 日付の下限
	 * @param ti 日付の上限
	 * @return 同一の場合trueを返す
	 */
	public boolean equals(String si, SearchKey ki, String fi, String ti){
		return
			compare(si, keyword) &&
			compare(fi, from) &&
			compare(ti, to) &&
			compare(ki, search);
	}

	/*
	 * nullと空文字列を同一視して文字列を比較する
	 * @param s1 文字列１
	 * @param s2 文字列２
	 * @return 同一の場合trueを返す
	 */
	private boolean compare(String s1, String s2){
		if (s1 == null)
			s1 = "";
		if (s2 == null)
			s2 = "";

		return s1.equals(s2);
	}

	/*
	 * 検索キーを比較する
	 * @param s1 検索キー１
	 * @param s2 検索キー２
	 * @return 同一の場合trueを返す
	 */
	private boolean compare(SearchKey s1, SearchKey s2){
		return (s1 == null && s2 == null) || (s1 != null && s2 != null && s1.equals(s2));
	}

	/*
	 * 内容をHTML形式で整形する
	 *
	 * @param noTag 前後に<HTML>タグを追加しない
	 * @return 生成したテキスト
	 */
	public String formatAsHTML(boolean noTag){
		StringBuilder sb = new StringBuilder(noTag ? "" : "<HTML>");
		String f = from;
		String t = to;

		if (search != null){
			sb.append(search.formatAsHTML(true));
		}

		// 過去ログ検索
		if (keyword != null){
			String str = keyword;
			Matcher ma = Pattern.compile("^(\\d\\d\\d\\d/)?(\\d\\d/\\d\\d)([ 　]+((\\d\\d\\d\\d/)?\\d\\d/\\d\\d))?[  　]?(.*)$").matcher(str);
			if (ma.find()) {
				f = (ma.group(1) != null ? ma.group(1) : "") + ma.group(2);
				t = ma.group(4);
				str = ma.group(6);
			}

			// オプションあり
			String option = "";
			Matcher	mc = Pattern.compile("^(#title|#detail)?[  　]+(.*)$").matcher(str);
			if (mc.find()){
				if (mc.group(1).equals("#title"))
					option = "(番組名一致)";
				else if (mc.group(1).equals("#detail"))
					option = "(番組内容一致)";
				str = mc.group(2);
			}

			if (str.length() > 0 && !sb.toString().contains("キーワード:"))
				sb.append("キーワード:<B>" + str + "</B> " + option + "<BR>");
		}

		if ((f != null && !f.isEmpty()) || (t != null && !t.isEmpty())){
			sb.append("放送日:<B>" + (f != null ? f : "")  + "～" + (t != null ? t : "") + "</B><BR>");
		}

		sb.append("検索回数:" + count + "<BR>");
		sb.append("直近検索日時:" + formatLastUsedTime(last_used_time) + "<BR>");

		if (!noTag)
			sb.append("</HTML>");

		return sb.toString();
	}

	/*
	 * 最終検索日時を YYYY/MM/DD hh:mm:ss 形式に整形する
	 *
	 * @param time 最終検索日時
	 * @return 整形したテキスト
	 */
	private String formatLastUsedTime(String time){
		Matcher ma = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})").matcher(time);
		if (ma.find()){
			return ma.group(1) + "/" + ma.group(2) + "/" + ma.group(3) + " " +
					ma.group(4) + ":" + ma.group(5) + ":" + ma.group(6);
		}

		return "";
	}

	public void notifyUse(){
		this.count++;
		this.last_used_time = CommonUtils.getDateTimeYMD(0);
	}

	public void setLabel(String s){ this.label = s; }
	public String getLabel(){ return this.label != null ? this.label : this.keyword; }

	public void setKeyword(String s){ this.keyword = s; }
	public String getKeyword() { return this.keyword; }

	public void setSearchKey(SearchKey k){ this.search = k; }
	public SearchKey getSearchKey(){ return this.search; }

	public void setFrom(String from){ this.from = from; }
	public String getFrom(){ return this.from; }

	public void setTo(String to){ this.to = to; }
	public String getTo(){ return this.to; }

	public void setCount(int n){ this.count = n; }
	public int getCount(){ return this.count; }

	public void setLastUsedTime(String s){ this.last_used_time = s; }
	public String getLastUsedTime(){ return this.last_used_time; }
}
