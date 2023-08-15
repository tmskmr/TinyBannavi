package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.SearchKey.TargetId;
import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgScrumble;
import tainavi.TVProgram.ProgSubgenre;


public class SearchProgram {

	//
	private ArrayList<SearchKey> searchKeys = new ArrayList<SearchKey>();

	private String searchKeyFile = null;
	protected void setSearchKeyFile(String s) { searchKeyFile = s; }

	private String searchKeyLabel = null;
	protected void setSearchKeyLabel(String s) { searchKeyLabel = s; }

	// 設定ファイルに書き出し
	public boolean save() {
		System.out.println(searchKeyLabel+"設定を保存します: "+searchKeyFile);
		if ( ! CommonUtils.writeXML(searchKeyFile, searchKeys) ) {
			System.err.println(searchKeyLabel+"設定の保存に失敗しました.");
			return false;
		}
		return true;
	}

	// 設定ファイルから読み出し
	@SuppressWarnings("unchecked")
	public void load() {
		System.out.println(searchKeyLabel+"設定を読み込みます: "+searchKeyFile);
		searchKeys = (ArrayList<SearchKey>) CommonUtils.readXML(searchKeyFile);
		if ( searchKeys == null ) {
			System.err.println(searchKeyLabel+"設定を読み込めなかったので登録なしで起動します.");
			searchKeys = new ArrayList<SearchKey>();
			return;
		}

		// もういらないのでは…
		for (SearchKey sk : searchKeys) {
			// 後方互換用・その１
			compile(sk);
			// 後方互換用・その２
			if (sk.getOkiniiri() == null) {
				sk.setOkiniiri("★");
			}
		}
	}

	// 検索用
	public ArrayList<SearchKey> getSearchKeys() {
		return(searchKeys);
	}

	// キーワード検索の追加
	public void add(SearchKey newkey) {
		searchKeys.add(newkey);
	}

	public void add(int index, SearchKey newkey) {
		searchKeys.add(index, newkey);
	}

	// キーワード検索の削除
	public void remove(String key) {
		for ( SearchKey k : searchKeys ) {
			if (k.getLabel().equals(key)) {
				searchKeys.remove(k);
				break;
			}
		}
	}

	// キーワード検索の置き換え
	public SearchKey replace(SearchKey xk, SearchKey sk) {
		int index = searchKeys.indexOf(xk);
		if ( index >= 0 ) {
			searchKeys.remove(xk);
			searchKeys.add(index,sk);
			return sk;
		}
		return null ;
	}

	// キーワード検索の整理
	public void compile(SearchKey keyword) {
		if (keyword == null || keyword.getTarget() == null)
			return;

		//
		keyword.alTarget = new ArrayList<TargetId>();
		keyword.alKeyword_regex = new ArrayList<Pattern>();
		keyword.alKeyword = new ArrayList<String>();
		keyword.alContain = new ArrayList<String>();

		//
		ArrayList<String> sTtmp = new ArrayList<String>();
		ArrayList<String> mRtmp = new ArrayList<String>();
		ArrayList<String> sCtmp = new ArrayList<String>();

		Matcher ma = null;

		//
		ma = Pattern.compile("(.*?)\t").matcher(keyword.getTarget());
		while (ma.find()) {
			sTtmp.add(String.valueOf(ma.group(1)));
		}
		//
		ma = Pattern.compile("(.*?)\t").matcher(keyword.getKeyword());
		while (ma.find()) {
			mRtmp.add(ma.group(1));
		}
		//
		ma = Pattern.compile("(.*?)\t").matcher(keyword.getContain());
		while (ma.find()) {
			sCtmp.add(String.valueOf(ma.group(1)));
		}

		// 「含む」を前に、「除く」を後に
		for (int x=0; x<sTtmp.size(); x++) {
			if (sCtmp.get(x).equals("0")) {
				setAlKeyword(keyword, x, sTtmp.get(x), mRtmp.get(x), sCtmp.get(x));
			}
		}
		for (int x=0; x<sTtmp.size(); x++) {
			if (sCtmp.get(x).equals("1")) {
				setAlKeyword(keyword, x, sTtmp.get(x), mRtmp.get(x), sCtmp.get(x));
			}
		}
	}
	private void setAlKeyword(SearchKey keyword, int x, String sT, String mR, String sC) {

		TargetId ti = TargetId.getTargetId(sT);
		keyword.alTarget.add(ti);

		// 正規表現なターゲット
		switch (ti) {
		case TITLEANDDETAIL:
		case TITLE:
		case DETAIL:
		case CHANNEL:
			if (keyword.getCaseSensitive() == false) {
				keyword.alKeyword_regex.add(Pattern.compile("("+TraceProgram.replacePop(mR)+")"));
			}
			else {
				keyword.alKeyword_regex.add(Pattern.compile("("+mR+")"));
			}
			break;
		case STARTDATETIME:
			keyword.alKeyword_regex.add(Pattern.compile("("+mR+")"));
			break;
		default:
			keyword.alKeyword_regex.add(null);
			break;
		}

		// 数値なターゲット
		switch (ti) {
		case LENGTH:
			Matcher ma = Pattern.compile("^(\\d+) ").matcher(mR);
			if (ma.find()) {
				keyword.alLength.add(Integer.valueOf(ma.group(1)));
			}
			else {
				keyword.alLength.add(0);
			}
			break;
		default:
			keyword.alLength.add(0);
			break;
		}

		// 時刻なターゲット
		switch (ti) {
		case STARTA:
		case STARTZ:
			Matcher ma = Pattern.compile("^(\\d\\d)(:\\d\\d)").matcher(mR);
			if (ma.find()) {
				Integer t = Integer.valueOf(ma.group(1));
				t += (CommonUtils.isLateNight(t)?(24):(0));
				keyword.alKeyword_plane.add(String.format("%02d%s", t, ma.group(2)));
				//System.out.println(keyword.alKeyword_plane.get(keyword.alKeyword_plane.size()-1));
			}
			else {
				keyword.alKeyword_plane.add("");
			}
			break;
		default:
			keyword.alKeyword_plane.add(mR);
			break;
		}

		keyword.alKeyword.add(TraceProgram.replacePop(mR));
		keyword.alKeyword_pop.add(TraceProgram.replacePop(mR));
		keyword.alContain.add(sC);
	}



	//
	private static int compareDT(String target, String limit)
	{
		Matcher ma = Pattern.compile("^(\\d\\d)(:\\d\\d)").matcher(target);
		if (ma.find()) {
			Integer t = Integer.valueOf(ma.group(1));
			t += (CommonUtils.isLateNight(t)?(24):(0));
			String ts = String.format("%02d%s", t, ma.group(2));
			return ts.compareTo(limit);
		}
		else {
			return 0;
		}
	}

	private static String matchedString = null;
	public static String getMatchedString() { return matchedString; }

	public static boolean isMatchKeyword(SearchKey keyword, String center, ProgDetailList tvd)
	{
		//
		ArrayList<TargetId> sT = keyword.alTarget;
		ArrayList<Pattern> mRe = keyword.alKeyword_regex;
		//ArrayList<String> mR = keyword.alKeyword;
		ArrayList<String> mRp = keyword.alKeyword_plane;
		//ArrayList<String> mRpop = keyword.alKeyword_pop;
		ArrayList<String> sC = keyword.alContain;
		ArrayList<Integer> sL = keyword.alLength;

		matchedString = null;

		//
		Matcher mx = null;
		boolean isOrMatch = false;
		for (int x=0; x<sT.size(); x++) {
			//
			boolean isCurMatch = false;
			TargetId ti = sT.get(x);

			// 0:"番組名、内容に"
			// 1:"番組名に"
			if (ti == TargetId.TITLE || ti == TargetId.TITLEANDDETAIL) {
				if (keyword.getCaseSensitive() == false) {
					mx = mRe.get(x).matcher(tvd.titlePop);
					if (mx.find()) {
						isCurMatch = true;
						if (matchedString == null) {
							int srcLen = tvd.title.length();
							int keyLen = mx.group(1).length();
							int keyIdx = tvd.titlePop.indexOf(mx.group(1));
							char[] ch = tvd.title.toCharArray();
							int cnt = 0;
							int matchIdx = -1;
							int matchLen = 0;
							for (int i=0; i<srcLen; i++) {
								if ( ! TraceProgram.isOmittedChar(ch[i])) {
									cnt++;
								}
								if (matchIdx < 0 && keyIdx == cnt-1) {
									matchIdx = i;
									cnt = 0;
								}
								if (matchIdx >= 0 && cnt < keyLen) {
									matchLen++;
								}
							}
							matchedString = tvd.title.substring(matchIdx,matchIdx+matchLen);
						}
					}
				}
				else {
					if (tvd.title.indexOf(mRp.get(x)) >= 0) {
						isCurMatch = true;
						if (matchedString == null) {
							matchedString = mRp.get(x);
						}
					}
				}
			}

			// 0:"番組名、内容に"
			// 2:"番組内容に"
			if (ti == TargetId.DETAIL || ti == TargetId.TITLEANDDETAIL) {
				if (keyword.getCaseSensitive() == false) {
					mx = mRe.get(x).matcher(tvd.detailPop);
					if (mx.find()) {
						isCurMatch = true;
					}
				}
				else {
					if (tvd.detail.indexOf(mRp.get(x)) >= 0) {
						isCurMatch = true;
					}
				}
			}

			switch ( ti ) {
			case CHANNEL:	// 3
				if (center == null)
					center = "";
				if (keyword.getCaseSensitive() == false) {
					mx = mRe.get(x).matcher(TraceProgram.replacePop(center));
					if (mx.find()) {
						isCurMatch = true;
					}
				}
				else {
					if (center.indexOf(mRp.get(x)) >= 0) {
						isCurMatch = true;
					}
				}
				break;

			case GENRE:	//4,15
				ProgGenre gr = ProgGenre.get(mRp.get(x));
				if ( gr != null ) {
					isCurMatch = tvd.isEqualsGenre(gr, null);
				}
				break;
			case SUBGENRE:
				ProgSubgenre sgr = ProgSubgenre.get(mRp.get(x));
				if ( sgr != null ) {
					isCurMatch = tvd.isEqualsGenre(null, sgr);
				}
				break;

			case NEW:	// 5,6,7,8,12,13
				if (tvd.flag == ProgFlags.NEW) {
					isCurMatch = true;
				}
				break;
			case LAST:
				if (tvd.flag == ProgFlags.LAST) {
					isCurMatch = true;
				}
				break;
			case REPEAT:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.REPEAT) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case FIRST:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.FIRST) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case SPECIAL:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.SPECIAL) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case RATING:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.RATING) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case NOSCRUMBLE:
				if (tvd.noscrumble == ProgScrumble.NOSCRUMBLE) {
					isCurMatch = true;
				}
				break;
			case LIVE:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.LIVE) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case BILINGUAL:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.BILINGUAL) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case MULTIVOICE:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.MULTIVOICE) {
						isCurMatch = true;
						break;
					}
				}
				break;
			case STANDIN:
				for (ProgOption o : tvd.getOption()) {
					if (o == ProgOption.STANDIN) {
						isCurMatch = true;
						break;
					}
				}
				break;

			case LENGTH:	// 9
				if (tvd.length >= sL.get(x)) {
					isCurMatch = true;
				}
				break;

			case STARTA:	// 10,11,14
				if (compareDT(tvd.start,mRp.get(x)) >= 0) {
					isCurMatch = true;
				}
				break;
			case STARTZ:
				if (compareDT(tvd.start,mRp.get(x)) <= 0) {
					isCurMatch = true;
				}
				break;
			case STARTDATETIME:
				mx = mRe.get(x).matcher(tvd.startDateTime);
				if (mx.find()) {
					isCurMatch = true;
				}
				break;

			default:
				break;
			}


			//
			if (keyword.getCondition().equals("0")) {
				// 0:次のすべての条件に一致
				if (sC.get(x).equals("0") && isCurMatch == false) {
					// 0:"を含む番組"
					return(false);
				}
				else if (sC.get(x).equals("1") && isCurMatch == true) {
					// 1:"を含む番組を除く"
					return(false);
				}
			}
			else {
				// 1:次のいずれかの条件に一致
				if (sC.get(x).equals("0") && isCurMatch == true) {
					// 0:"を含む番組"
					isOrMatch = true;
				}
				else if (sC.get(x).equals("1") && isCurMatch == true) {
					// 1:"を含む番組を除く"

					// ★★★ 特殊動作注意！番ナビスレ Part.11 No.274付近参照 ★★★
					// 「含む ∪ 含む ∪ 含まない ∪ 含まない」ではなく
					// 「( 含む ∪ 含む ) ∩ ～（含まない ∪ 含まない）」となる

					return(false);
				}
			}
		}

		if (keyword.getCondition().equals("0")) {
			// 0:次のすべての条件に一致で、すべての条件に適合した場合
			return(true);
		}
		else {
			// 1:次のいずれかの条件に一致、の場合
			return(isOrMatch);
		}
	}



	// コンストラクタ
	public SearchProgram() {
		setSearchKeyFile("env"+File.separator+"keyword.xml");
		setSearchKeyLabel("キーワード検索");
	}
}
