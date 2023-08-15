package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
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


public class PlugIn_TVPDimora extends TVProgramUtils implements TVProgram,Cloneable {

	public PlugIn_TVPDimora clone() {
		return (PlugIn_TVPDimora) super.clone();
	}

	private static final String thisEncoding = "UTF-8";


	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	@Override
	public String getTVProgramId() { return "Dimora"; }

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

	private final String MSGID = "["+getTVProgramId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	private final String PROGCACHE_PREFIX = "Dimora_";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 新しい入れ物の臨時格納場所
	protected ArrayList<ProgList> newplist = new ArrayList<ProgList>();

	// 未定義のフラグの回収場所
	private final HashMap<String,String> nf = new HashMap<String, String>();

	protected String getCenterInfoId() { return "ChannelTypeDB"; }
	protected String getChType() { return "6"; }

	protected String getCenterInfoId4K(){ return "m_ChannelTypeUhd"; }
	protected String getChType4K() { return "96"; }

	protected String getSiteUrl(){ return "https://www.dimora.jp/"; }
	protected String getUrl(){ return "https://www.dimora.jp/dc/pc/P4501.do"; }

	protected String getCenterCode(String id, String code) { return id.matches("^3.*$")?(bsCode):(code); }

	protected String getProgCacheFile(String areacode, String adate) { return String.format(getProgDir()+File.separator+"Dimora_%s_%s.html", areacode, adate.substring(6,8)); }


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/


	/*******************************************************************************
	 * 番組情報を取得する
	 ******************************************************************************/
	@Override
	public boolean loadProgram(String areaCode, boolean force) {
		setCacheFileOnly(true);

//		if (!force && loadFromProgCache(PROGCACHE_PREFIX + areaCode)){
//			return true;
//		}

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
					pages.put(cr.getAreaCode(),1);
				}
			}
		}
		else if ( ! areaCode.equals(bsCode) ) {
			// 地域個別
			for ( Center cr : crlist ) {
				if ( cr.getOrder() > 0 ) {
					// 有効局がある場合のみ集める
					pages.put(areaCode,1);
					pages.put(bsCode,1);
					break;
				}
			}
		}

		// トップの下に局ごとのリストを生やす
		for ( String ac : pages.keySet() ) {
			for ( Center cr : crlist ) {
				if ( cr.getOrder() <= 0 ) {
					// 設定上無効な局はいらない
					continue;
				}
				if ( ac.equals(cr.getAreaCode()) ) {
					ProgList pl = new ProgList();
					pl.Area = cr.getAreaCode();
					pl.SubArea = cr.getType();
					pl.Center = cr.getCenter();
					pl.BgColor = cr.getBgColor();
					pl.enabled = true;

					newplist.add(pl);
				}
			}
		}

		// 局の下に日付ごとのリストを生やす
		GregorianCalendar cal = CommonUtils.getCalendar(0);
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
		boolean has4k = hasBS4K();

		int counterMax = 0;
		for ( String ac : pages.keySet() ) {
			if ( ac.equals(bsCode) ) {
				continue;
			}
			counterMax += pages.get(ac)*getDogDays();
		}
		if (has4k)
			counterMax += getDogDays();

		clrCookie();

		// 日付の下に番組情報ごとのリストを生やす
		int counter = 1;
		for ( String ac : pages.keySet() ) {
			if ( ac.equals(bsCode) ) {
				continue;
			}
			cale = (GregorianCalendar) cal.clone();
			for ( int i=0; i<getDogDays(); i++ ) {
				String adate = CommonUtils.getDateYMD(cale);
				cale.add(Calendar.DATE, 1);
				String edate = CommonUtils.getDateYMD(cale);
				if (!_loadProgram(ac, adate, edate, force, i, counter++, counterMax)){
					newplist = null;
					return false;
				}

				if (isCancelRequested()){
					newplist = null;
					reportProgress(ERRID+"中止要求があったので番組表の取得にを中止します。");
					return false;
				}
			}
		}

		// 4K/8Kの放送局を含む場合別途番組情報を取得する
		if (has4k){
			cale = (GregorianCalendar) cal.clone();
			for ( int i=0; i<getDogDays(); i++ ) {
				String adate = CommonUtils.getDateYMD(cale);
				cale.add(Calendar.DATE, 1);
				String edate = CommonUtils.getDateYMD(cale);
				if (!_loadProgram4K(adate, edate, force, i, counter++, counterMax)){
					newplist = null;
					return false;
				}

				if (isCancelRequested()){
					newplist = null;
					reportProgress(ERRID+"中止要求があったので番組表の取得にを中止します。");
					return false;
				}
			}
		}

		// 開始・終了日時を正しい値に計算しなおす
		for ( ProgList pl : newplist ) {
			setAccurateDate(pl.pdate);
		}

		// 古いデータから補完できないかな？
		CompensatesPrograms(newplist);

//		// 解析用
//		{
//			for ( String f : nf.keySet() ) {
//				System.out.println(String.format(DBGID+"未定義のフラグです: [%s]",f));
//			}
//		}

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
	 * 非公開メソッド
	 */

	protected boolean _loadProgram(String areacode, String adate, String edate, boolean force, int wdaycol, int counter, int counterMax) {
		//
		final String progCacheFile = getProgCacheFile(areacode,adate);
		String dt = adate.substring(6,8);
		String aname = getArea(areacode);
		String targetCache = "["+dt+"日/"+aname+"]: ("+counter+"/"+counterMax+") "+progCacheFile;
		//
		try {
			//
			String response = null;
			String prefix = null;
			File f = new File(progCacheFile);
			boolean bFileAvailable = CommonUtils.isFileAvailable(f,10);
			if (force || !f.exists() || !bFileAvailable || isCacheOld(progCacheFile)){
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(new Date());

				String hh = String.valueOf(CommonUtils.getCalendar(0).get(Calendar.HOUR_OF_DAY));
				String tvParam = "1%2C1%2C1%2CDR%2C10%2C1%2C1%2C1%2C1%2C1%2C1";
				String url = getUrl();
				String pstr = String.format("c_time=%s&win_id=P4501&isLogin=1&start_time=%s0500&end_time=%s0500&ch_type=%s&args=%s0500%s%s%s",
						CommonUtils.getDateTimeYMD(c),
						adate,
						edate,
						getChType(),
						adate,
						//"%2C5%2C",
						"%2C"+hh+"%2C",
						getChType(),
						"%2C0%2C"+tvParam);

				// 本体
				//clrCookie(); -> もっと上に
				addCookie("KEY_AREA", areacode);
				addCookie("tvParam", tvParam);
				//addCookie("CookiePcDmSnsButton", "0");
				//addCookie("__utma", "95544457.1095879584.1348394165.1352897699.1352948391.32");
				//addCookie("__utmz", "95544457.1348394165.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)");
				response = webToBuffer(
						url,
						pstr,
						null,
						url,
						thisEncoding,
						false);

				String targetOnline = "["+dt+"日/"+aname+"]: ("+counter+"/"+counterMax+") "+url+"?"+pstr+"("+areacode+")";
				if ( response != null ){
					reportProgress(MSGID+"番組表(オンライン)を取得しました" + targetOnline);
					prefix = "<!-- "+url+"?"+pstr+" -->\n";
					setCacheFileOnly(false);
				}
				else{
					reportProgress(ERRID+"番組表(オンライン)の取得に失敗しました" + targetOnline);
					if (f.exists() == false)
						return false;
					reportProgress(MSGID+"番組表(キャッシュ)を代わりに使用します: "+targetCache);
				}
			}

			if (response == null){
				if (bFileAvailable) {
					// キャッシュファイルの読み込み
					response = CommonUtils.read4file(progCacheFile, false);
					if ( response == null ) {
						reportProgress(ERRID+"番組表(キャッシュ)の取得に失敗しました"+targetCache);
						return false;
					}
					reportProgress(MSGID+"番組表(キャッシュ)を取得しました"+targetCache);
				}
				else {
					reportProgress(ERRID+"番組表(キャッシュ)がみつかりません"+targetCache);
					return false;
				}
			}

			// 番組リストの追加
			if (!getPrograms(areacode, wdaycol, response)){
				reportProgress(ERRID+"番組表に有効なエントリがありません"+targetCache);
//				return false;
			}

			if ( prefix != null ){
				if ( CommonUtils.write2file(progCacheFile, prefix+response) ) {
					reportProgress(MSGID+"番組表(キャッシュ)に保存しました" + targetCache);
				}
				else{
					reportProgress(ERRID+"番組表(キャッシュ)の保存に失敗しました: "+targetCache);
				}

				setCacheFileOnly(false);
			}
		}
		catch (Exception e) {
			reportProgress(ERRID+"番組表の取得で例外が発生しました： "+e.toString());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/*
	 * BS 4K/8Kの番組情報を取得する
	 */
	protected boolean _loadProgram4K(String adate, String edate, boolean force, int wdaycol, int counter, int counterMax) {
		//
		String areacode = "bs4k";
		String aname = "BS4K";
		final String progCacheFile = getProgCacheFile(areacode,adate);
		String dt = adate.substring(6,8);
		String targetCache = "["+dt+"日/"+aname+"]: ("+counter+"/"+counterMax+") "+progCacheFile;
		//
		try {
			//
			String response = null;
			String prefix = null;
			File f = new File(progCacheFile);
			boolean bFileAvailable = CommonUtils.isFileAvailable(f,10);
			if (force || !f.exists() || !bFileAvailable || isCacheOld(progCacheFile)){
				GregorianCalendar c = new GregorianCalendar();
				c.setTime(new Date());

				String hh = String.valueOf(CommonUtils.getCalendar(0).get(Calendar.HOUR_OF_DAY));
				String tvParam = "1%2C1%2C1%2CDR%2C10%2C1%2C1%2C1%2C1%2C1%2C1";
				String url = getUrl();
				String pstr = String.format("c_time=%s&win_id=P4501&isLogin=1&start_time=%s0500&end_time=%s0500&ch_type=%s&args=%s0500%s%s%s",
						CommonUtils.getDateTimeYMD(c),
						adate,
						edate,
						getChType4K(),
						adate,
						"%2C"+hh+"%2C",
						getChType4K(),
						"%2C0%2C"+tvParam);

				// 本体
				//clrCookie(); -> もっと上に
				addCookie("tvParam", tvParam);
				response = webToBuffer(
						url,
						pstr,
						null,
						url,
						thisEncoding,
						false);

				String targetOnline = "["+dt+"日/"+aname+"]: ("+counter+"/"+counterMax+") "+url+"?"+pstr+"("+areacode+")";
				if ( response != null ){
					prefix = "<!-- "+url+"?"+pstr+" -->\n";
					reportProgress(MSGID+"番組表(オンライン)を取得しました" + targetOnline);
					setCacheFileOnly(false);
				}
				else{
					reportProgress(ERRID+"番組表(オンライン)の取得に失敗しました" + targetOnline);
					if (f.exists() == false)
						return false;

					reportProgress(MSGID+"番組表(キャッシュ)を代わりに使用します: "+targetCache);
				}

			}

			if (response == null){
				if (bFileAvailable) {
					// キャッシュファイルの読み込み
					response = CommonUtils.read4file(progCacheFile, false);
					if ( response == null ) {
						reportProgress(ERRID+"番組表(キャッシュ)の取得に失敗しました"+targetCache);
						return false;
					}
					reportProgress(MSGID+"番組表(キャッシュ)を取得しました"+targetCache);
				}
				else {
					reportProgress(ERRID+"番組表(キャッシュ)がみつかりません"+targetCache);
					return false;
				}
			}

			// 番組リストの追加
			if (!getPrograms(bsCode, wdaycol, response)){
				reportProgress(ERRID+"番組表に有効なエントリがありません"+targetCache);
				return false;
			}

			if (prefix != null){
				if ( CommonUtils.write2file(progCacheFile, prefix+response) ) {
					reportProgress(MSGID+"番組表(キャッシュ)に保存しました" + targetCache);
				}
				else{
					reportProgress(ERRID+"番組表(キャッシュ)の保存に失敗しました: "+targetCache);
				}
			}
		}
		catch (Exception e) {
			reportProgress(ERRID+"番組表の取得で例外が発生しました： "+e.toString());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	//
	private boolean getPrograms(String areacode, int wdaycol, String src) {

		int count = 0;

		for ( Center cr : crlist ) {

			if ( ! cr.getAreaCode().equals(areacode) && ! cr.getAreaCode().equals(bsCode) ) {
				continue;
			}

			Matcher ma = Pattern.compile(String.format("<div\\s+id=\"%s\">([\\s\\S]*?)</div>",cr.getLink())).matcher(src);
			if ( ! ma.find() ) {
				continue;
			}

			ProgDateList pcl = null;
			for ( ProgList pl : newplist ) {
				if ( (pl.Area.equals(areacode)||pl.Area.equals(bsCode)) && pl.Center.equals(cr.getCenter()) ) {
					pcl = pl.pdate.get(wdaycol);
					break;
				}
			}
			if ( pcl == null || pcl.row > 0 ) {
				// 複数地域を同時に選択していると、BSの番組情報が重複して取得されるので、既得(pcl.row>0)ならスキップ
				continue;
			}

			String sdat = String.format("%s %02d:00",pcl.Date,getTimeBarStart());
			GregorianCalendar cx = CommonUtils.getCalendar(sdat);
			GregorianCalendar cy = (GregorianCalendar) cx.clone();
			cy.add(Calendar.DAY_OF_MONTH, 1);

			GregorianCalendar pcz = null;
			Matcher mb = Pattern.compile("([\\s\\S]+?)\\|").matcher(ma.group(1));
			while ( mb.find() && pcl.row < 1440 ) {

				String[] dat = mb.group(1).split(",");

				ProgDetailList pdl = new ProgDetailList();

				// 開始・終了日時
				GregorianCalendar ca = CommonUtils.getCalendar(dat[1]);
				if ( ca == null ) {
					continue;
				}
				if ( ca.compareTo(cy) >= 0 ) {
					// なんか翌日付のデータが混じってるぞ？
					continue;
				}
				pdl.start = CommonUtils.getTime(ca);

				GregorianCalendar cz = CommonUtils.getCalendar(dat[2]);
				if ( cz == null ) {
					continue;
				}
				pdl.end = CommonUtils.getTime(cz);

				pdl.length = (int)(CommonUtils.getDiffDateTime(dat[1], dat[2])/60000L);

				if ( pcz == null ) {
					if ( cz.compareTo(cx) <= 0 ) {
						// 終了時刻が05:00ちょうど
						continue;
					}
					if ( ca.compareTo(cx) <= 0 ) {
						// 開始時刻が05:00よりまえ
						pdl.length = (int)(CommonUtils.getDiffDateTime(sdat, dat[2])/60000L);
					}
					else if ( cx.compareTo(ca) < 0 ) {
						// 開始時刻が05:00よりあと
						addEmptyInfo(pcl, sdat, dat[1]);
					}
				}
				else {
					if ( pcz.compareTo(ca) < 0 ) {
						// 前の番組との間が空いている
						addEmptyInfo(pcl, CommonUtils.getDateTime(pcz), dat[1]);
					}
				}

				pcz = (GregorianCalendar) cz.clone();

				// タイトル＆番組詳細
				pdl.title = CommonUtils.unEscape(dat[3]);

				dat[4] = CommonUtils.unEscape(dat[4]).trim();
				dat[5] = CommonUtils.unEscape(dat[5]).trim();
				dat[6] = CommonUtils.unEscape(dat[6]).trim();
				pdl.detail  =
						(((dat[4].length()>0)?(dat[4]+DETAIL_SEP):(""))
						+((dat[5].length()>0)?(dat[5]+DETAIL_SEP):(""))
						+dat[6]);
				pdl.detail = pdl.detail.replaceAll("\\^", " ");
				// 改行が挟まっているサブタイトルを１行にしたい
				pdl.detail = Pattern.compile("(#\\s*\\d+\\s*「[^」]*?)[\r\n]+[ 　]*(.*?」)",Pattern.DOTALL).matcher(pdl.detail).replaceAll("$1 $2");

				// タイトルから各種フラグを分離する
				doSplitFlags(pdl, nf);

				// ジャンル
				{
					ArrayList<String> genrelist = new ArrayList<String>();
					Matcher md = Pattern.compile("([0-9A-F][0-9A-F])").matcher(dat[7]);
					while ( md.find() ) {
						genrelist.add(md.group(1));
					}
					setMultiGenre(pdl,genrelist);
				}

				// サブタイトル分離
				doSplitSubtitle(pdl);

				// 検索対象外領域にこっそりジャンル文字列を入れる
				pdl.setGenreStr();

				// プログラムID？
				String chid =  ContentIdDIMORA.getChId(cr.getLink());
				ContentIdDIMORA.decodeChId(chid);
				pdl.progid = ContentIdDIMORA.getContentId(0,dat[8]);

				// その他フラグ
				pdl.extension = false;
				//pdl.flag = ProgFlags.NOFLAG;
				pdl.nosyobo = false;

				//
				pcl.pdetail.add(pdl);

				//
				pcl.row += pdl.length;

				count++;
			}
			if ( pcz == null ) {
				// 番組情報がないよ
				addEmptyInfo(pcl, CommonUtils.getDateTime(cx), CommonUtils.getDateTime(cy));
			}
			else if ( pcz.compareTo(cy) < 0 ) {
				// 終了時刻が29:00より前
				addEmptyInfo(pcl, CommonUtils.getDateTime(pcz), CommonUtils.getDateTime(cy));
			}
		}

		return count > 0;
	}

	/*
	 * ＢＳ４Ｋ／８Ｋの放送局が選択されているか
	 */
	private boolean hasBS4K(){
		for (Center c : crlist){
			if (!c.getAreaCode().equals(bsCode))
				continue;
			if (c.getOrder() <= 0)
				continue;

			if (c.getLink().startsWith("2"))
				return true;
		}

		return false;
	}

	/*******************************************************************************
	 * 地域情報を取得する
	 ******************************************************************************/

	// 普通は東京
	@Override
	public String getDefaultArea() {return "東京";}

	//
	@Override
	public void loadAreaCode() {

		// 設定ファイルが存在していればファイルから
		File f = new File(getAreaSelectedFile());
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			ArrayList<AreaCode> tmp = (ArrayList<AreaCode>) CommonUtils.readXML(getAreaSelectedFile());
			if ( tmp != null ) {
				aclist = tmp;
				System.out.println(MSGID+"地域リストを読み込みました: "+getAreaSelectedFile());
				return;
			}
			else {
				System.err.println(ERRID+"地域リストの読み込みに失敗しました: "+getAreaSelectedFile());
			}
		}

		// 存在していなければWeb上から
		String uri = getSiteUrl();
		String response = webToBuffer(uri, thisEncoding, true);;
		if ( response == null ) {
			System.err.println(ERRID+"地域情報の取得に失敗しました: "+uri);
			return;
		}

		// 地域一覧の作成
		ArrayList<AreaCode> newaclist = new ArrayList<AreaCode>();

		Matcher ma = Pattern.compile("<div\\s+id=\"includeAreaList\">(.+?)</div>").matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("(.+?),(.+?)\\|").matcher(ma.group(1));
			while ( mb.find() ) {
				AreaCode ac = new AreaCode();
				ac.setArea(mb.group(2));
				ac.setCode(mb.group(1));
				newaclist.add(ac);
			}
		}

		if ( newaclist.size() == 0 ) {
			System.err.println(ERRID+"地域一覧の取得結果が０件だったため情報を更新しません");
			return;
		}

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

		aclist = newaclist;
		saveAreaCode();
	}


	/*******************************************************************************
	 * 放送局情報を取得する
	 ******************************************************************************/

	// 設定ファイルがなければWebから取得
	@Override
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

				System.out.println(MSGID+"放送局リストを読み込みました: "+centerListFile);
				return;
			}
			else {
				System.err.println(ERRID+"放送局リストの読み込みに失敗しました: "+centerListFile);
			}
		}

		// Web上から放送局の一覧を取得する
		ArrayList<Center> newcrlist = new ArrayList<Center>();

		int cntMax = ((code.equals(allCode))?(aclist.size()-2):(1));
		int cnt = 1;

		if ( code.equals(allCode) ) {
			for ( AreaCode ac : aclist ) {
				if ( ! ac.getCode().equals(allCode) && ! ac.getCode().equals(bsCode) ) {
					_loadCenter(newcrlist,ac.getCode(),(cnt!=cntMax));
					reportProgress(MSGID+"放送局情報を取得しました: ("+cnt+"/"+cntMax+") ");
					cnt++;
				}
			}
		}
		else if ( ! code.equals(bsCode) ) {
			_loadCenter(newcrlist,code,false);
			reportProgress(MSGID+"放送局情報を取得しました: ("+cnt+"/"+cntMax+") ");
			cnt++;
		}

		_loadCenter4K(newcrlist);
		reportProgress(MSGID+"BS4K/8K放送局情報を取得しました");

		if ( newcrlist.size() == 0 ) {
			System.err.println(ERRID+"放送局情報の取得結果が０件だったため情報を更新しません");
			return;
		}

		crlist = newcrlist;
		attachChFilters();	// 放送局名変換
		saveCenter();
	}

	private boolean _loadCenter(ArrayList<Center> newcrlist, String code, boolean bsexist) {
		// 本体
		String uri = getUrl();

		GregorianCalendar c = CommonUtils.getCalendar(0);
		String cdate = CommonUtils.getDateTimeYMD(c);
		String adate = CommonUtils.getDateYMD(c);
		c.add(Calendar.DAY_OF_MONTH, 1);
		String edate = CommonUtils.getDateYMD(c);

		clrCookie();
		addCookie("KEY_AREA", code);
		String response = webToBuffer(
				uri,
				String.format("c_time=%s&win_id=P4501&isLogin=1&start_time=%s0500&end_time=%s0500&ch_type=%s&args=%s0500%s%s%s",
						cdate,
						adate,
						edate,
						getChType(),
						adate,
						"%2C5%2C",
						getChType(),
						"%2C0%2C0%2C1%2C1%2CDR%2C10%2C1%2C1%2C1%2C1%2C1%2C1"),
				null,
				getUrl(),
				thisEncoding,
				true);
		if ( response == null ) {
			reportProgress(ERRID+"番組表の取得に失敗しました: "+uri);
			return false;
		}

		//CommonUtils.write2file("TMP.htm", response);

		String expr = String.format("<div\\s+id=\"%s\"\\s*>(.+?)</div>", getCenterInfoId());
		Matcher ma = Pattern.compile(expr).matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("(.+?),.+?,.+?,(.+?),(.+?),.+?\\|").matcher(ma.group(1));
			while ( mb.find() ) {
				String centerName = CommonUtils.unEscape(mb.group(3));
				String chNo = mb.group(2);
				String centerId = mb.group(1);

				String areacode = getCenterCode(centerId,code);
				if ( bsexist && areacode.equals(bsCode) ) {
					// 重複するＢＳは排除
					continue;
				}

				// NHK関連
				if ( areacode.equals(bsCode) ) {
					if ( centerName.startsWith("NHK") ) {
						centerName = String.format("%s(%s)", centerName, chNo);
					}
				}
				else {
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

				// 重複する放送局は追加しない
				boolean found=false;
				for (Center cr2 : newcrlist){
					if (areacode.equals(cr2.getAreaCode()) && centerName.equals(cr2.getCenterOrig())){
						found = true;
						break;
					}
				}
				if (found)
					continue;

				Center cr = new Center();
				cr.setAreaCode(areacode);
				cr.setCenterOrig(centerName);
				cr.setLink(centerId);
				cr.setType("");
				cr.setEnabled(true);
				if ( response.matches(String.format(".*<div\\s+id=\"%s\">.*",centerId)) ) {
					newcrlist.add(cr);
				}
			}
		}

		return true;
	}

	/*
	 * BS4K/8Kの放送局を取得する
	 */
	private boolean _loadCenter4K(ArrayList<Center> newcrlist) {
		// 本体
		String uri = getUrl();
		String areacode = bsCode;

		GregorianCalendar c = CommonUtils.getCalendar(0);
		String cdate = CommonUtils.getDateTimeYMD(c);
		String adate = CommonUtils.getDateYMD(c);
		c.add(Calendar.DAY_OF_MONTH, 1);
		String edate = CommonUtils.getDateYMD(c);

		clrCookie();
		String response = webToBuffer(
				uri,
				String.format("c_time=%s&win_id=P4501&isLogin=1&start_time=%s0500&end_time=%s0500&ch_type=%s&args=%s0500%s%s%s",
						cdate,
						adate,
						edate,
						getChType4K(),
						adate,
						"%2C3%2C",
						getChType4K(),
						"%2C0%2C1%2C1%2C1%2CDR%2C10%2C1%2C1%2C1%2C1%2C1%2C1"),
				null,
				getUrl(),
				thisEncoding,
				true);
		if ( response == null ) {
			reportProgress(ERRID+"番組表の取得に失敗しました: "+uri);
			return false;
		}

		//CommonUtils.write2file("TMP.htm", response);

		String expr = String.format("<div\\s+id=\"%s\"\\s*>(.+?)</div>", getCenterInfoId4K());
		Matcher ma = Pattern.compile(expr).matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("(.+?),.+?,.+?,(.+?),(.+?),.+?\\|").matcher(ma.group(1));
			while ( mb.find() ) {
				String centerName = CommonUtils.unEscape(mb.group(3));
//				String chNo = mb.group(2);
				String centerId = mb.group(1);

				// 一部の放送局がＢＳと同じ名前のため、４Ｋ／８Ｋが必ず付くようにする
				Matcher mc = Pattern.compile("(４Ｋ|８Ｋ|4K|8K)").matcher(centerName);
				if (!mc.find()){
					centerName += "　４Ｋ";
				}

				// 重複する放送局は追加しない
				boolean found=false;
				for (Center cr2 : newcrlist){
					if (areacode.equals(cr2.getAreaCode()) && centerName.equals(cr2.getCenterOrig())){
						found = true;
						break;
					}
				}
				if (found)
					continue;

				Center cr = new Center();
				cr.setAreaCode(areacode);
				cr.setCenterOrig(centerName);
				cr.setLink(centerId);
				cr.setType("");
				cr.setEnabled(true);
				if ( response.matches(String.format(".*<div\\s+id=\"%s\">.*",centerId)) ) {
					newcrlist.add(cr);
				}
			}
		}

		return true;
	}
}
