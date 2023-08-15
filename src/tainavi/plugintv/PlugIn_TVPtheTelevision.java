package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.AreaCode;
import tainavi.Center;
import tainavi.CommonUtils;
import tainavi.ContentIdDIMORA;
import tainavi.ProgDateList;
import tainavi.ProgDetailList;
import tainavi.ProgList;
import tainavi.TVProgram;
import tainavi.TVProgramUtils;


public class PlugIn_TVPtheTelevision extends TVProgramUtils implements TVProgram,Cloneable {

	public PlugIn_TVPtheTelevision clone() {
		return (PlugIn_TVPtheTelevision) super.clone();
	}

	private static final String thisEncoding = "UTF-8";


	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return "webザテレビジョン"; }

	@Override
	public ProgType getType() { return ProgType.PROG; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.TERRA; }


	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	@Override
	public int getTimeBarStart() {return 5;}

	//private int getDogDays() { return 7; }


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 新しい入れ物の臨時格納場所
	protected ArrayList<ProgList> newplist = new ArrayList<ProgList>();

	// 未定義のフラグの回収場所
	private final HashMap<String,String> nf = new HashMap<String, String>();


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/


	/*******************************************************************************
	 * 番組情報を取得する
	 ******************************************************************************/

	public boolean loadProgram(String areaCode, boolean force) {
		setCacheFileOnly(true);

		// 入れ物を空にする
		newplist.clear();
		nf.clear();

		//
		int counterMax = getSortedCRlist().size();
		int counter=1;
		for ( Center c : getSortedCRlist() ) {
			_loadProgram(c, force, counter++, counterMax);
		}

		// 古い番組データを置き換える
		pcenter = newplist;
		return true;
	}

	/* ここまで */



	/*
	 * 非公開メソッド
	 */

	protected void _loadProgram(Center cr, boolean force, int counter, int counterMax) {
		//
		try {
			//　progfilesの読み出し

			// 局リストの追加
			ProgList pl = new ProgList();
			pl.Center = cr.getCenter();
			pl.CenterId = cr.getLink();
			pl.Area = cr.getAreaCode();
			pl.Type = cr.getType();
			pl.BgColor = cr.getBgColor();
			pl.enabled = true;
			newplist.add(pl);

			final String progCacheFile = String.format(getProgDir()+File.separator+"TVTheTV_%s_%s.html", pl.Area, pl.CenterId);
			String response = null;
			//
			File f = new File(progCacheFile);
			if (force == true ||
					(f.exists() == true && isCacheOld(progCacheFile) == true) ||
					(f.exists() == false && isCacheOld(null) == true)) {
				String url = "";
				if (pl.Type.equals("csa")) {
					url = "http://www.television.co.jp/programlist/guide24.php?range=24&type=cs&channel="+pl.CenterId;
				}
				else if (pl.Type.equals("csd")) {
					url = "http://www.television.co.jp/digitalguide/guide24.php?range=24&type=cs&channel="+pl.CenterId;
				}
				else if (pl.Type.equals(bsCode)) {
					url = "http://www.television.co.jp/digitalguide/guide24.php?range=24&type=bs&channel="+pl.CenterId;
				}
				else {
					url = "http://www.television.co.jp/digitalguide/guide24.php?area_pref_key="+pl.Area+"&range=24&type="+pl.Type+"&channel="+pl.CenterId;
				}

				// Web番組表の読み出し
				response = webToBuffer(url, thisEncoding, true);

				// キャッシュファイルの保存
				if ( ! CommonUtils.write2file(progCacheFile, response) ) {
					reportProgress(ERRID+"番組表(キャッシュ)の保存に失敗しました: ("+counter+"/"+counterMax+") "+pl.Center+"    "+progCacheFile);
				}

				reportProgress(MSGID+"(オンライン)を取得しました: ("+counter+"/"+counterMax+") "+pl.Center+"    "+url);
				setCacheFileOnly(false);
			}
			else if (f.exists()) {
				// キャッシュファイルの読み込み
				response = CommonUtils.read4file(progCacheFile, true);
				if ( response == null ) {
					reportProgress(ERRID+"番組表(キャッシュ)の取得に失敗しました: ("+counter+"/"+counterMax+") "+pl.Center+"    "+progCacheFile);
				}
				reportProgress(MSGID+"番組表(キャッシュ)を取得しました: ("+counter+"/"+counterMax+") "+pl.Center+"    "+progCacheFile);
			}
			else {
				reportProgress(ERRID+"番組表(キャッシュ)がみつかりません: ("+counter+"/"+counterMax+") "+pl.Center+"    "+progCacheFile);
				return;
			}

			// 日付リストの追加
			getDateList(pl, response);

			// 番組リストの追加
			getPrograms(pl, response);

			// 日付の調整
			setAccurateDate(pl.pdate);
		}
		catch (Exception e) {
			reportProgress(ERRID+"番組表の取得で例外が発生しました： "+e.toString());
			e.printStackTrace();
		}
	}

	//
	private void getDateList(ProgList pl, String response) {
		// 日付の処理
		Matcher ma = Pattern.compile("<tr class=\"head\">(.+?)</tr>").matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile(">(\\d+)/(\\d+)（(.)）</a>").matcher(ma.group(1));
			while ( mb.find() ) {
				ProgDateList cl = new ProgDateList();
				cl.row = 0;

				int month = Integer.valueOf(mb.group(1));
				int day = Integer.valueOf(mb.group(2));
				int wday = CommonUtils.getWday(mb.group(3));
				cl.Date = CommonUtils.getDateByMD(month, day, wday, true);

				pl.pdate.add(cl);
			}

			// ８日目は取得できない
		}
	}

	//
	private void getPrograms(ProgList pl, String src) {

		ArrayList<ProgDateList> pdate = pl.pdate;

		HashMap<String,ProgOption> marks = new HashMap<String,ProgOption>();
		marks.put("/function-50.", ProgOption.FIRST);
		marks.put("/function-201.", ProgOption.BILINGUAL);
		marks.put("/function-210.", ProgOption.MULTIVOICE);
		marks.put("/function-401.", ProgOption.STANDIN);
		marks.put("/function-501.", ProgOption.SUBTITLE);
		marks.put("/function-601.", ProgOption.DATA);
		marks.put("/function-711.", ProgOption.PV);
		marks.put("/function-nama.", ProgOption.LIVE);

		Matcher ma = Pattern.compile("<td class=\"program (category-(\\d*?)|overhead)\" rowspan=\"(\\d+?)\">(.+?)</td>").matcher(src);
		while ( ma.find() ) {

			ProgDetailList pdl = new ProgDetailList();

			// 番組長
			pdl.length = Integer.valueOf(ma.group(3));

			// 挿入位置決定
			int col = -1;
			int rowMin = 9999;
			for ( int i=0; i<pdate.size(); i++ ) {
				if (pdate.get(i).row < rowMin) {
					col = i;
					rowMin = pdate.get(i).row;
				}
			}
			if ( col == -1 ) {
				System.err.println(ERRID+"番組情報の挿入先がみつかりません");
				return;
			}

			pdate.get(col).pdetail.add(pdl);
			pdate.get(col).row += pdl.length;

			// 詳細設定
			if ( ma.group(2) == null ) {
				pdl.title = pdl.splitted_title = "番組情報がありません";
			}
			else {
				// 番組ジャンル
				if (ma.group(2).equals("0")) {
					pdl.genre = ProgGenre.NEWS;
				}
				else if (ma.group(2).equals("1")) {
					pdl.genre = ProgGenre.SPORTS;
				}
				else if (ma.group(2).equals("2")) {
					pdl.genre = ProgGenre.VARIETYSHOW;
				}
				else if (ma.group(2).equals("3")) {
					pdl.genre = ProgGenre.DORAMA;
				}
				else if (ma.group(2).equals("4")) {
					pdl.genre = ProgGenre.MUSIC;
				}
				else if (ma.group(2).equals("5")) {
					pdl.genre = ProgGenre.VARIETY;
				}
				else if (ma.group(2).equals("6")) {
					pdl.genre = ProgGenre.MOVIE;
				}
				else if (ma.group(2).equals("7")) {
					pdl.genre = ProgGenre.ANIME;
				}
				else if (ma.group(2).equals("8")) {
					pdl.genre = ProgGenre.DOCUMENTARY;
				}
				else if (ma.group(2).equals("9")) {
					pdl.genre = ProgGenre.THEATER;
				}
				else if (ma.group(2).equals("10")) {
					pdl.genre = ProgGenre.HOBBY;
				}
				else if (ma.group(2).equals("11")) {
					pdl.genre = ProgGenre.WELFARE;
				}
				else if (ma.group(2).equals("14")) {
					// 拡張エリア - 広帯域CS デジタル放送拡張用情報　　映画 - 邦画
					pdl.genre = ProgGenre.MOVIE;
				}
				else if (ma.group(2).equals("15")) {
					pdl.genre = ProgGenre.NOGENRE;
				}
				else {
					if ( ma.group(2) != null && ma.group(2).length() > 0 ) {
						System.out.println(DBGID+"未定義のジャンルコード： "+ma.group(2)+", "+ma.group(4));
					}
					pdl.genre = ProgGenre.NOGENRE;
				}

				// 番組タイトル＋α
				// 1 : dummy
				// 2 : id-a
				// 3 : id-b
				// 4 : title

				// マークを取り出してみる
				Matcher mb = Pattern.compile("(/function-.+?\\.)").matcher(ma.group(4));
				while ( mb.find() ) {
					for ( String mark : marks.keySet() ) {
						if ( mb.group(1).equals(mark) ) {
							pdl.addOption(marks.get(mark));
							break;
						}
					}
				}

				// 番組詳細
				{
					mb = Pattern.compile("<div class=\"subtitle\">(.*?)</div>").matcher(ma.group(4));
					if ( mb.find() ) {
						pdl.detail = mb.group(1);
					}

					mb = Pattern.compile("<div name=\"programcontent\" class=\"content\">((【.+?】)*)(.*?)</div>").matcher(ma.group(4));
					if ( mb.find() ) {
						// 番組詳細の「先頭」に新番組マークなどがある場合がある。ない場合もある。
						Matcher mc = Pattern.compile("(【.+?】)").matcher(mb.group(1));
						while (mc.find()) {
							if (mc.group(1).equals("【新】")) {
								pdl.flag = ProgFlags.NEW;
							}
							else if (mc.group(1).equals("【終】")) {
								pdl.flag = ProgFlags.LAST;
							}
							else if (mc.group(1).equals("【サブタイトル】")) {
								// 何もしない
							}
							else {
								// その他の不明な【？】はそのままにする
								pdl.detail += mc.group(1);
							}
						}
						// マーク以降
						pdl.detail += mb.group(3);
					}

					// 記号を文字化する
					pdl.detail = Pattern.compile("(<img .+? alt=\"(.+?)\"/>)",Pattern.DOTALL).matcher(pdl.detail).replaceAll("$2").trim();
				}

				// タイトル・番組詳細ページへのリンク
				mb = Pattern.compile("<a href=\"(javascript:detail.show\\('(.+?)'\\)|detail\\.php\\?id=(.+?)&ref=guide24)\">[\\s,]*(.+?)[\\s,]*</a>").matcher(ma.group(4));
				if ( mb.find() ) {
					pdl.title =  CommonUtils.unEscape(mb.group(4));
					Matcher mc = Pattern.compile("(<img src=\"(.+?)\" alt=\"(.+?)\"/>)").matcher(pdl.title);
					while ( mc.find() ) {
						pdl.title = pdl.title.replace(mc.group(1),mc.group(3));
					}

					if (mb.group(2) != null) {
						pdl.link = "http://www.television.co.jp/digitalguide/detail.php?id="+mb.group(2);
					}
					else if (mb.group(3) != null) {
						pdl.link = "http://www.television.co.jp/programlist/detail.php?id="+mb.group(3)+"&ref=guide";
					}
					else {
						pdl.link = "";
					}
				}

				// タイトルから各種フラグを分離する
				doSplitFlags(pdl, nf);

				// サブタイトル分離
				doSplitSubtitle(pdl);

				if ( ma.group(4).indexOf("\"再放送\"") >= 0 ) {
					pdl.addOption(ProgOption.REPEAT);
				}

				if ( ma.group(4).indexOf("\"生放送\"") >= 0 ) {
					pdl.addOption(ProgOption.LIVE);
				}

				if ( ma.group(4).indexOf("\"無料放送\"") >= 0 ) {
					pdl.noscrumble = ProgScrumble.NOSCRUMBLE;
				}

				if ( ma.group(4).indexOf("function-a.") >= 0 ) {
					pdl.flag = ProgFlags.NEW;
				}
				else if ( ma.group(4).indexOf("function-l.") >= 0 ) {
					pdl.flag = ProgFlags.LAST;
				}

				if ( ma.group(4).indexOf("function-50.") >= 0 ) {
					pdl.addOption(ProgOption.FIRST);
				}

				// 番組ID
				try {
					int hdr = 0;
					int chnum = 0;
					if ( pl.CenterId.startsWith("CS") ) {
						chnum = Integer.valueOf(pl.CenterId.substring(2));
						hdr = 7;
					}
					else if ( pl.CenterId.startsWith("OTD") ) {
						chnum = Integer.valueOf(pl.CenterId.substring(3));
						hdr = ( chnum < 1024 ) ? (4) : (0x7FFF);
					}
					if ( chnum > 0 ) {
						ContentIdDIMORA.decodeChId(String.format("%04X%04X%04X", hdr,0,chnum));
						pdl.progid = ContentIdDIMORA.getContentId(0,"");
					}
				}
				catch (NumberFormatException e) {
					System.err.println("[ERROR] invalid chcode format "+pl.Center+" "+pl.CenterId+" "+e.toString());
				}

				// 開始・終了時刻
				mb = Pattern.compile("<b>(\\d+):(\\d+)</b>").matcher(ma.group(4));
				if ( mb.find() ) {
					GregorianCalendar c = new GregorianCalendar(Locale.JAPAN);

					int ahh = Integer.valueOf(mb.group(1));
					int amm = Integer.valueOf(mb.group(2));
					pdl.start = String.format("%02d:%02d", ahh, amm);

					c.set(Calendar.HOUR_OF_DAY, ahh);
					c.set(Calendar.MINUTE, amm);
					c.add(Calendar.MINUTE,pdl.length);
					pdl.end = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
				}

				//
				pdl.extension = false;
				//pdl.flag = ProgFlags.NOFLAG;
				pdl.nosyobo = false;
			}
		}
	}


	/*******************************************************************************
	 * 地域情報を取得する
	 ******************************************************************************/

	// 普通は東京
	@Override
	public String getDefaultArea() {return "東京";}

	//
	public void loadAreaCode() {

		// 設定ファイルが存在していればファイルから
		File f = new File(getAreaSelectedFile());
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			ArrayList<AreaCode> tmp = (ArrayList<AreaCode>) CommonUtils.readXML(getAreaSelectedFile());
			if ( tmp != null ) {

				aclist = tmp;

				// 後方互換
				{
					boolean bsflg = false;
					for (AreaCode ac : aclist) {
						if (ac.getCode().equals(bsCode)) {
							bsflg = true;
							break;
						}
					}
					if ( ! bsflg) {
						AreaCode ac = new AreaCode();
						ac.setArea("ＢＳ");
						ac.setCode(bsCode);
						aclist.add(ac);

						saveAreaCode();
					}
				}

				System.out.println("地域リストを読み込みました: "+getAreaSelectedFile());
				return;
			}
			else {
				System.err.println("地域リストの読み込みに失敗しました: "+getAreaSelectedFile());
			}
		}

		// 地域一覧の作成
		ArrayList<AreaCode> newaclist = new ArrayList<AreaCode>();

		// 存在していなければWeb上から
		String uri = "http://www.television.co.jp/digitalguide/map.html";
		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.out.println("地域情報の取得に失敗しました: "+uri);
			return;
		}

		{
			// 北海道
			Matcher ma = Pattern.compile("<option value=\"([^\"]+?)\">(.+?)</option>").matcher(response);
			while (ma.find()) {
				AreaCode ac = new AreaCode();
				ac.setArea(ma.group(2));
				ac.setCode(ma.group(1));
				newaclist.add(ac);
			}

			// 北海道以外
			ma = Pattern.compile("\"javascript:setPref\\('(.+?)'\\)\">(.+?)</a>").matcher(response);
			while (ma.find()) {
				AreaCode ac = new AreaCode();
				ac.setArea(ma.group(2));
				ac.setCode(ma.group(1));
				newaclist.add(ac);
			}
		}

		if ( newaclist.size() == 0 ) {
			System.err.println(ERRID+"地域一覧の取得結果が０件だったため情報を更新しません");
			return;
		}

		{
			{
				AreaCode ac = new AreaCode();
				ac.setArea("全国");
				ac.setCode(allCode);
				newaclist.add(0,ac);
			}
			{
				AreaCode ac = new AreaCode();
				ac.setArea("ＢＳ");
				ac.setCode(bsCode);
				newaclist.add(ac);
			}
		}

		aclist = newaclist;
		saveAreaCode();
	}


	/*******************************************************************************
	 * 放送局情報を取得する
	 ******************************************************************************/

	// 設定ファイルがなければWebから取得
	public void loadCenter(String code, boolean force) {

		if ( code == null ) {
			System.out.println(ERRID+"地域コードがnullです.");
			return;
		}

		String centerListFile = getCenterListFile(getTVProgramId(), code);

		if (force) {
			File f = new File(centerListFile);
			f.delete();
		}

		File f = new File(centerListFile);
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			ArrayList<Center> tmp = (ArrayList<Center>) CommonUtils.readXML(centerListFile);
			if ( tmp != null ) {

				crlist = tmp;
				attachChFilters();	// 放送局名変換

				// 後方互換
				for (Center c : crlist) {
					if ( c.getType().equals(bsCode) && ! c.getAreaCode().equals(bsCode)) {
						c.setAreaCode(bsCode);
					}
				}

				System.out.println("放送局リストを読み込みました: "+centerListFile);
				return;
			}
			else {
				System.err.println("放送局リストの読み込みに失敗しました: "+centerListFile);
			}
		}

		// Web上から放送局の一覧を取得する
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		int cntMax = ((code.equals(allCode))?(aclist.size()-2):(1)) + 1;
		int cnt = 1;

		// 地上波
		if (code.equals(allCode)) {
			for (AreaCode ac : aclist) {
				if ( ! ac.getCode().equals(allCode) && ! ac.getCode().equals(bsCode)) {
					String url = "http://www.television.co.jp/digitalguide/guide.php?type=tv&area_pref_key="+ac.getCode();
					if ( _loadCenter(newcrlist, ac.getCode(),"tv", url) ) {
						reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
					}
					cnt++;
				}
			}
		}
		else {
			String url = "http://www.television.co.jp/digitalguide/guide.php?type=tv&area_pref_key="+code;
			if ( _loadCenter(newcrlist, code,"tv",url) ) {
				reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
			}
			cnt++;
		}

		// BS
		{
			String url = "http://www.television.co.jp/digitalguide/guide.php?type=bs";
			if ( _loadCenter(newcrlist, bsCode,bsCode,url) ) {
				reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
			}
			cnt++;
		}

		if ( newcrlist.size() == 0 ) {
			System.err.println(ERRID+"放送局情報の取得結果が０件だったため情報を更新しません");
			return;
		}

		crlist = newcrlist;
		attachChFilters();	// 放送局名変換
		saveCenter();		// 保存
	}

	protected boolean _loadCenter(ArrayList<Center> newcrlist, String code, String type, String uri) {

		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.err.println("放送局情報の取得に失敗しました: "+uri);
			return false;
		}

		// 局名リストに追加する

		Matcher ma = Pattern.compile("<td><select(.+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"([^\"]+?)\">\\s*(&nbsp;)*(.+?)\\s*</option>").matcher(ma.group(1));
			while (mb.find()) {
				String centerName = CommonUtils.unEscape(mb.group(3));
				String centerId = mb.group(1);

				// NHK総合・NHK教育
				if ( type.equals("tv") ) {
					Matcher mc = Pattern.compile("^NHK(総合|Eテレ)(\\d+)・?(.+)$").matcher(centerName);
					if ( mc.find() ) {
						String prefix = "";
						if ( mc.group(1).equals("総合") ) {
							prefix = "ＮＨＫ総合";
						}
						else if ( mc.group(1).equals("Eテレ") ) {
							prefix = "ＮＨＫ Ｅテレ";
						}
						else {
							prefix = "ＮＨＫ"+mc.group(1);
						}
						if ( mc.group(2).equals("1") ) {
							centerName = prefix+"・"+mc.group(3);
						}
						else {
							centerName = prefix+mc.group(2)+"・"+mc.group(3);
						}
					}
				}

				Center cr = new Center();
				cr.setAreaCode(code);
				cr.setCenterOrig(centerName);
				cr.setLink(centerId);
				cr.setType(type);
				cr.setEnabled(true);
				newcrlist.add(cr);
			}
		}

		return true;
	}

}
