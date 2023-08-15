package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
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


public class PlugIn_TVPTVKingdom extends TVProgramUtils implements TVProgram,Cloneable {

	public PlugIn_TVPTVKingdom clone() {
		return (PlugIn_TVPTVKingdom) super.clone();
	}

	final static String thisEncoding = "UTF-8";


	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return "Gガイド.テレビ王国"; }

	@Override
	public ProgType getType() { return ProgType.PROG; }

	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.TERRA; }


	/*******************************************************************************
	 * 個体の特性
	 ******************************************************************************/

	@Override
	public int getTimeBarStart() {return 5;}

	private int getDogDays() { return ((getExpandTo8())?(8):(7)); }


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	protected final static int accessWait = 1200;

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	private final String PROGCACHE_PREFIX = "TVK_";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 新しい入れ物の臨時格納場所
	protected ArrayList<ProgList> newplist = new ArrayList<ProgList>();

	// 未定義のフラグの回収場所
	protected final HashMap<String,String> nf = new HashMap<String, String>();

	protected void debugNF() {
		for ( String f : nf.keySet() ) {
			System.err.println(String.format("【デバッグ情報】未定義のフラグです: [%s]",f));
		}
	}

	// BS番組表の総ページ数
	private String getBscntFile() { return String.format("env"+File.separator+"bscnt.%s",getTVProgramId()); }
	private int bscnt = 2;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/


	/*******************************************************************************
	 * 番組情報を取得する
	 ******************************************************************************/

	@Override
	public boolean loadProgram(String areaCode, boolean force) {
		setCacheFileOnly(true);

		if (!force && loadFromProgCache(PROGCACHE_PREFIX + areaCode)){
			return true;
		}

		// 入れ物を空にする
		newplist = new ArrayList<ProgList>();
		nf.clear();

		// 地域コードごとの参照ページ数の入れ物を用意する
		LinkedHashMap<String,Integer> pages = new LinkedHashMap<String, Integer>();

		// 参照する地域コードをまとめる
		if ( areaCode.equals(allCode) ) {
			// 「全国」
			for ( Center cr : crlist ) {
				if ( cr.getOrder() > 0 ) {
					// 有効局の地域コードのみ集める
					pages.put(cr.getAreaCode(),0);
				}
			}
		}
		else {
			// 地域個別
			pages.put(areaCode,0);
			pages.put(bsCode,0);
		}

		// トップの下に局ごとのリストを生やす
		for ( String ac : pages.keySet() ) {
			for ( Center cr : crlist ) {
				if ( ac.equals(cr.getAreaCode()) ) {
					ProgList pl = new ProgList();
					pl.Area = cr.getAreaCode();
					pl.SubArea = cr.getType();
					pl.Center = cr.getCenter();
					pl.CenterId = cr.getLink();
					pl.BgColor = cr.getBgColor();

					// <TABLE>タグの列数を決め打ちで処理するので、設定上無効な局も内部的には列の１つとして必要
					pl.enabled = (cr.getOrder()>0)?(true):(false);

					newplist.add(pl);

					int pg = Integer.valueOf(cr.getType());
					if ( pl.enabled && pages.get(ac) < pg ) {
						// 地域コードごとの最大参照ページ数を格納する
						pages.put(ac,pg);
					}
				}
			}
		}

		// 局の下に日付ごとのリストを生やす
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		if ( CommonUtils.isLateNight(cal) ) {
			// ４時までは当日扱いにする
			cal.add(Calendar.DATE, -1);
		}
		GregorianCalendar cale = (GregorianCalendar) cal.clone();
		for (int i=0; i<getDogDays(); i++) {
			String date = CommonUtils.getDate(cale);
			for ( ProgList pl : newplist ) {
				ProgDateList cl = new ProgDateList();
				cl.Date = date;
				pl.pdate.add(cl);
			}
			cale.add(Calendar.DATE, 1);
		}

		// 参照する総ページ数を計算
		int counterMax = 0;
		for ( String ac : pages.keySet() ) {
			counterMax += pages.get(ac)*getDogDays();
		}

		// 番組リストの追加
		int counter = 1;
		for (String ac : pages.keySet()) {
			cale = (GregorianCalendar) cal.clone();
			for (int i=0; i<getDogDays(); i++) {
				String date = CommonUtils.getDateYMD(cale)+"0500";
				int dMax = (pages.get(ac)<REFPAGESMAX) ? pages.get(ac) : REFPAGESMAX;
				for ( int d=1; d<=dMax; d++ ) {	// 最大{REFPAGESMAX}ページまでしか参照しない
					String url = null;
					String cookie = null;
					if (ac.equals(bsCode)) {
						// BS4K8K
						if (d>bscnt){
							int d4k = d-bscnt;
							url = "https://www.tvkingdom.jp/chart/bs4k8k_"+d4k+".action?head="+date+"&span=24";
						}
						// BS
						else
							url = "https://www.tvkingdom.jp/chart/bs"+d+".action?head="+date+"&span=24";
						cookie = null;
					}
					else {
						// 地上波・UHF
						if ( d == 1 ) {
							url = "https://www.tvkingdom.jp/chart/"+ac+".action?head="+date+"&span=24";
							cookie = "gtv.stationAreaId=" + ac + "; ";
						}
						else if ( d == 2 ) {
							String ck = "";
							for ( ProgList pl : newplist ) {
								if ( pl.Area.equals(ac) && pl.SubArea.equals("2") ) {
									// サブチャンネルがある
									String[] id = pl.CenterId.split(",");
									if (id.length == 2) {
										ck += "gtv.selectedStationId."+id[0]+"="+id[1]+"; ";
									}
								}
							}
							if ( ! ck.equals("") ) {
								url = "https://www.tvkingdom.jp/chart/"+ac+".action?head="+date+"&span=24";
								cookie = ck;
							}
						}
					}
					if ( url != null ) {
						if (!_loadProgram(ac, d, dMax, url, cookie, force, i, cale.get(Calendar.MONTH)+1, cale.get(Calendar.DATE), counter++, counterMax)){
							newplist = null;
							return false;
						}
					}

					if (isCancelRequested()){
						newplist = null;
						reportProgress(ERRID+"中止要求があったので番組表の取得を中止します。");
						return false;
					}

				}

				cale.add(Calendar.DATE, 1);
			}
		}

		// 開始・終了日時を正しい値に計算しなおす
		for (ProgList pl : newplist) {
			setAccurateDate(pl.pdate);
		}

		// 解析用
//		debugNF();

		// 古い番組データを置き換える
		if (pcenter != null)
			pcenter.clear();
		pcenter = newplist;
		newplist = null;

		saveToProgCache(PROGCACHE_PREFIX + areaCode);

		return true;
	}

	/* ここまで */



	/*
	 * 非公開メソッド等
	 */

	//
	private boolean _loadProgram(String ac, int page, int pmax, String url, String cookie, boolean force, int wdaycol, int month, int day, int counter, int counterMax) {
		//　progfilesの読み出し
		//
		final String progCacheFile = String.format(getProgDir()+File.separator+"TVK_%s_%s_%04d.html", ac, page, day);
		final String target = String.format("(%d/%d) %s - %d日[%d/%d] %s", counter,counterMax,getArea(ac),day,page,pmax,url);

		try {
			File f = new File(progCacheFile);
			if (!force && f.exists() && CommonUtils.isFileAvailable(f,10) && !isCacheOld(progCacheFile)) {
				reportProgress(String.format("%s (キャッシュ)を取得しました: %s", getTVProgramId(), target));
			}
			else{
				if (!webToFile(url, null, cookie, null, progCacheFile, thisEncoding)){
					reportProgress(ERRID+String.format("%s (オンライン)の取得に失敗しました: %s", getTVProgramId(), target));
					if (f.exists() && CommonUtils.isFileAvailable(f,10))
						reportProgress(String.format("%s (キャッシュ)を代わりに使用します: %s", getTVProgramId(), target));
					else
						return false;
				}

				setCacheFileOnly(false);
				reportProgress(String.format("%s (オンライン)を取得しました: %s",getTVProgramId(), target));
				// 連続アクセス規制よけ
				CommonUtils.milSleep(accessWait);
			}

			// キャッシュファイルの読み込み
			String response = CommonUtils.read4file(progCacheFile, true);
			if ( response == null ) {
				reportProgress(ERRID+"番組表の取得に失敗しました: "+url);
				return false;
			}

			// キャッシュが不整合を起こしていたら投げ捨てる
			Matcher ma = Pattern.compile(String.format("<title>%d月 %d日",month,day)).matcher(response);
			if ( ! ma.find() ) {
				reportProgress(String.format("%s (キャッシュ)が無効です: %s",getTVProgramId(), target));
				return false;
			}

			// 番組リストの追加
			getPrograms(ac, String.valueOf(page), wdaycol, response);
		}
		catch (Exception e) {
			// 例外
			System.out.println("Exception: _loadProgram()");
			e.printStackTrace();
			reportProgress(ERRID+"番組表の取得に失敗しました: "+url);
			return false;
		}

		return true;
	}

	//
	private void getPrograms(String areacode, String page, int wdaycol, String src) {

		//
		int col = -1;
		for ( int i=0; i<newplist.size(); i++ ) {
			ProgList pl = newplist.get(i);
			if ( pl.Area.equals(areacode) && pl.SubArea.equals(page) ) {
				col = i;
				break;
			}
		}
		if (col < 0) {
			System.out.println("getProgram() error");
			return;
		}

		Matcher ma = Pattern.compile("<div class=\"cell-station cell-top\"(.+?)<div class=\"cell-station\"").matcher(src);
		while ( ma.find() ) {
			ProgList pl = newplist.get(col++);
			// 一局分取り込む
			getDetails(pl.pdate.get(wdaycol), ma.group(1));
		}
	}
	//
	protected void getDetails(ProgDateList pcl, String response) {

		Matcher mb = Pattern.compile("<div class=\"cell-schedule( cell-genre-(\\d+?) cell-genre-(\\d+?) )?(.+?)</div>").matcher(response);
		while (mb.find())
		{
			Matcher mc = Pattern.compile("top: (\\d+)px;.+?(>番組情報がありません<|(/schedule/\\d+(\\d\\d)(\\d\\d)\\.action).+?<span class=\"schedule-title.?\">(.*?)</span>.+?<span class=\"schedule-summary.?\">(.*?)</span>)").matcher(mb.group(4));
			while (mc.find()) {
				// なんかしらんが位置が被ってるのがある
				ProgDetailList pdl = null;
				if (pcl.pdetail.size() > 0) {
					if (pcl.pdetail.get(pcl.pdetail.size()-1).length == Integer.valueOf(mc.group(1))) {
						pdl = pcl.pdetail.get(pcl.pdetail.size()-1);
					}
					else {
						pdl = new ProgDetailList();
						pcl.pdetail.add(pdl);
					}
				}
				else {
					pdl = new ProgDetailList();
					pcl.pdetail.add(pdl);
				}

				// 詳細情報の処理
				if (mc.group(2).equals(">番組情報がありません<")) {
					pdl.title = pdl.splitted_title = "番組情報がありません";
					pdl.detail = "";
					pdl.length = Integer.valueOf(Integer.valueOf(mc.group(1)));
					pdl.genre = ProgGenre.NOGENRE;
					pdl.start = "";
				}
				else {
					// タイトルと番組詳細
					pdl.title = CommonUtils.unEscape(mc.group(6)).replaceAll("<wbr/>","");
					pdl.detail = CommonUtils.unEscape(mc.group(7)).replaceAll("<wbr/>","").replaceAll("<BR>", "\n");
					pdl.length = Integer.valueOf(Integer.valueOf(mc.group(1)));

					// タイトルから各種フラグを分離する
					doSplitFlags(pdl, nf);

					//
					if (mb.group(3) == null) {
						pdl.genre = ProgGenre.NOGENRE;
					}
					else if (mb.group(3).equals("100000")) {
						pdl.genre = ProgGenre.NEWS;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("101000")) {
						pdl.genre = ProgGenre.SPORTS;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("102000")) {
						pdl.genre = ProgGenre.VARIETYSHOW;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("103000")) {
						pdl.genre = ProgGenre.DORAMA;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("104000")) {
						pdl.genre = ProgGenre.MUSIC;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("105000")) {
						pdl.genre = ProgGenre.VARIETY;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("106000")) {
						pdl.genre = ProgGenre.MOVIE;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("107000")) {
						pdl.genre = ProgGenre.ANIME;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("108000")) {
						pdl.genre = ProgGenre.DOCUMENTARY;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("109000")) {
						pdl.genre = ProgGenre.THEATER;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("110000")) {
						pdl.genre = ProgGenre.HOBBY;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else if (mb.group(3).equals("111000")) {
						pdl.genre = ProgGenre.WELFARE;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}
					else {
						pdl.genre = ProgGenre.NOGENRE;
						pdl.subgenre = getSubgenre(pdl.genre, mb.group(2).substring(4));
					}

					// サブタイトル分離
					doSplitSubtitle(pdl);

					// 番組ID
					Matcher md = Pattern.compile("/iepg\\.tvpi\\?id=(\\d+)\"").matcher(mb.group(4));
					if ( md.find() ) {
						String chid = null;
						if ( md.group(1).startsWith("200") && md.group(1).length() >= 6 ) {
							chid = String.format("%04X%04X%04X", 4,0,Integer.valueOf(md.group(1).substring(3,6)));
						}
						else if ( md.group(1).startsWith("500") && md.group(1).length() >= 6 ) {
							chid = String.format("%04X%04X%04X", 7,0,Integer.valueOf(md.group(1).substring(3,6)));
						}
						else if ( md.group(1).startsWith("1") && md.group(1).length() >= 6 ) {
							chid = String.format("%04X%04X%04X", 0x7FFF,0,Integer.valueOf(md.group(1).substring(0,6))-100000);
						}
						if ( chid != null ) {
							ContentIdDIMORA.decodeChId(chid);
							pdl.progid = ContentIdDIMORA.getContentId(0,"");
						}
					}

					//
					pdl.link = "https://www.tvkingdom.jp"+mc.group(3);

					//
					pdl.start = String.format("%02d:%02d", Integer.valueOf(mc.group(4)), Integer.valueOf(mc.group(5)));
				}
			}
		}
		// lengthの調整
		for (int i=1; i<pcl.pdetail.size(); i++) {
			pcl.pdetail.get(i-1).length = (pcl.pdetail.get(i).length - pcl.pdetail.get(i-1).length)/3;
		}
		if (pcl.pdetail.size() >= 1) {
			pcl.pdetail.get(pcl.pdetail.size()-1).length = 24*60 - (pcl.pdetail.get(pcl.pdetail.size()-1).length-20)/3;
		}

		for (int i=0; i<pcl.pdetail.size(); i++) {
			pcl.row += pcl.pdetail.get(i).length;
		}

		// 終了時刻の調整
		GregorianCalendar c = new GregorianCalendar(Locale.JAPAN);
		c.setTime(new Date());
		for (ProgDetailList pdl : pcl.pdetail) {
			Matcher mx = Pattern.compile("(\\d\\d):(\\d\\d)").matcher(pdl.start);
			if (mx.find()) {
				c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(mx.group(1)));
				c.set(Calendar.MINUTE, Integer.valueOf(mx.group(2)));
				c.add(Calendar.MINUTE, pdl.length);
				pdl.end = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
			}
		}
	}
	//
	private ProgSubgenre getSubgenre(ProgGenre genre, String s) {
		String code = String.format("%X", Integer.valueOf(s));
		for (ProgSubgenre subgenre : ProgSubgenre.values(genre)) {
			//if (subgenre.getGenre() == genre && subgenre.toIEPG().equals(code)) {
			if (subgenre.toIEPG().equals(code)) {
				return(subgenre);
			}
		}
		return(null);
	}



	/*******************************************************************************
	 * 地域情報を取得する
	 ******************************************************************************/

	//
	@Override
	public String getDefaultArea() {return "東京";}

	//
	public void loadAreaCode(){

		// 設定ファイルが存在していればファイルから
		File f = new File(getAreaSelectedFile());
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			ArrayList<AreaCode> tmp = (ArrayList<AreaCode>) CommonUtils.readXML(getAreaSelectedFile());
			if ( tmp != null ) {

				aclist = tmp;

				// 後方互換
				for ( int i=aclist.size()-1; i>=0; i-- ) {
					if (aclist.get(i).getCode().equals("bs2")) {
						aclist.remove(i);
					}
					else if (aclist.get(i).getCode().equals("bs1")) {
						aclist.get(i).setArea("ＢＳ");
						aclist.get(i).setCode(bsCode);
					}
				}

				return;
			}
			else  {
				System.err.println("地域リストの読み込みに失敗しました: "+getAreaSelectedFile());
			}
		}

		// 地域一覧の作成
		ArrayList<AreaCode> newaclist = new ArrayList<AreaCode>();

		// 存在していなければWeb上から
		String uri = "https://www.tvkingdom.jp/chart/23.action";
		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.err.println("地域情報の取得に失敗しました: "+uri);
			return;
		}

		Matcher ma = Pattern.compile("<select name=\"stationAreaId\" id=\"area-selector\" onchange=\"this.form.submit\\(\\);\">(.+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"([^\"]+?)\" ?(selected=\"selected\")?>(.+?)</option>").matcher(ma.group(1));
			while (mb.find()) {
				if (mb.group(3).indexOf("CATV") == -1) {
					AreaCode ac = new AreaCode();
					ac.setArea(mb.group(3));
					ac.setCode(mb.group(1));
					newaclist.add(ac);
				}
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

		//
		aclist = newaclist;
		saveAreaCode();
	}

	//
	public void saveAreaCode() {
		if ( ! CommonUtils.writeXML(getAreaSelectedFile(), aclist) ) {
			System.err.println("地域リストの保存に失敗しました: "+getAreaSelectedFile());
		}
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

		// BSのページ数の初期化(事前に判明していない場合は2)
		int bscntTmp = CommonUtils.loadCnt(getBscntFile());
		bscntTmp = bscnt = (bscntTmp > 0)?(bscntTmp):(2);

		//
		String centerListFile = getCenterListFile(getTVProgramId(), code);

		if (force) {
			File f = new File(centerListFile);
			f.delete();
		}

		File f = new File(centerListFile);
		if (f.exists() == true) {
			//System.out.println("Center Alredy Exist: "+centerListFile);
			@SuppressWarnings("unchecked")
			ArrayList<Center> tmp = (ArrayList<Center>) CommonUtils.readXML(centerListFile);
			if ( tmp != null ) {

				crlist = tmp;

				// 放送局名変換
				attachChFilters();

				// 後方互換
				for (Center c : crlist) {
					if (c.getAreaCode().equals("bs2")) {
						c.setAreaCode(bsCode);
						c.setType("2");
					}
					else if (c.getAreaCode().equals("bs1")) {
						c.setAreaCode(bsCode);
						c.setType("1");
					}
					else if (c.getType().equals("")) {
						c.setType("1");
					}
				}

				System.out.println("放送局リストを読み込みました: "+centerListFile);

				return;
			}
			else {
				System.err.println("放送局リストの読み込みに失敗しました: "+centerListFile);
			}
		}

		// 放送局をつくるよ
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		// 地上派・UHFは地域別に扱う

		int bs4kcnt = 2;	// BS4K8Kのページ数は現状固定
		int cntMax = ((code.equals(allCode))?(aclist.size()-2):(1))+bscnt+bs4kcnt;
		int cnt = 1;
		for (AreaCode ac : aclist) {
			if (ac.getCode().equals(bsCode)) {
				continue;
			}
			else if (code.equals(allCode) && ac.getCode().equals(allCode)) {
				continue;
			}
			else if ( ! code.equals(allCode) && ! ac.getCode().equals(code)) {
				continue;
			}

			delCookie("gtv.stationAreaId");
			addCookie("gtv.stationAreaId", code);

			String url;

			// 地上波
			url = "https://www.tvkingdom.jp/chart/"+ac.getCode()+".action";
			if ( _loadCenter(newcrlist, ac.getCode(), "1", url) ) {
				reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
			}
			cnt++;
		}

		// BSdは共通にする(bscntは_loadCenter()中で増加する可能性あり)

		for ( int d=1; d<=bscnt; d++ )
		{
			String url = "https://www.tvkingdom.jp/chart/bs"+d+".action";
			if ( _loadCenter(newcrlist, bsCode, String.valueOf(d), url) ) {
				reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
			}
			cnt++;
		}

		// BS4K&8Kは共通にする（ページ数はとりあえず固定）
		for ( int d=1; d<=bs4kcnt; d++ ){
			String url = "https://www.tvkingdom.jp/chart/bs4k8k_"+d+".action?group=4"+d;
			if ( _loadCenter(newcrlist, bsCode, String.valueOf(bscnt+d), url) ) {
				reportProgress("放送局情報を取得しました: ("+cnt+"/"+cntMax+") "+url);
			}
			cnt++;
		}

		// BSのページ数を記録する

		if ( bscntTmp < bscnt ) {
			reportProgress("BSのページ数が変更されました: "+bscntTmp+"→"+bscnt);
			CommonUtils.saveCnt(bscnt,getBscntFile());
		}

		crlist = newcrlist;
		attachChFilters();	// 放送局名変換
		saveCenter();
	}

	private boolean _loadCenter(ArrayList<Center> newcrlist, String code, String page, String uri) {

		String response = webToBuffer(uri,thisEncoding,true);
		CommonUtils.milSleep(accessWait);	// 連続アクセス規制よけ
		if ( response == null ) {
			System.err.println("放送局情報の取得に失敗しました: "+uri);
			return false;
		}

		// BSのページ数を計算する

		for ( int i=bscnt+1; i<=10; i++ ) {
			if ( ! response.matches(".*\"/chart/bs"+i+"\\.action\".*") ) {
				if ( bscnt < i-1 ) {
					bscnt = i-1;
				}
				break;
			}
		}

		// 局名リストに追加する

		int fCol = newcrlist.size();
		Matcher mb = Pattern.compile("id=\"cell-station-top-(\\d+?)\"\\s*title=\"(.+?)\"").matcher(response);
		while (mb.find()) {
			String centerName = CommonUtils.unEscape(mb.group(2));
			String centerId = mb.group(1);

			// NHK総合・NHK教育
			//centerName = centerName.replaceFirst("^ＮＨＫ総合1・", "ＮＨＫ総合・");
			centerName = centerName.replaceFirst("ＮＨＫＥテレ１・", "ＮＨＫ Ｅテレ・");
			centerName = centerName.replaceFirst("ＮＨＫＥテレ", "ＮＨＫ Ｅテレ・");

			Center cr = new Center();
			cr.setAreaCode(code);
			cr.setCenterOrig(centerName);
			cr.setLink(centerId);
			cr.setType(page);
			cr.setEnabled(true);
			newcrlist.add(cr);
		}

		// マルチチャンネル情報を取得する

		if ( ! code.equals(bsCode) ) {
			HashMap<String,String> mCH = new HashMap<String, String>();
			HashMap<String,String> mNM = new HashMap<String, String>();
			mb = Pattern.compile("<div id=\"station-selectors\">([\\s\\S]+?)</div>").matcher(response);
			if ( mb.find() ) {
				Matcher mc = Pattern.compile("<ul [\\s\\S]*? id=\"station-selector-(\\d+?)\" [\\s\\S]*?>(.+?)</ul>").matcher(mb.group(1));
				while ( mc.find() ) {
					Matcher md = Pattern.compile("\\?selectedStationId=(\\d+?)\">([\\s\\S]+?)</a>").matcher(mc.group(2));
					while ( md.find() ) {
						if ( ! mc.group(1).equals(md.group(1)) && ! mCH.containsKey(mc.group(1)) ) {
							mCH.put(mc.group(1), md.group(1));
							mNM.put(md.group(1), md.group(2));
							break;
						}
					}
				}
			}
			if ( mCH.size() > 0 ) {
				int eCol = newcrlist.size()-1;
				for ( int i=fCol; i<=eCol; i++ ) {
					String oID = newcrlist.get(i).getLink();
					Center cr = new Center();
					if ( mCH.containsKey(oID) ) {
						String mID = mCH.get(oID);
						cr.setCenterOrig(mNM.get(mID));
						cr.setLink(oID+","+mID);
					}
					else {
						cr.setCenterOrig("（選択できません）");
						cr.setLink("");
					}
					cr.setAreaCode(code);
					cr.setType("2");
					cr.setEnabled(true);
					newcrlist.add(cr);
				}
			}
		}

		return true;
	}

}
