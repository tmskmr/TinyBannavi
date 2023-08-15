package tainavi.plugintv;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.Center;
import tainavi.CommonUtils;
import tainavi.ContentIdSyobo;
import tainavi.ProgDateList;
import tainavi.ProgDetailList;
import tainavi.ProgList;
import tainavi.TVProgram;
import tainavi.TVProgramUtils;
import tainavi.TraceProgram;

/**
 * しょぼかるから番組表と放送局リストを取得する。プラグインではないよ。
 */
public class Syobocal extends TVProgramUtils implements TVProgram,Cloneable {
	private final String thisEncoding = "UTF-8";

	public void setDebug(boolean b) { debug = b; }

	private boolean debug = false;

	private boolean rss2 = false;

	/* 必須コード  - ここから */

	// 種族の特性
	private static final String tvProgId = "Syobocal";

	private static int pastDays = 0;

	public static void setPastDays(int n){ pastDays = n; }

	//private final String progCacheFile = getProgDir()+File.separator+"syobocal.xml";
	private final String centerFile = "env"+File.separator+"center."+getTVProgramId()+".xml";

	private final String MSGID = "[しょぼかる] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	@Override
	public String getTVProgramId() { return tvProgId; }

	@Override
	public boolean isAreaSelectSupported() { return false; }

	@Override
	public ProgType getType() { return ProgType.SYOBO; }
	public ProgSubtype getSubtype() { return ProgSubtype.NONE; }

	@Override
	public Syobocal clone() {
		return (Syobocal) super.clone();
	}

	// 個体の特性

	//
	@Override
	public int getTimeBarStart() {return 5;}

	private int getDogDays() { return ((getExpandTo8())?(8):(7)); }

	//
	@Override
	public boolean loadProgram(String areaCode, boolean force) {

		String progCacheFile = null;
		if ( rss2 ) {
			progCacheFile = getProgDir()+File.separator+"syobocal.xml";
		}
		else {
			progCacheFile = getProgDir()+File.separator+"syobocal.rss";
		}

		// 新しい番組データの入れ物を作る
		ArrayList<ProgList> newplist = new ArrayList<ProgList>();

		int cnt = 0;

		try {
			String response = null;

			String cirtDateTimeYMD = CommonUtils.getCritDateTime().replaceAll("[/: ]", "");

			File f = new File(progCacheFile);
			if (force == true ||
					(f.exists() == true && isCacheOld(progCacheFile) == true) ||
					(f.exists() == false && isCacheOld(null) == true)) {

				String url = null;
				if ( rss2 ) {
					url = "http://cal.syoboi.jp/rss2.php?start="+cirtDateTimeYMD+"&days="+getDogDays()+"&titlefmt=$(Flag)^^^$(FlagW)^^^$(Cat)^^^$(ChName)^^^$(EdTime)^^^$(Title)^^^$(SubTitleB)";
				}
				else {
//					url = "http://cal.syoboi.jp/rss.php?start=today&count=1500&days=8&days="+getDogDays()+"&titlefmt=$(Flag)^^^$(FlagW)^^^$(Cat)^^^$(ChName)^^^$(EdTime)^^^$(Title)^^^$(SubTitleB)";

					GregorianCalendar c = new GregorianCalendar();
					if ( CommonUtils.isLateNight(c.get(Calendar.HOUR_OF_DAY)) ) {
						c.add(Calendar.DAY_OF_MONTH, -1);
					}
					c.add(Calendar.DAY_OF_MONTH, -pastDays);

					String start = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
					int days = getDogDays() + pastDays;

					url = "http://cal.syoboi.jp/rss.php?start="+start+"&count=1500&days="+days+"&titlefmt=$(Flag)^^^$(FlagW)^^^$(Cat)^^^$(ChName)^^^$(EdTime)^^^$(Title)^^^$(SubTitleB)";
				}
				response = webToBuffer(url, thisEncoding, true);
				if ( response == null ) {
					reportProgress(ERRID+"RSS2.0(オンライン)の取得に失敗しました: "+url);
					return false;
				}

				reportProgress(MSGID+"RSS2.0(オンライン)を取得しました: "+url);
				CommonUtils.write2file(progCacheFile, response);
			}
			else if (f.exists()) {
				//
				response = CommonUtils.read4file(progCacheFile, true);
				if ( response == null ) {
					reportProgress(ERRID+"RSS2.0(キャッシュ)の取得に失敗しました: "+progCacheFile);
					return false;
				}
				reportProgress(MSGID+"RSS2.0(キャッシュ)を取得しました: "+progCacheFile);
			}
			else {
				reportProgress(ERRID+"RSS2.0(キャッシュ)がみつかりません: "+progCacheFile);
				return false;
			}

			// 情報解析

			Matcher ma = Pattern.compile("<item(.+?)</item>",Pattern.DOTALL).matcher(response);
			while (ma.find()) {

				// 入れ物
				ProgDetailList pDetail = new ProgDetailList();

				// <title>金曜ロードショー ヱヴァンゲリヲン新劇場版：破 TV版</title>
				Matcher mb = Pattern.compile("<title>(.+?)</title>",Pattern.DOTALL).matcher(ma.group(1));
				if ( ! mb.find()) {
					continue;
				}

				String[] t = mb.group(1).split("\\^\\^\\^",7);

				if (t.length < 7) {
					System.err.println(ERRID+"書式が不正： "+mb.group(1));
				}

				pDetail.title = CommonUtils.unEscape(t[5]);

				pDetail.detail = CommonUtils.unEscape(t[6]);
				if ( pDetail.detail.matches("^#1$|^#1[^0-9].*$") ) {
					// まあなにもしなくていいか
				}

				if (t[0] != null && t[0].length() > 0) {
					int flag = Integer.valueOf(t[0]);
					if (flag != 0) {
						if ((flag & 0x01) != 0x00) {
							pDetail.addOption(ProgOption.SPECIAL);
							flag ^= 0x01;
						}
						if ((flag & 0x02) != 0x00) {
							pDetail.flag = ProgFlags.NEW;
							flag ^= 0x02;
						}
						if ((flag & 0x04) != 0x00) {
							pDetail.flag = ProgFlags.LAST;
							flag ^= 0x04;
						}
						if ((flag & 0x08) != 0x00) {
							pDetail.addOption(ProgOption.REPEAT);
							flag ^= 0x08;
						}
						if (flag != 0) {
							System.out.println(DBGID+"未対応のマーク： "+flag);
						}
					}
				}
				if (t[1] != null && t[1].length() > 0) {
					int flagw = Integer.valueOf(t[1]);
					if (flagw != 0) {
						if ((flagw & 0x01) != 0x00) {
							if (pDetail.flag != ProgFlags.NEW && ! pDetail.isOptionEnabled(ProgOption.SPECIAL)) {
								pDetail.addOption(ProgOption.MOVED);	// 新番組や特番なら(移)はいらんやろ
							}
							flagw ^= 0x01;
						}
						if (flagw != 0) {
							System.out.println(DBGID+"未対応の警告フラグ： "+flagw);
						}
					}
				}
				//  <tv:genre>アニメ(終了/再放送)</tv:genre>
				pDetail.genre = null;
				pDetail.subgenre = null;
				pDetail.genrelist = new ArrayList<TVProgram.ProgGenre>();
				pDetail.subgenrelist = new ArrayList<TVProgram.ProgSubgenre>();
				boolean anime_etc = true;

				if (t[2] != null && t[2].length() > 0) {
					// ジャンル
					if (t[2].equals("1") || t[2].equals("10")  || t[2].equals("5")) {
						// 1:アニメ 10:アニメ(終了/再放送) 5:アニメ関連
					}
					else if (t[2].equals("7")) {
						// 7:OVA
						pDetail.genre = ProgGenre.ANIME;
						pDetail.subgenre = ProgSubgenre.ANIME_KOKUNAI;
						pDetail.genrelist.add(ProgGenre.ANIME);
						pDetail.subgenrelist.add(ProgSubgenre.ANIME_KOKUNAI);
						anime_etc = false;
					}
					else if (t[2].equals("4")) {
						// 4:特撮
						pDetail.genre = ProgGenre.ANIME;
						pDetail.subgenre = ProgSubgenre.ANIME_TOKUSATSU;
						pDetail.genrelist.add(ProgGenre.ANIME);
						pDetail.subgenrelist.add(ProgSubgenre.ANIME_TOKUSATSU);
						anime_etc = false;
					}
					else if (t[2].equals("8")) {
						// 8:映画
						pDetail.genre = ProgGenre.MOVIE;
						pDetail.subgenre = ProgSubgenre.MOVIE_ETC;
						pDetail.genrelist.add(ProgGenre.MOVIE);
						pDetail.subgenrelist.add(ProgSubgenre.MOVIE_ETC);
						pDetail.removeOption(ProgOption.MOVED);	// 映画なら(移)はいらんやろ
					}
					else if (t[2].equals("0")) {
						// 0:その他
						pDetail.genrelist.add(ProgGenre.NOGENRE);
						pDetail.subgenrelist.add(ProgSubgenre.NOGENRE_ETC);
					}
					else if (t[2].equals("3") || t[2].equals("2") || t[2].equals("6")) {
						// 3:テレビ 2:ラジオ 6:メモ
					}
					else {
						System.out.println(DBGID+"未対応のジャンル： "+t[2]);
					}

					// 最後に
					if (pDetail.genre == null) {
						pDetail.genre = ProgGenre.ANIME;
						pDetail.subgenre = ProgSubgenre.ANIME_ETC;
					}
					if (pDetail.genrelist.size() == 0 || anime_etc) {
						pDetail.genrelist.add(ProgGenre.ANIME);
						pDetail.subgenrelist.add(ProgSubgenre.ANIME_ETC);
					}
				}

				// <dc:publisher>日本テレビ</dc:publisher>
				if ( t[3] == null || t[3].length() == 0) {
					System.err.println(ERRID+"放送局名がない： "+mb.group(1));
					continue;
				}

				// ベアな放送局名と、ChannelConvert.datを適用した結果の放送局名
				String location = CommonUtils.unEscape(t[3]);
				String modifiedloc = getChName(location);

				// <tv:startDatetime>2011-08-29T00:00:00+09:00</tv:startDatetime>
				if ( rss2 ) {
					mb = Pattern.compile("<pubDate>(.+?)\\+09:00</pubDate>",Pattern.DOTALL).matcher(ma.group(1));
				}
				else {
					mb = Pattern.compile("<tv:startDatetime>(.+?)\\+09:00</tv:startDatetime>",Pattern.DOTALL).matcher(ma.group(1));
				}
				if ( ! mb.find()) {
					System.err.println(ERRID+"開始日時がない");
					continue;
				}
				GregorianCalendar ca = CommonUtils.getCalendar(mb.group(1));
				if ( ca == null ) {
					System.err.println(ERRID+"開始日時が不正： "+mb.group(1));
					continue;
				}

				pDetail.startDateTime = CommonUtils.getDateTime(ca);
				pDetail.start = pDetail.startDateTime.substring(11, 16);
				pDetail.accurateDate = CommonUtils.getDate(ca);

				if ( t[4] == null || t[4].length() == 0) {
					System.err.println(ERRID+"終了時刻がない： "+mb.group(1));
					continue;
				}

				GregorianCalendar cz = CommonUtils.getCalendar(CommonUtils.getDate(ca)+" "+t[4]);
				if (ca.compareTo(cz) > 0) {
					cz.add(Calendar.DAY_OF_MONTH, 1);
				}

				pDetail.endDateTime = CommonUtils.getDateTime(cz);
				pDetail.end = pDetail.endDateTime.substring(11, 16);

				// 24:00～28:59までは前日なんだニャー
				if (CommonUtils.isLateNight(ca.get(Calendar.HOUR_OF_DAY))) {
					ca.add(Calendar.DAY_OF_MONTH, -1);
				}

				// 番組情報を入れるべき日付
				String progdate = CommonUtils.getDate(ca);

				// <description>HD放送</description>
				mb = Pattern.compile("<description>(.+?)</description>").matcher(ma.group(1));
				if (mb.find()) {
					pDetail.detail += " <" + CommonUtils.unEscape(mb.group(1)) + ">";
					if (pDetail.detail.contains("無料放送")) {
						pDetail.noscrumble = ProgScrumble.NOSCRUMBLE;
					}
					if (pDetail.detail.contains("先行放送")) {
						pDetail.addOption(ProgOption.PRECEDING);
					}
					if (pDetail.detail.contains("変更の可能性")) {
						pDetail.extension = true;
					}
					if ( pDetail.detail.contains("繰り下げ") ) {
						pDetail.extension = true;
					}
					if (pDetail.detail.contains("副音声")) {
						pDetail.addOption(ProgOption.MULTIVOICE);
					}
				}

				// <link>http://cal.syoboi.jp/tid/44#198593</link>
				mb = Pattern.compile("<link>(.+?)</link>",Pattern.DOTALL).matcher(ma.group(1));
				if (mb.find()) {
					pDetail.link = mb.group(1);
					if ( ! ContentIdSyobo.isValid(pDetail.link)) {
						System.out.println(DBGID+"TIDとPIDが取得できない： "+pDetail.link);
					}
				}

				// 追加詳細
				pDetail.setGenreStr();

				// 統合
				{
					// 放送局が存在するか
					ProgList prog = null;
					for (ProgList pl : newplist) {
						if (pl.Center.equals(modifiedloc)) {
							prog = pl;
							break;
						}
					}
					if (prog == null) {
						// 番組表
						prog = new ProgList();
						prog.Center = modifiedloc;
						prog.enabled = true;
						prog.pdate = new ArrayList<ProgDateList>();

						newplist.add(prog);
					}

					// 日付が存在するか
					ProgDateList pCenter = null;
					for (ProgDateList pcl : prog.pdate) {
						if (pcl.Date.equals(progdate)) {
							pCenter = pcl;
							break;
						}
					}
					if (pCenter == null) {
						pCenter = new ProgDateList();
						pCenter.Date = progdate;
						pCenter.pdetail = new ArrayList<ProgDetailList>();
						prog.pdate.add(pCenter);
					}

					// 連結
					pCenter.pdetail.add(pDetail);

					cnt++;
				}

				//
				pDetail.splitted_title = pDetail.title;
				pDetail.splitted_detail = pDetail.detail;

				// 詳細を登録する
				pDetail.titlePop = TraceProgram.replacePop(pDetail.title);
				pDetail.splitted_titlePop = pDetail.titlePop;
				pDetail.detailPop = TraceProgram.replacePop(pDetail.detail);
				pDetail.length = Integer.valueOf(CommonUtils.getRecMin(pDetail.start.substring(0,2),pDetail.start.substring(3,5),pDetail.end.substring(0,2),pDetail.end.substring(3,5)));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		pcenter = newplist;
		System.out.println(DBGID+"番組の数： "+cnt);
		return true;
	}

	/* ここまで */

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	@Override
	public void loadAreaCode() {
	}

	@Override
	public void saveAreaCode() {
	}

	@Override
	public String getCode(String area) {
		return "1";
	}

	@Override
	public String getDefaultArea() {
		return "しょぼかる";
	}

	@Override
	public String getSelectedArea() {
		return "しょぼかる";
	}

	@Override
	public String getSelectedCode() {
		return "1";
	}

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	@Override
	public void loadCenter(String code, boolean force) {

		if ( code == null ) {
			System.out.println(ERRID+"地域コードがnullです.");
			return;
		}

		if ( ! force && new File(centerFile).exists() ) {
			// NOT FORFCEならキャッシュからどうぞ
			@SuppressWarnings("unchecked")
			ArrayList<Center> tmp = (ArrayList<Center>) CommonUtils.readXML(centerFile);
			if ( tmp != null ) {
				crlist = tmp;
				return;
			}
		}

		String uri = "http://cal.syoboi.jp/mng?Action=ShowChList";
		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.err.println(ERRID+"放送局リストの取得に失敗： "+uri);
			return;
		}
		System.out.println(MSGID+"放送局リストを取得： "+uri);

		Matcher ma = Pattern.compile("<table class=\"tframe output\".*?>(.+?)</table>",Pattern.DOTALL).matcher(response);
		if ( ! ma.find() ) {
			System.err.println(ERRID+"放送局情報がない： "+uri);
			return;
		}

		// 新しい放送局リストの入れ物を作る
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		int cnt = 1;
		Matcher mb = Pattern.compile("<tr>(.+?)</tr>",Pattern.DOTALL).matcher(ma.group(1));
		while ( mb.find() ) {
			String[] d = mb.group(1).split("<.*?>",9);
			if ( d.length != 9 ) {
				System.err.println(ERRID+"書式不正（カラム数が足りない）： "+d.length);
				continue;
			}
			if ( ! d[1].matches("^\\d+$") ) {
				continue;
			}

			// 放送局リスト
			Center cr = new Center();
			cr.setLink(d[5]);
			cr.setAreaCode("1");
			cr.setCenterOrig(CommonUtils.unEscape(d[7]));
			//cr.setCenter(this.chconv.get(cr.getCenterOrig()));	// ChannelConvert.datで入れ替えたもの
			cr.setType("");
			cr.setEnabled(true);
			cr.setOrder(cnt++);

			newcrlist.add(cr);

			if (debug) System.out.println(MSGID+"放送局を追加： "+cr.getCenterOrig()+"  ->  "+cr.getCenter());
		}

		if ( newcrlist.size() == 0 ) {
			System.err.println(ERRID+"放送局情報の取得結果が０件だったため情報を更新しません");
			return;
		}

		System.out.println(DBGID+"放送局の数： "+newcrlist.size());

		crlist = newcrlist;
		attachChFilters();
		setSortedCRlist();
		CommonUtils.writeXML(centerFile, crlist);
	}

	@Override
	public boolean saveCenter() {
		return false;
	}

	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */

	/**
	 * 日付変更線(29:00)をまたいだら過去のデータはカットする
	 * <B> PassedProgramでは使わない
	 */
	@Override
	public void refresh() {

		String critDate = CommonUtils.getDate529(-60*60*24*pastDays, true);
		for ( ProgList p : pcenter ) {
			int i = 0;
			for ( ProgDateList c : p.pdate ) {
				if ( c.Date.compareTo(critDate) >= 0 ) {
					break;
				}
				i++;
			}
			for ( int j=0; j<i; j++) {
				p.pdate.remove(0);
			}
		}
	}

}
