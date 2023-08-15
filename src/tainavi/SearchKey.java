package tainavi;

import java.util.ArrayList;
import java.util.regex.Pattern;

import tainavi.TVProgram.ProgSubgenre;


public class SearchKey implements SearchItem, Cloneable {

	private String label;

	// 0:"次のすべての条件に一致"
	// 1:"次のいずれかの条件に一致"
	private String condition;

	// 0:"延長感染源にする"
	// 1:"延長感染源にしない"
	private String infection;

	public static enum TargetId {
		TITLEANDDETAIL	("0",	true,	true,	"番組名、内容に"),
		TITLE			("1",	true,	true,	"番組名に"),
		DETAIL			("2",	true,	true,	"番組内容に"),
		CHANNEL			("3",	true,	true,	"チャンネル名に"),
		GENRE			("4",	false,	true,	"ジャンルに"),
		NEW				("5",	false,	false,	"新番組"),
		LAST			("6",	false,	false,	"最終回"),
		REPEAT			("7",	false,	false,	"再放送"),
		FIRST			("8",	false,	false,	"初回放送"),
		LENGTH			("9",	false,	true,	"番組長が"),
		STARTA			("10",	false,	true,	"開始時刻(上限)が"),
		STARTZ			("11",	false,	true,	"開始時刻(下限)が"),
		SPECIAL			("12",	false,	false,	"特番"),
		NOSCRUMBLE		("13",	false,	false,	"無料放送"),
		STARTDATETIME	("14",	true,	true,	"開始日時に"),
		SUBGENRE		("15",	false,	true,	"サブジャンルに"),
		LIVE			("16",	false,	false,	"生放送"),
		BILINGUAL		("17",	false,	false,	"二か国語放送"),
		STANDIN			("18",	false,	false,	"吹替放送"),
		RATING			("19",	false,	false,	"視聴制限"),
		MULTIVOICE		("20",	false,	false,	"副音声/コメンタリ"),
		;

		private String id;
		private boolean useregexpr;
		private boolean usekeyword;
		private String name;

		private TargetId(String id, boolean useregexpr, boolean usekeyword, String name) {
			this.id = id;
			this.useregexpr = useregexpr;
			this.usekeyword = usekeyword;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		public String getId() {
			return id;
		}

		public boolean getUseRegexpr() {
			return useregexpr;
		}

		public boolean getUseKeyword() {
			return usekeyword;
		}

		public static TargetId getTargetId(String id) {
			for ( TargetId ti : TargetId.values() ) {
				if ( ti.id.equals(id) ) {
					return ti;
				}
			}
			return null;
		}
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}

	private String target;

	// s\t..:キーワード
	private String keyword;

	// 0\t..:"を含む番組"
	// 1\t..:"を含む番組を除く"
	private String contain;

	// 1:レベル1
	// 2:レベル2
	// 3:レベル3
	// 4:レベル4
	// 5:レベル5
	private String okiniiri;

	// 大小同一視無効
	private boolean caseSensitive;

	// 番組追跡表示あり
	private boolean showInStandby = true;

	// リストのソート順　列名＋1:ASC/2:DESC
	private String sortBy = null;

	// 正規表現はプリコンパイルしておくべきだ！
	ArrayList<TargetId> alTarget = new ArrayList<TargetId>();
	ArrayList<Pattern> alKeyword_regex = new ArrayList<Pattern>();
	ArrayList<String> alKeyword = new ArrayList<String>();
	ArrayList<String> alKeyword_plane = new ArrayList<String>();
	ArrayList<String> alKeyword_pop = new ArrayList<String>();
	ArrayList<String> alContain = new ArrayList<String>();
	ArrayList<Integer> alLength = new ArrayList<Integer>();

	// 検索結果のカウント
	private ArrayList<ProgDetailList> _matched = null;

	//

	public void setLabel(String s) { label = s; }
	public String getLabel() { return label; }
	public void setCondition(String s) { condition = s; }
	public String getCondition() { return condition; }

	public void setInfection(String s) { infection = s; }
	public String getInfection() { return infection; }

	public void setTarget(String s) { target = s; }
	public String getTarget() { return target; }
	public void setKeyword(String s) { keyword = s; }
	public String getKeyword() { return keyword; }
	public void setContain(String s) { contain = s; }
	public String getContain() { return contain; }

	public void setOkiniiri(String s) { okiniiri = s; }
	public String getOkiniiri() { return okiniiri; }

	public void setCaseSensitive(boolean b) { caseSensitive = b; }
	public boolean getCaseSensitive() { return caseSensitive; }

	public void setShowInStandby(boolean b) { showInStandby = b; }
	public boolean getShowInStandby() { return showInStandby; }

	public void setSortBy(String s){ sortBy = s; }
	public String getSortBy(){ return sortBy; }

	// interface

	@Override
	public String toString() { return label; }

	@Override
	public void clearMatchedList() { _matched = new ArrayList<ProgDetailList>(); }
	@Override
	public void addMatchedList(ProgDetailList pdl) { _matched.add(pdl); }
	@Override
	public ArrayList<ProgDetailList> getMatchedList() { return _matched; }
	@Override
	public boolean isMatched() { return _matched != null && _matched.size() != 0; }
	@Override
	public boolean equals(Object o){
		SearchKey k = (SearchKey)o;

		return compare(k.getTarget(), getTarget()) &&
				compare(k.getKeyword(), getKeyword()) &&
				compare(k.getContain(), getContain()) &&
				compare(k.getInfection(), getInfection()) &&
				k.getCaseSensitive() == getCaseSensitive() &&
				k.getShowInStandby() == getShowInStandby();
	}

	/*
	 * nullと空文字列を同一視して文字列を比較する
	 *
	 * @param s1 文字列１
	 * @param s2 文字列２
	 * @return 同一の場合true
	 */
	private boolean compare(String s1, String s2){
		if (s1 == null)
			s1 = "";
		if (s2 == null)
			s2 = "";

		return s1.equals(s2);
	}

	/*
	 * 内容をHTML形式に整形する
	 *
	 * @param noTag 前後に<HTML>を付けない
	 * @return 整形したテキスト
	 */
	public String formatAsHTML(boolean noTag){
		if (getTarget() == null)
			return "";

		StringBuilder sb = new StringBuilder(noTag ? "" : "<HTML");

		String [] ts = getTarget().split("\t");
		String [] rs = getKeyword().split("\t");
		String [] cs = getContain().split("\t");

		StringBuilder fsb = new StringBuilder("");
		String from = null;
		String to = null;

		for (int n=0; n<ts.length; n++){
			switch(ts[n]){
			case "0":			// TITLEANDDETAIL	("0",	true,	true,	"番組名、内容に"),
				sb.append("キーワード:<B>" + rs[n] + "</B><BR>");
				break;
			case "1":			// TITLE			("1",	true,	true,	"番組名に"),
				sb.append("キーワード:<B>" + rs[n] + "</B> (番組名一致)<BR>");
				break;
			case "2":			// DETAIL			("2",	true,	true,	"番組内容に"),
				sb.append("キーワード:<B>" + rs[n] + "</B> (番組内容一致)<BR>");
				break;
			case "3":			// CHANNEL			("3",	true,	true,	"チャンネル名に"),
				sb.append("チャンネル名:<B>" + rs[n] + "</B><BR>");
				break;
			case "4":			// GENRE			("4",	false,	true,	"ジャンルに"),
				sb.append("ジャンル:<B>" + rs[n] + "</B><BR>");
				break;
			case "5":			// NEW				("5",	false,	false,	"新番組"),
				fsb.append("新");
				break;
			case "6":			// LAST			("6",	false,	false,	"最終回"),
				fsb.append("終");
				break;
			case "7":			// REPEAT			("7",	false,	false,	"再放送"),
				fsb.append("再");
				break;
			case "8":			// FIRST			("8",	false,	false,	"初回放送"),
				fsb.append("初");
				break;
			case "9":			// LENGTH			("9",	false,	true,	"番組長が"),
				if (cs[n].equals("0"))
					from = rs[n].trim();
				else if (cs[n].equals("1")){
					try{
						to = String.valueOf(Integer.parseInt(rs[n].trim())-1);
					}catch(NumberFormatException e){
						to = rs[n].trim();
					}
				}
				break;
			// STARTA			("10",	false,	true,	"開始時刻(上限)が"),
			// STARTZ			("11",	false,	true,	"開始時刻(下限)が"),
			case "12":			// SPECIAL			("12",	false,	false,	"特番"),
				fsb.append("特");
				break;
			case "15":			// SUBGENRE		("15",	false,	true,	"サブジャンルに"),
				ProgSubgenre subgenre = ProgSubgenre.get(rs[n]);
				sb.append("サブジャンル:<B>" + (subgenre != null ? subgenre.toString() : "") + "</B><BR>");
				break;
			}
		}

		if (from != null || to != null){
			sb.append("番組長:<B>" + (from != null ? from + "分" : "") + "～" + (to != null ? to + "分" : "") +  "</B><BR>");
		}
		if (fsb.length() > 0){
			sb.append("フラグ:<B>" + fsb.toString() + "</B><BR>");
		}

		if (!noTag)
			sb.append("</HTML>");

		return sb.toString();
	}
}
