package tainavi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgScrumble;
import tainavi.TVProgram.ProgSubgenre;


/**
 * {@link TVProgram}インタフェース をインプルメントしたWeb番組表プラグインのクラスで利用できる、共有部品の集合です。
 */
public class TVProgramUtils implements Cloneable {

	/*******************************************************************************
	 * ディープコピーが意外と大変
	 ******************************************************************************/

	@Override
	public TVProgramUtils clone() {
		try {
			TVProgramUtils p = (TVProgramUtils) super.clone();

			// フィールドコピーしてもらいたくないもの
			p.pcenter = null;

			// static化したのでコピー抑制を必要としなくなったものたち
			//p.setProgressArea(null);
			//p.setChConv(null);

			FieldUtils.deepCopy(p, this); // ディープコピーするよ

			p.pcenter = new ArrayList<ProgList>();

			/*
			// 地域設定をコピー
			p.aclist = new ArrayList<AreaCode>();
			for ( AreaCode ac : aclist ) {
				p.aclist.add(ac.clone());
			}

			// 放送局設定をコピー
			p.crlist = new ArrayList<Center>();
			for ( Center cr : crlist ) {
				p.crlist.add(cr.clone());
			}
			*/

			p.setSortedCRlist();

			return p;

		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}


	/*******************************************************************************
	 * 共通情報
	 ******************************************************************************/

	/**
	 * Proxy
	 */
	public static boolean setProxy(String host, String port) {
		Proxy newproxy = null;
		if ( host != null ) {
			try {
				newproxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.valueOf(port)));
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		proxy = newproxy;
		return true;
	}

	private Proxy getProxy() { return proxy; }

	private static Proxy proxy = null;

	/**
	 * ChannelConvert.dat
	 */
	public static void setChConv(ChannelConvert cc) { chconv = cc; }

	private ChannelConvert getChConv() { return chconv; }

	private static ChannelConvert chconv = null;

	/**
	 *  ログと進捗ダイアログ
	 */
	public static void setProgressArea(StatusWindow o) { stw = o; }
	public static void setProgressAreaBackground(StatusTextArea o){ mwinBackground = o; }

	public static void reportProgress(String msg) {
		if (mwinBackground != null){
			mwinBackground.appendMessage(msg);
		}
		else if (stw != null) {
			stw.append(msg);
		}
		System.out.println(msg);
	}

	private static StatusWindow stw = null;
	private static StatusTextArea mwinBackground = null;

	public static void setCancelRequested(boolean b){ cancelRequested = b; }
	public static boolean isCancelRequested(){
		if (stw != null && stw.isWindowCloseRequested())
			return true;

		return cancelRequested;
	}

	private static boolean cancelRequested = false;

	/*
	 * 高速キャッシュファイルを使用するか
	 */
	public static void setUseProgCache(boolean b){ useProgCache = b;}
	public static boolean getUseProgCache(){ return useProgCache; }
	private static boolean useProgCache = true;

	/*
	 * 高速キャッシュファイルの読み込みを無効化する
	 */
	public static void invalidateProgCache(boolean b){ invalidProgCache = b; }
	private static boolean invalidProgCache = false;

	/*******************************************************************************
	 * オプション確認
	 ******************************************************************************/

	public boolean isAreaSelectSupported() { return true; }		// デフォルトはエリアを選べる


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// コネクションタイムアウト
	private static final int conntout = 5;

	// read()タイムアウト
	private static final int readtout = 30;

	// 番組表が複数ページに跨っている場合、何ページまで追いかけるか
	protected static final int REFPAGESMAX = 10;

	// 番組詳細に情報をconcatする時のセパレータ
	protected final String DETAIL_SEP = "\n\n";

	// メッセージID
	private static final String MSGID = "[番組表共通] ";
	private static final String DBGID = "[DEBUG]"+MSGID;
	private static final String ERRID = "[ERROR]"+MSGID;

	// キャッシュファイルで使用する放送局のターミネータ
	private static final String PLEND = "#PLEND\n";

	/*******************************************************************************
	 * メンバ変数関連
	 ******************************************************************************/

	// デバッグフラグ
	public void setDebug(boolean b) { debug = b; }
	protected boolean getDebug() { return debug; }	// 参照はサブクラスのみに許可
	private boolean debug = false;

	// 通信エラー時のリトライ回数
	protected void setRetryCount(int n) { retrycount = n; }	// サブクラスのみに許可（特殊）
	private int retrycount = 1;

	// User-Agent
	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String s) { userAgent = s; }
	private String userAgent = "";

	// 番組表キャッシュの格納フォルダ
	public String getProgDir() { return progDir; }
	public void setProgDir(String s) { progDir = s; }
	private String progDir = "progcache";

	// 番組表キャッシュの有効時間（Ｈ）
	public int getCacheExpired() { return cacheExpired; }
	public void setCacheExpired(int h) { cacheExpired = h; }
	private int cacheExpired = 6;

	// 番組詳細取得を行なうかどうか
	public boolean getUseDetailCache() { return useDetailCache; }
	public void setUseDetailCache(boolean b) { useDetailCache = b; }
	private boolean useDetailCache = true;

	// 29時跨ぎで２つに分かれた番組情報をくっつけるかどうか
	public boolean getContinueTomorrow() { return continueTomorrow; }
	public void setContinueTomorrow(boolean b) { continueTomorrow = b; }
	private boolean continueTomorrow = false;

	// タイトルから話数以降を分離する
	public void setSplitEpno(boolean b) { splitEpno = b; }
	public boolean isSplitEpno() { return splitEpno; }
	private boolean splitEpno = true;

	// 可能なら８日分取得する
	public boolean getExpandTo8() { return expandTo8; }
	public void setExpandTo8(boolean b) { expandTo8 = b; }
	private boolean expandTo8 = false;

	// 番組詳細キャッシュをオンライン取得するかどうか
	@Deprecated
	protected boolean isForceLoadDetInfo() { return forceLoadDetInfo; }
	@Deprecated
	private boolean forceLoadDetInfo = false;

	// もう使っていないようだ
	//public void setAbnormal(boolean b) { abnormal = b; }
	//public boolean getAbnormal() { return abnormal; }
	//private boolean abnormal = false;

	// 今回の取得でキャッシュのみを使用したか
	public void setCacheFileOnly(boolean b){ cacheFileOnly = b; }
	public boolean getCacheFileOnly(){ return cacheFileOnly; }
	private boolean cacheFileOnly = false;

	/*******************************************************************************
	 * 番組表固有の情報
	 ******************************************************************************/


	/*******************************************************************************
	 * 番組情報
	 ******************************************************************************/

	/**
	 * <P>番組表が格納される場所
	 * <P>上から順の{@link ProgList}(pcenter/放送局別)->{@link ProgDateList}(pdate/日付別)→{@link ProgDetailList}pdetail/(番組詳細)
	 */
	public ArrayList<ProgList> getCenters() { return(pcenter); }
	public ArrayList<ProgList> pcenter = new ArrayList<ProgList>();


	/*******************************************************************************
	 * 放送局情報
	 ******************************************************************************/

	/**
	 * 放送局名リスト
	 * @see ProgList#Center
	 * @see #sortedcrlist
	 */
	public ArrayList<Center> getCRlist() { return(crlist); }
	public ArrayList<Center> crlist = new ArrayList<Center>();

	/**
	 * ソート済み放送局名リスト
	 * @see ProgList#Center
	 * @see #crlist
	 */
	public ArrayList<Center> getSortedCRlist() { return(sortedcrlist); }
	public void setSortedCRlist() {
		sortedcrlist = new ArrayList<Center>();
		for (int order=1; order<=crlist.size(); order++) {
			for (Center center : crlist) {
				if (center.getOrder() == order) {
					sortedcrlist.add(center);
					break;
				}
			}
		}
	}
	public ArrayList<Center> sortedcrlist = new ArrayList<Center>();

	// 設定ファイルへ書き出し
	public boolean saveCenter() {
		String centerListFile = getCenterListFile(getTVProgramId(), getSelectedCode());
		if ( ! CommonUtils.writeXML(centerListFile,crlist) ) {
			System.out.println("放送局リストの保存に失敗しました: "+centerListFile+", "+getSelectedCode());
			return false;
		}
		System.out.println("放送局リストを保存しました: "+centerListFile+", "+getSelectedCode());
		return true;
	}

	// 放送局リストファイル名
	protected String getCenterListFile(String id, String code) {
		return String.format("env%scenter.%s.%s.xml", File.separator, id, code);
	}

	// 放送局名に一括してフィルタをかける
	protected void attachChFilters() {
		for ( Center c : crlist ) {
			if ( c.getCenterOrig() == null ) {
				c.setCenterOrig(c.getCenter());
			}
			c.setCenter(getChConv().get(c.getCenterOrig()));
		}
	}

	// 放送局名個別にフィルタをかけたい
	protected String getChName(String chorig) {
		return getChConv().get(chorig);
	}


	/*******************************************************************************
	 * 地域情報
	 ******************************************************************************/

	/**
	 * 地域コード のリストを取得する
	 */
	public ArrayList<AreaCode> getAClist() { return(aclist); }
	public ArrayList<AreaCode> aclist = new ArrayList<AreaCode>();

	/**
	 * 地域コードから地域名を取得
	 */
	public String getArea(String code) {
		for (AreaCode ac : aclist) {
			if (ac.getCode().equals(code)) {
				return(ac.getArea());
			}
		}
		return(null);
	}

	/**
	 * 地域名から地域コードを取得
	 */
	public String getCode(String area) {
		for (AreaCode ac : aclist) {
			if (ac.getArea().equals(area)) {
				return(ac.getCode());
			}
		}
		return(null);
	}

	/**
	 * 地域名を指定して選択中の地域を変更し、ついでにその地域コードを返す
	 * @see #getSelectedCode()
	 */
	public String setSelectedAreaByName(String area) {
		AreaCode ac = null;
		for (AreaCode atmp : aclist) {
			if (atmp.getArea().equals(area)) {
		        atmp.setSelected(true);
		        ac = atmp;
			}
			else {
		        atmp.setSelected(false);
			}
		}
		if (ac != null) {
			return(ac.getCode());
		}
		return(null);
	}

	/**
	 * 地域コードを指定して選択中の地域を変更し、ついでにその地域名を返す
	 * @see #getSelectedArea()
	 */
	public String setSelectedAreaByCode(String code) {
		AreaCode ac = null;
		for (AreaCode atmp : aclist) {
			if (atmp.getCode().equals(code)) {
		        atmp.setSelected(true);
		        ac = atmp;
			}
			else {
		        atmp.setSelected(false);
			}
		}
		if (ac != null) {
			return(ac.getArea());
		}
		return(null);
	}

	/**
	 * 選択中の地域名を返す
	 */
	public String getSelectedArea() {
		for (AreaCode ac : aclist) {
			if (ac.getSelected() == true) {
				return(ac.getArea());
			}
		}
		return(getDefaultArea());
	}

	/**
	 * 選択中の地域コードを返す
	 */
	public String getSelectedCode() {
        for (AreaCode ac : aclist) {
        	if (ac.getSelected() == true) {
        		return(ac.getCode());
        	}
        }
		return(getCode(getDefaultArea()));
	}

	// 設定ファイルへ書き出し
	public void saveAreaCode() {
		if ( CommonUtils.writeXML(getAreaSelectedFile(), aclist) ) {
			System.out.println(MSGID+"地域リストを保存しました: "+getAreaSelectedFile());
		}
		else {
			System.err.println(ERRID+"地域リストの保存に失敗しました: "+getAreaSelectedFile());
		}
	}

	// 地域情報ファイル名
	protected String getAreaSelectedFile() {
		return String.format("env%sarea.%s.xml", File.separator, getTVProgramId());
	}


	/*******************************************************************************
	 * タイトル操作関連
	 ******************************************************************************/

	/**
	 * 話数重複排除
	 */
	protected String doCutDupEpno(String title, String detail) {
		// タイトルの末尾に話数がついているかな？
		Matcher md = Pattern.compile("[ 　]*?(#[# 　-・0-9]+|第[0-9]+話)$").matcher(title);
		if ( ! md.find() ) {
			return title;
		}

		ArrayList<String> tnoa = new ArrayList<String>();
		{
			Matcher me = Pattern.compile("(\\d+)").matcher(md.group(1));
			while ( me.find() ) {
				tnoa.add(me.group(1));
			}
			if ( tnoa.size() == 0 ) {
				return title;
			}
		}

		// 番組詳細と重複しているかな？
		{
			ArrayList<String> dnoa = new ArrayList<String>();
			Matcher me = Pattern.compile("#[ 　]*([0-9]+)|第([0-9]+)話").matcher(detail);
			while ( me.find() ) {
				if ( me.group(1) != null ) {
					dnoa.add(me.group(1));
				}
				else if ( me.group(2) != null ) {
					dnoa.add(me.group(2));
				}
			}
			if ( dnoa.size() == 0 ) {
				return title;
			}

			for ( String tno : tnoa ) {
				for ( String dno : dnoa ) {
					if ( dno.equals(tno) ) {
						dnoa.remove(dno);
						break;
					}
				}
			}
			if ( dnoa.size() == 0 ) {
				title = md.replaceFirst("");
			}
		}

		return title;
	}

	/**
	 * サブタイトルの分離
	 * @param pdl
	 */
	protected void doSplitSubtitle(ProgDetailList pdl)	{

		pdl.splitted_title = doCutDupEpno(pdl.title, pdl.detail);	// タイトルと番組詳細中の話数の重複排除

		String [] d = doSplitEpno(pdl.genre, pdl.splitted_title);	// 分離！

		pdl.splitted_title = pdl.title.substring(0,d[0].length());

		if ( d[1].length() > 0 ) {
			// 番組詳細はサブタイトル分離番組詳細へのポインタでいいよ
			pdl.splitted_detail = d[1]+DETAIL_SEP+pdl.detail;
			pdl.detail = pdl.splitted_detail.substring(d[1].length()+DETAIL_SEP.length());
		}
		else {
			// サブタイトルが分離されなかったから同じでいいよ
			pdl.splitted_detail = pdl.detail;
		}

		// タイトル＆番組詳細のキーワード検索情報の設定
		String key_title;
		String key_detail;
		if (isSplitEpno()) {
			// サブタイトルを分離するならばそれを考慮した値を使う
			key_title = pdl.splitted_title;
			key_detail = pdl.splitted_detail;
		}
		else {
			key_title = pdl.title;
			key_detail = pdl.detail;
		}
		pdl.titlePop = TraceProgram.replacePop(key_title);
		pdl.detailPop = TraceProgram.replacePop(key_detail);

		// 分離しない場合でも、番組追跡はサブタイトル抜きでの検索ができるようにしたい
		pdl.splitted_titlePop = TraceProgram.replacePop(pdl.splitted_title);
	}

	/**
	 * サブタイトル分離(Part.11 444-)
	 */
	private String[] doSplitEpno(ProgGenre genre, String title) {
		if ( genre == ProgGenre.MOVIE || genre == ProgGenre.DOCUMENTARY ) {
			//　映画とドキュメンタリーは何もしない
		}
		else {
			if ( genre == ProgGenre.DORAMA ) {
				// ドラマの場合は、"「*」"での分割をしない（土曜ドラマ「タイトル」とかあるため）
				Matcher mc = Pattern.compile(SPEP_EXPR_DORAMA).matcher(title);
				if ( mc.find() ) {
					return(new String[] { mc.group(1),mc.group(2)+" " });
				}
			}
			else {
				String ani = "";
				String tit = title;
				Matcher mani = Pattern.compile("^(\\s*(?:TV|ＴＶ)?アニメ\\s*)(.*)$").matcher(title);
				if ( mani.find() ) {
					ani = mani.group(1);
					tit = mani.group(2);
				}

				// いきなり「で始まる場合や、タイトル中に『TOKYO「萌」探偵』のように「ほげほげ」を含む場合
				Matcher mc = Pattern.compile("^([^ 　]*「.+?」[^ 　]+)(.*)$").matcher(tit);
				if (mc.find()) {
					Matcher md = Pattern.compile("^[ 　]*(.*?)[ 　]*?" + SPEP_EXPR).matcher(mc.group(2));
					if (md.find()) {
						if (md.group(1).length() == 0) {
							return (new String[]{ani+mc.group(1), md.group(2) + " "});
						} else {
							return (new String[]{ani+mc.group(1) + " " + md.group(1), md.group(2) + " "});
						}
					} else {
						return (new String[]{ani+tit, ""});
					}
				}
				mc = Pattern.compile("^(.+?)[ 　]*?" + SPEP_EXPR).matcher(tit);
				if (mc.find()) {
					return (new String[]{ani+mc.group(1), mc.group(2) + " "});
				}
			}
		}
		return(new String[] { title,"" });
	}

	// サブタイトル判定条件
	private static final String SPEP_EXPR = "(([<＜]?[(（#＃♯第全「][第]?[1234567890１２３４５６７８９０一二三四五六七八九十百千]+?[回話章]?|「).*)$";

	// サブタイトル判定条件（ジャンル＝ドラマ専用）
	private static final String SPEP_EXPR_DORAMA = "^(.+?)[ 　]*?(([<＜]?[(（#＃♯第全「][第]?[1234567890１２３４５６７８９０一二三四五六七八九十百千]+?[回話章]?).*)$";


	/**
	 * NGワード
	 */
	public void abon(ArrayList<String> ngword) {
		//
		if (ngword.size() == 0) {
			return;
		}
		for ( ProgList p : pcenter ) {
			for ( ProgDateList c : p.pdate ) {
				for (ProgDetailList d : c.pdetail) {
					for (String ngs : ngword) {
						if (d.title.indexOf(ngs) != -1 || d.detail.indexOf(ngs) != -1) {
							d.abon();
							break;
						}
					}
				}
			}
		}
	}



	/*******************************************************************************
	 * 番組情報キャッシュ
	 ******************************************************************************/

	// キャッシュファイルが有効期限内か確認する
	public boolean isCacheOld(String fname) {
		// キャッシュ期限が無期限の場合はＤＬ禁止
		if (cacheExpired == 0) {
			return(false);
		}
		// キャッシュ期限があってファイルがない場合はＤＬしなさい
		//if (cacheExpired > 0 && fname == null) {
		if (fname == null) {	// あれだ、期限に関係なくファイル名の指定がなきゃＤＬするしかないよ
			return(true);
		}
		// 実際のファイルのタイムスタンプを確認する
		try {
			File f = new File(fname);
			if (f.exists() == true) {
				long t = System.currentTimeMillis();
				if (f.lastModified() < (t - cacheExpired*3600000L) ) {
					// ファイルを更新
					f.delete();
					f.createNewFile();
					return(true);
				}
			}
			else {
				// ファイルを作成
				f.createNewFile();
				return(true);
			}
		}
		catch (Exception e) {
			// 例外
			System.out.println("Exception: isCacheOld() "+e);
		}
		return(false);
	}


	/*******************************************************************************
	 * 番組詳細キャッシュ（現在は使われていない）
	 ******************************************************************************/

	/**
	 * 番組詳細をオンライン取得するかどうか
	 */
	@Deprecated
	protected void chkForceLoadDetInfo(boolean force) {
		File f = new File(getDetCacheFile());
		if (force == true ||
				(f.exists() == true && isCacheOld(getDetCacheFile()) == true) ||
				(f.exists() == false && isCacheOld(null) == true)) {
			// Webからロードします
			forceLoadDetInfo = true;
		}
		else if (f.exists()) {
			// キャッシュファイルが有効なようなので利用します
			forceLoadDetInfo = false;
		}
		else {
			// 無くても平気…
			forceLoadDetInfo = false;
		}
	}

	// 番組詳細キャッシュのLOAD
	@Deprecated
	protected HashMap<String,String> loadDetCache(){

		// 設定ファイルが存在していればファイルから
		File f = new File(getDetCacheFile());
		if (f.exists() == true) {
			@SuppressWarnings("unchecked")
			HashMap<String,String> cache = (HashMap<String, String>) CommonUtils.readXML(getDetCacheFile());
			if ( cache != null ) {
				return cache;
			}

			System.out.println(ERRID+"【致命的エラー】番組詳細キャッシュが読み込めません: "+getDetCacheFile());
		}

		// キャッシュなし＆エラーは空配列を返す
		return new HashMap<String, String>();
	}

	// 番組詳細キャッシュのSAVE
	@Deprecated
	protected void saveDetCache(HashMap<String,String> cache) {
		if ( ! CommonUtils.writeXML(getDetCacheFile(), cache) ) {
			System.err.println(ERRID+"【致命的エラー】番組詳細キャッシュが書き込めません: "+getDetCacheFile());
		}
	}


	/*******************************************************************************
	 * 番組情報のリフレッシュ関連
	 ******************************************************************************/

	/**
	 * 日付変更線(29:00)をまたいだら過去のデータはカットする
	 * <B> PassedProgramでは使わない
	 */
	public void refresh() {

		String critDate = CommonUtils.getDate529(0, true);
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

	/**
	 * ２４時間分番組枠が埋まっているかどうか確認する
	 */
	public String chkComplete() {
		for ( ProgList p : pcenter ) {
			if (p.enabled) {
				for ( ProgDateList c : p.pdate ) {
					if (c.pdetail.size()<=1) {
						String msg = "番組枠が存在しません.("+p.Center+","+c.Date+")";
						System.out.println(msg);
						return(msg);
					}
					if (c.row < 24*60) {
						String msg = "番組枠が２４時間分取得できませんでした.("+p.Center+","+c.Date+","+c.row+")";
						System.out.println(msg);
						return(msg);
					}
				}
				if (p.pdate.size() < 7) {
					String msg = "番組表が一週間分取得できませんでした.("+p.Center+")";
					System.out.println(msg);
					return(msg);
				}
			}
		}
		return null;
	}

	/**
	 * 開始終了日時の整理
	 */
	public void setAccurateDate(ArrayList<ProgDateList> pcenter) {

		// 先頭のエントリの開始時刻が 5:00 以前の場合
		for ( ProgDateList pcl : pcenter ) {
			if (pcl.pdetail.size() <= 0) {
				continue;
			}

			ProgDetailList pd = pcl.pdetail.get(0);
			Matcher ma = Pattern.compile("(\\d\\d):(\\d\\d)").matcher(pd.start);
			if (ma.find()) {
				int prelength = 0;
				int ahh = Integer.valueOf(ma.group(1));
				int amm = Integer.valueOf(ma.group(2));

				GregorianCalendar c = new GregorianCalendar();
				c.setTime(new Date());
				c.set(Calendar.HOUR_OF_DAY, ahh);
				c.set(Calendar.MINUTE, amm);

				if ( pd.start.compareTo("05:00") < 0 ) {
					// 5:00以前
					prelength = (5*60+0)-(ahh*60+amm);
					c.add(Calendar.MINUTE,prelength+pd.length);
					pd.end = CommonUtils.getTime(c);
				}
				else if ( pd.start.compareTo("18:00") >= 0 && pd.start.compareTo("24:00") < 0 ) {
					// 前日の24:00以前
					prelength = (24*60+0)-(ahh*60+amm)+(5*60);
					c.add(Calendar.MINUTE,prelength+pd.length);
					pd.end = CommonUtils.getTime(c);
				}
			}
		}

		for ( ProgDateList pcl : pcenter ) {

			GregorianCalendar c = CommonUtils.getCalendar(pcl.Date);

			boolean extend = false;
			boolean overtwodays = false;
			for ( int i=0; i<pcl.pdetail.size(); i++ ) {

				ProgDetailList pdl = pcl.pdetail.get(i);

				// 番組情報がありません
				if (pdl.start.compareTo("") == 0) {
					continue;
				}

				// 表示上の開始日時
				if ( i == 0 ) {
					if ( pdl.start.compareTo("18:00") >= 0 && pdl.start.compareTo("24:00") < 0 ) {
						// いったい何時間放送するんだよ(--#
						c.add(Calendar.DAY_OF_MONTH, -1);
						overtwodays = true;
					}
				}
				else {
					if ( (pdl.start.compareTo("00:00") >= 0 && pdl.start.compareTo("05:00") < 0 && pdl.end.compareTo("05:00") < 0) && extend == false) {
						c.add(Calendar.DAY_OF_MONTH, 1);
						extend = true;
					}
				}
				pdl.startDateTime = String.format("%s %s", CommonUtils.getDate(c,false), pdl.start);

				// 正確な開始日
				pdl.accurateDate = CommonUtils.getDate(c);

				// 表示上の終了日時
				if ( overtwodays ) {
					c.add(Calendar.DAY_OF_MONTH, 1);
					overtwodays = false;
				}
				else {
					if ( pdl.start.compareTo(pdl.end) > 0  && extend == false) {
						c.add(Calendar.DAY_OF_MONTH, 1);
						extend = true;
					}
				}

				pdl.endDateTime = String.format("%s %s", CommonUtils.getDate(c,false), pdl.end);
			}
		}

		// 29時をまたいで同タイトルが続いている場合は同一番組とみなす
		if ( continueTomorrow ) {
			for (int w=0; w<pcenter.size()-1; w++) {
				if (pcenter.get(w).pdetail.size() > 0 && pcenter.get(w+1).pdetail.size() > 0) {
					ProgDetailList pd1 = pcenter.get(w).pdetail.get(pcenter.get(w).pdetail.size()-1);
					ProgDetailList pd2 = pcenter.get(w+1).pdetail.get(0);
					if (pd1.title.equals(pd2.title)) {
						pd1.end = pd2.end;
						pd1.endDateTime = pd2.endDateTime;

						pd2.start = pd1.start;
						pd2.startDateTime = pd1.startDateTime;
						pd2.accurateDate = pd1.accurateDate;
					}
					else if (pd2.title.equals("承前")) {
						pd1.end = pd2.end;
						pd1.endDateTime = pd2.endDateTime;

						pd2.start = pd1.start;
						pd2.startDateTime = pd1.startDateTime;
						pd2.accurateDate = pd1.accurateDate;

						pd2.title = pd1.title;
						pd2.detail = pd1.detail;
						pd2.setAddedDetail(pd1.getAddedDetail());
						pd2.link = pd1.link;
						pd2.titlePop = pd1.titlePop;
						pd2.detailPop = pd1.detailPop;
						pd2.nosyobo = pd1.nosyobo;
						pd2.extension = pd1.extension;
						pd2.flag = pd1.flag;
						pd2.genre = pd1.genre;
					}
				}
			}
		}
	}

	// 以前に取得したデータから当日の取得不能領域のデータを補完する
	protected void CompensatesPrograms(ArrayList<ProgList> newplist) {
		//
		for ( ProgList newpl : newplist ) {

			if ( newpl.enabled != true ) {
				// 無効局は処理なし
				continue;
			}

			ArrayList<ProgDateList> newpcla = newpl.pdate;
			if ( newpcla.size() == 0 ) {
				// 日付リストが存在しない場合は処理なし
				continue;
			}
			ProgDateList newpcl = newpcla.get(0);

			ArrayList<ProgDetailList> newpdla = newpcl.pdetail;
			if ( newpdla.size() == 0 ) {
				// 番組情報が存在しない場合は処理なし
				if (debug) System.out.println(DBGID+"番組表情報がないので過去ログは参照しない： "+newpcl.Date+" "+newpl.Center);
				continue;
			}

			if ( newpdla.get(0).start.length() != 0 ) {
				// 先頭の番組情報が「番組情報がありません」以外の場合は処理なし
				if (debug) System.out.println(DBGID+"先頭から有効な情報なので過去ログは参照しない： "+newpcl.Date+" "+newpl.Center+" "+newpdla.get(0).start+" "+newpdla.get(0).title);
				continue;
			}

			PassedProgram oldplist = new PassedProgram();
			if ( ! oldplist.loadByCenter(newpcl.Date, newpl.Center) || oldplist.getProgCount() == 0 ) {
				// 過去情報が取得できなければ処理なし
				System.out.println(DBGID+"過去ログに情報はありませんでした");
				continue;
			}
			ProgDateList oldpcl = oldplist.pcenter.get(0).pdate.get(0);

			// 補填候補抽出
			ArrayList<ProgDetailList> tmppdla = new ArrayList<ProgDetailList>();
			if ( newpdla.size() == 1 ) {
				// 「番組情報がありません」しかない場合は全面複写
				for ( ProgDetailList oldpdl : oldpcl.pdetail ) {
					tmppdla.add(oldpdl.clone());
				}
			}
			else {
				int idx = 0;
				for ( ProgDetailList oldpdl : oldpcl.pdetail ) {
					if ( idx == 0 ) {
						// 過去ログの最初は無条件に追加してよい
						tmppdla.add(oldpdl.clone());
					}
					else if ( oldpdl.startDateTime.compareTo(newpdla.get(1).startDateTime) < 0 ) {
						// ２個目以降は当日の有効情報の前まで（「番組情報がありません」は無条件追加）
						tmppdla.add(oldpdl.clone());
					}
					else {
						// 有効情報を越えたら終了
						break;
					}
					idx++;
				}
			}

			// 先頭の「番組情報はありません」と差し替えて補填
			newpdla.remove(0);
			for ( int i=0; i<tmppdla.size(); i++ ) {
				newpdla.add(i,tmppdla.get(i));
			}
			tmppdla = null;
		}
	}

	protected void addEmptyInfo(ProgDateList pcl, String sdat, String edat) {
		ProgDetailList pdl = new ProgDetailList();
		pdl.abon();
		pdl.startDateTime = sdat;
		//pdl.endDateTime = edat;
		pdl.length = (int)(CommonUtils.getDiffDateTime(sdat, edat)/60000L);
		pcl.pdetail.add(pdl);
		pcl.row += pdl.length;
	}

	/*******************************************************************************
	 * フラグ処理関連
	 ******************************************************************************/

	/**
	 * 延長警告を設定する
	 */
	public void setExtension(String spoexSearchStart, String spoexSearchEnd, boolean spoexLimitation, ArrayList<SearchKey> extKeys) {
		//
		for ( ProgList p : pcenter ) {
			// 局ごと

			// キーワード検索用
			String centerPop = TraceProgram.replacePop(p.Center);

			for ( ProgDateList c : p.pdate ) {
				// 日ごと
				boolean poisoned = false;
				for (ProgDetailList d : c.pdetail) {
					// 番組ごと
					boolean soi = false;
					for ( SearchKey k : extKeys ) {
						// 個別設定による延長可否
						boolean isMatch = SearchProgram.isMatchKeyword(k, ((k.getCaseSensitive()==false)?(centerPop):(p.Center)), d);
						if (isMatch) {
							if (k.getInfection().equals("0")) {
								// 延長感染源にする
								soi = true;
							}
							else {
								// 延長感染源にしない（優先）
								soi = false;
								break;
							}
						}
					}
					if (soi) {
						poisoned = true;
					}

					d.extension = poisoned;
				}
			}
		}
	}


	/**
	 * コードポイントを文字に変換する
	 *
	 * @param cp	コードポイント
	 * @return		変換後の文字
	 */
	protected static String cp2s(int cp){
		StringBuffer sb = new StringBuffer("");
		sb.appendCodePoint(cp);

		return sb.toString();
	}

	/*
	 * UNIODEの番組記号のマッピング情報
	 *
	 */
	enum UnicodeCharMap{
		BMODE		("Ｂ"	,0x0001F131,	ProgOption.BMODE),
		NEWS		("Ｎ"	,0x0001F13D,	ProgOption.NEWS),
		PROGRESSIVE	("Ｐ"	,0x0001F13F,ProgOption.PROGRESSIVE),
		STEREO		("Ｓ"	,0x0001F142,	ProgOption.STEREO),
		WIDE		("Ｗ"	,0x0001F146,	ProgOption.WIDE),
		HDTV		("HV"	,0x0001F14A,	ProgOption.HDTV),
		MULTIVIEW	("MV"	,0x0001F14B,	ProgOption.MULTIVIEW),
		SDTV		("SD"	,0x0001F14C,	ProgOption.SDTV),
		SURROUND	("SS"	,0x0001F14D,	ProgOption.SURROUND),
		PPV			("PPV"	,0x0001F14E,	ProgOption.PPV),

		SIGN		("手"	,0x0001F210,	ProgOption.SIGN),
		SUBTITLE	("字"	,0x0001F211,	ProgOption.SUBTITLE),
		TWOWAY		("双"	,0x0001F212,	ProgOption.TWOWAY),
		DATA		("デ"	,0x0001F213,	ProgOption.DATA),
		BILINGUAL	("二"	,0x0001F214,	ProgOption.BILINGUAL),
		MULTIVOICE	("多"	,0x0001F215,	ProgOption.MULTIVOICE),
		COMMENTARY	("解"	,0x0001F216,	ProgOption.COMMENTARY),
		WEATHER		("天"	,0x0001F217,	ProgOption.WEATHER),
		TRAFFIC		("交"	,0x0001F218,	ProgOption.TRAFFIC),
		MOVIE		("映"	,0x0001F219,	ProgOption.MOVIE),
		FREE		("無"	,0x0001F21A,	ProgOption.FREE),
		PAY			("料"	,0x0001F21B,	ProgOption.PAY),
		FORMER		("前"	,0x0001F21C,	ProgOption.NOOPTION),
		LATTER		("後"	,0x0001F21D,	ProgOption.NOOPTION),
		REPEAT		("再"	,0x0001F21E,	ProgOption.REPEAT),
		NEW			("新"	,0x0001F21F,	ProgOption.NOOPTION),
		FIRST		("初"	,0x0001F220,	ProgOption.FIRST),
		END			("終"	,0x0001F221,	ProgOption.NOOPTION),
		LIVE		("生"	,0x0001F222,	ProgOption.LIVE),
		SHOPPING	("販"	,0x0001F223,	ProgOption.SHOPPING),
		VOICE		("声"	,0x0001F224,	ProgOption.NOOPTION),
		STANDIN		("吹"	,0x0001F225,	ProgOption.STANDIN),

		// テレビ王国の独自コード
		BS4K		("4K"	,0x0001F19E,	ProgOption.BS4K),
		BS8K		("8K"	,0x0001F19F,	ProgOption.BS8K),
		SURROUND51	("5.1"	,0x0001F1A0,	ProgOption.SURROUND51),
		SURROUND222	("22.2"	,0x0001F1A2,	ProgOption.SURROUND222),
		HDR			("HDR"	,0x0001F1A7,	ProgOption.HDR),
		;

		String	label;			// ラベル
		int		codePoint;		// コードポイント
		ProgOption progOption;	// 番組オプション

		/**
		 * コンストラクタ
		 *
		 * @param l		ラベル
		 * @param cp	コードポイント
		 * @param po	番組オプション
		 */
		private UnicodeCharMap(String l, int cp, ProgOption po){
			label = l;
			codePoint = cp;
			progOption = po;
		}

		public String getLabel(){ return label; }
		public String getCode(){ return cp2s(codePoint); }
		public ProgOption getProgOption(){ return progOption; }

		/**
		 * 指定した文字列で検索する
		 *
		 * @param str	検索対象の文字列
		 * @return		Matcherオブジェクト
		 */
		public Matcher getMatcher(String str){
			return Pattern.compile(cp2s(codePoint)).matcher(str);
		}

		/**
		 * 番組情報から記号を抽出する
		 *
		 * @param pdl	番組情報
		 */
		public void extractSymbol(ProgDetailList pdl){
			Matcher md = getMatcher(pdl.title);
			if ( md.find() ) {
				if (progOption != ProgOption.NOOPTION)
					pdl.addOption(progOption);
				pdl.title = md.replaceAll("");
			}
		}

		/**
		 * 番組情報からすべての記号を抽出する
		 *
		 * @param pdl	番組情報
		 */
		private static String codeList = null;
		public static void ExtractAllSymbols(ProgDetailList pdl){
			if (codeList == null){
				StringBuilder sb = new StringBuilder("");
				for (UnicodeCharMap chm : UnicodeCharMap.values()){
					sb.append(chm.getCode());
				}

				codeList = sb.toString();
			}

			Matcher md = Pattern.compile("[" + codeList + "]").matcher(pdl.title);
			boolean found = false;
			while (md.find()){
				UnicodeCharMap chm = getMapFromCode(md.group());
				if (chm != null && chm.getProgOption() != ProgOption.NOOPTION)
					pdl.addOption(chm.getProgOption());
				found = true;
			}

			if (found)
				pdl.title = md.replaceAll("");
		}

		/**
		 * 文字コードからマップ情報を取得する
		 *
		 * @param code		文字コード
		 * @return			マップ情報
		 */
		public static UnicodeCharMap getMapFromCode(String code){
			for (UnicodeCharMap chm : UnicodeCharMap.values()){
				if (code.equals(chm.getCode())){
					return chm;
				}
			}

			return null;
		}
	}

	/**
	 * 番組情報からUNICODEの記号を抽出する
	 *
	 * @param pdl	番組情報
	 */
	protected void ExtractAllUnicodeSymbols(ProgDetailList pdl){
		// [新]
		Matcher md = UnicodeCharMap.NEW.getMatcher(pdl.title);
		if ( md.find() ) {
			pdl.flag = ProgFlags.NEW;
			pdl.title = md.replaceAll("");
		}
		// [終]
		md = UnicodeCharMap.END.getMatcher(pdl.title);
		if ( md.find() ) {
			pdl.flag = ProgFlags.LAST;
			pdl.title = md.replaceAll("");
		}
		// [無]
		md = UnicodeCharMap.FREE.getMatcher(pdl.title);
		if ( md.find() ) {
			pdl.noscrumble = ProgScrumble.NOSCRUMBLE;
			pdl.title = md.replaceAll("");
		}

		UnicodeCharMap.ExtractAllSymbols(pdl);
	}

	/*
	 * タイトルからフラグを抽出する
	 */
	protected void doSplitFlags(ProgDetailList pdl, HashMap<String, String> nf) {
		// テレビ王国のUNICODE記号を抽出する
		ExtractAllUnicodeSymbols(pdl);

		Matcher md = Pattern.compile("(#1|第1話)\\b").matcher(pdl.title);
		if ( md.find() ) {
			pdl.flag = ProgFlags.NEW;
		}

		md = Pattern.compile("([ 　]?[＜<]新[＞>]| 新$| NEW$)").matcher(pdl.title);
		if ( md.find() ) {
			pdl.flag = ProgFlags.NEW;
			pdl.title = md.replaceAll("");
		}
		md = Pattern.compile("([ 　]?[＜<]終[＞>]| 終$| END$)").matcher(pdl.title);
		if ( md.find() ) {
			pdl.flag = ProgFlags.LAST;
			pdl.title = md.replaceAll("");
		}
		md = Pattern.compile("[(（]終[）)]",Pattern.DOTALL).matcher(pdl.detail);
		if ( md.find() ) {
			pdl.flag = ProgFlags.LAST;
		}
		md = Pattern.compile("^無料≫").matcher(pdl.title);
		if ( md.find() ) {
			pdl.noscrumble = ProgScrumble.NOSCRUMBLE;
			pdl.title = md.replaceAll("");
		}

		Pattern pat = Pattern.compile("初放送",Pattern.DOTALL);
		if ( pat.matcher(pdl.detail).find() ) {
			pdl.addOption(ProgOption.FIRST);
		}
		else if ( pat.matcher(pdl.title).find() ) {
			pdl.addOption(ProgOption.FIRST);
		}

		pat = Pattern.compile("(視聴(..)?制限|[RＲ]([-－]?[1１][5５8８][+＋]?|指定))",Pattern.DOTALL);
		if ( pat.matcher(pdl.detail).find() ) {
			pdl.addOption(ProgOption.RATING);
		}
		else if ( pat.matcher(pdl.title).find() ) {
			pdl.addOption(ProgOption.RATING);
		}

		if ( pdl.detail.indexOf("5.1サラウンド") != -1 ) {
			pdl.addOption(ProgOption.SURROUND);
		}

		HashMap<String, String> xf = new HashMap<String, String>();

		String flagExpr = "[\\[［(（【]([^\\]］)）】]{1,4})[\\]］)）】]";
		Matcher mx = Pattern.compile(flagExpr).matcher(pdl.title);
		while (mx.find()) {
			String mark = mx.group(1);
			ProgOption po = ProgOption.GetProgOptionFromMark("[" + mark + "]");

			if (mark.equals("新") || mark.equals("新番組")) {
				pdl.flag = ProgFlags.NEW;
			}
			else if (mark.equals("終") || mark.equals("完") || mark.equals("最終回")) {
				pdl.flag = ProgFlags.LAST;
			}
			else if (mark.equals("再")) {
				pdl.addOption(ProgOption.REPEAT);
			}
			else if (mark.equals("初")) {
				pdl.addOption(ProgOption.FIRST);
			}
			else if (mark.equals("無") || mark.equals("無料")) {
				//pdl.addOption(ProgOption.NOSCRUMBLE);
				pdl.noscrumble = ProgScrumble.NOSCRUMBLE;
			}
			else if (mark.equals("二／吹")) {
				pdl.addOption(ProgOption.BILINGUAL);
				pdl.addOption(ProgOption.STANDIN);
			}
			else if (mark.equals("字幕") || mark.equals("字幕版")) {
				pdl.addOption(ProgOption.SUBTITLE);
			}
			else if (mark.equals("吹替") || mark.equals("吹替版")) {
				pdl.addOption(ProgOption.STANDIN);	// (ないよ)
			}
			else if (po != null && po != ProgOption.NOOPTION)
				pdl.addOption(po);
			else if (mark.matches("^(Ｓ|Ｎ|Ｂ|映|双|解|手|天|英|日|録|HV)$")) {
				// 無視するフラグ
				if ( mark.equals("日") && ( ! pdl.title.matches(String.format("^(%s)*[(（]日[）)].*", flagExpr)) && ! pdl.title.matches(".*[\\[［]日[\\]］].*")) ) {
					// 削除しないフラグ（特殊）
					continue;
				}
			}
			else if (mark.matches("^(韓|仮|[前後][編篇半]|[月火水木金土]|[0-9０-９]+)$")) {
				// 削除しないフラグ
				continue;
			}

			else {
				// 未知のフラグ
				nf.put(mark, null);
				continue;
			}

			// 削除するフラグ
			xf.put(mark, null);
		}
		{
			// 認識されたフラグだけ削除する.
			String repExpr = "";
			for ( String f : xf.keySet() ) {
				repExpr += String.format("%s|",f);
			}
			if ( repExpr.length() > 0 ) {
				repExpr = "[\\[［(（【]("+repExpr.substring(0, repExpr.length()-1)+")[\\]］)）】]";
				pdl.title = pdl.title.replaceAll(repExpr, "");
			}
		}

		if ( pdl.title.matches("^特[:：].*") ) {
			pdl.option.add(ProgOption.SPECIAL);
			pdl.title = pdl.title.substring(2);
		}
		else if ( pdl.detail.contains("OVA") && ! pdl.detail.contains("+OVA") ) {
			pdl.option.add(ProgOption.SPECIAL);
		}
		else if ( pdl.detail.contains("未放送") ) {
			pdl.option.add(ProgOption.SPECIAL);
		}
	}

	/**
	 * マルチジャンル処理
	 */
	protected void setMultiGenre(ProgDetailList pdl, ArrayList<String> genrelist) {
		// コード順にならべかえる
		Collections.sort(genrelist);

		// ここに入ってこない限り genrelist == null なので対応プラグイン以外ではマルチジャンルは機能しない
		pdl.genrelist = new ArrayList<TVProgram.ProgGenre>();
		pdl.subgenrelist = new ArrayList<TVProgram.ProgSubgenre>();

		String gcode = ProgGenre.NOGENRE.toIEPG();
		String subgcode = ProgSubgenre.NOGENRE_ETC.toIEPG();

		// マルチジャンルコードを設定する
		for ( String gstr : genrelist ) {
			// ジャンルコードが複数ある場合は基本的に最初のものを代表にするが、一部例外をもうける（ニュースよりドキュメンタリー優先、など）
			// 鯛ナビの一覧で表示されるジャンルは代表のものだけである
			String gv = gstr.substring(0,1);
			String subgv = gstr.substring(1,2);
			if ( gcode.equals(ProgGenre.NOGENRE.toIEPG()) ) {
				gcode = gv;
				subgcode = subgv;
			}
			else if ( gcode.equals(ProgGenre.NEWS.toIEPG()) && gv.equals(ProgGenre.DOCUMENTARY.toIEPG())) {
				gcode = gv;
				subgcode = subgv;
			}
			/*
			else if ( gcode.equals(ProgGenre.MUSIC.toIEPG()) && md.group(1).equals(ProgGenre.VARIETY.toIEPG())) {
				gcode = md.group(1);
				subgcode = md.group(2);
			}
			*/

			// 3.14.12βでマルチジャンル対応を追加した
			// 一覧では代表しか見えないが、検索処理ではすべてのジャンルコードが対象になる
			{
				ProgGenre ng = ProgGenre.NOGENRE;
				ProgSubgenre nsubg = ProgSubgenre.NOGENRE_ETC;
				for ( ProgGenre g : ProgGenre.values() ) {
					if ( g.toIEPG().equals(gv) ) {
						ng = g;
						for ( ProgSubgenre subg : ProgSubgenre.values() ) {
							if ( subg.getGenre().equals(g) && subg.toIEPG().equals(subgv) ) {
								nsubg = subg;
								break;
							}
						}
						break;
					}
				}
				if ( ng != ProgGenre.NOGENRE ) {
					pdl.genrelist.add(ng);
					pdl.subgenrelist.add(nsubg);
				}
			}
			if ( pdl.genrelist.size() == 0 ) {
				pdl.genrelist.add(ProgGenre.NOGENRE);
				pdl.subgenrelist.add(ProgSubgenre.NOGENRE_ETC);
			}
		}

		// 代表ジャンルコードを設定する
		for ( ProgGenre g : ProgGenre.values() ) {
			if ( g.toIEPG().equals(gcode) ) {
				pdl.genre = g;
				for ( ProgSubgenre subg : ProgSubgenre.values() ) {
					if ( subg.getGenre().equals(g) && subg.toIEPG().equals(subgcode) ) {
						pdl.subgenre = subg;
						break;
					}
				}
				break;
			}
		}
	}


	/*******************************************************************************
	 * 通信系
	 ******************************************************************************/

	// Web上から取得してファイルにキャッシュする

	// GET型
	public boolean webToFile(String uri, String fname, String thisEncoding) {
		return webToFile(uri, null, null, null, fname, thisEncoding);
	}

	// POST型
	public boolean webToFile(String uri, String pstr, String cookie, String referer, String fname, String thisEncoding) {
		int retry = 0;
		while (true) {
			if ( _webToFile(uri, pstr, cookie, referer, fname, thisEncoding) == true ) {
				break;
			}
			if ( ++retry > retrycount ) {
				return false;
			}
			System.out.println("wait for retry...");
			CommonUtils.milSleep(1000);
		}

		return true;
	}

	// GET/POST本体
	private boolean _webToFile(String uri, String pstr, String cookie, String referer, String fname, String thisEncoding) {

		HttpURLConnection ucon = null;
		BufferedWriter filewriter = null;
		BufferedReader filereader = null;
		BufferedOutputStream streamwriter = null;
		BufferedInputStream streamreader = null;
		try {
			ucon = _webToXXX(uri,pstr,cookie,referer,thisEncoding);
			if (ucon == null) {
				return false;
			}

			// 一時ファイルに書き出し
			if (thisEncoding != null) {
				filewriter = new BufferedWriter(new FileWriter(fname+".tmp"));
				filereader = new BufferedReader(new InputStreamReader(ucon.getInputStream(), thisEncoding));
				String str;
			    while((str = filereader.readLine()) != null){
			    	filewriter.write(str);
			    	filewriter.write("\n");
			    }
			    filewriter.close();
			    filewriter = null;
			    filereader.close();
			    filereader = null;
			}
			else {
				streamwriter = new BufferedOutputStream(new FileOutputStream(fname+".tmp"));
				streamreader = new BufferedInputStream(ucon.getInputStream());
				byte[] buf = new byte[65536];
				int len;
			    while((len = streamreader.read(buf,0,buf.length)) != -1){
			    	streamwriter.write(buf,0,len);
			    }
			    streamwriter.close();
			    streamwriter = null;
			    streamreader.close();
			    streamreader = null;
			}
		    ucon.disconnect();
		    ucon = null;

		    // クローズしてからじゃないと失敗するよ

			// キャッシュファイルに変換
		    File o = new File(fname);
		    if ( o.exists() && ! o.delete() ) {
		    	System.err.println("削除できないよ： "+fname);
		    }
		    File n = new File(fname+".tmp");
		    if ( ! n.renameTo(o) ) {
		    	System.err.println("リネームできないよ： "+fname+".tmp to "+fname);
		    	return false;
		    }

		    return true;
		}
		catch (Exception e) {
			// 例外
			System.out.println("Webアクセスに失敗しました("+uri+"): "+e);
		}
		finally {
			CommonUtils.closing(filewriter);
			CommonUtils.closing(filereader);
			CommonUtils.closing(streamwriter);
			CommonUtils.closing(streamreader);
			CommonUtils.closing(ucon);
		}
		return false;
	}

	// Web上から取得してバッファに格納する

	// GET型
	public String webToBuffer(String uri, String thisEncoding, boolean nocr) {
		return webToBuffer(uri, null, null, null, thisEncoding, nocr);
	}

	// POST型
	public String webToBuffer(String uri, String pstr, String cookie, String referer, String thisEncoding, boolean nocr) {
		int retry = 0;
		while (true) {
			String response = _webToBuffer(uri, pstr, cookie, referer, thisEncoding, nocr);
			if ( response != null ) {
				return response;
			}
			if ( ++retry > retrycount ) {
				break;
			}
			System.out.println("wait for retry...");
			CommonUtils.milSleep(1000);
		}
		return null;
	}

	// 本体
	private String _webToBuffer(String uri, String pstr, String cookie, String referer, String thisEncoding, boolean nocr) {

		if ( thisEncoding == null ) {
			return null;
		}

		try {
			HttpURLConnection ucon = null;
			BufferedReader reader = null;
			try {
				ucon = _webToXXX(uri,pstr,cookie,referer,thisEncoding);
				if (ucon == null) {
					return null;
				}

				// バッファ作成
				StringBuilder sb = new StringBuilder();
				reader = new BufferedReader(new InputStreamReader(ucon.getInputStream(), thisEncoding));
				String str;
			    while((str = reader.readLine()) != null){
			    	sb.append(str);
			        if ( ! nocr) sb.append("\n");
			    }
			    return sb.toString();
			}
			catch (Exception e) {
				System.out.println("Webアクセスに失敗しました("+uri+"): "+e.toString());
				//e.printStackTrace();
			}
			finally {
				if ( reader != null ) {
					reader.close();
					reader = null;
				}
				if ( ucon != null ) {
					ucon.disconnect();
					ucon = null;
				}
			}
		}
		catch ( Exception e ) {
			// close()の例外は無視
		}
		return null;
	}

	/*
	 * File/Bufferの共通部品
	 */

	private HttpURLConnection _webToXXX(String uri, String pstr, String cookie, String referer, String thisEncoding) {
		try {
			URL url = new URL(uri);
			HttpURLConnection ucon;
			if ( getProxy() == null ) {
				ucon = (HttpURLConnection)url.openConnection();
			}
			else {
				ucon = (HttpURLConnection)url.openConnection(getProxy());
			}
			ucon.setConnectTimeout(conntout*1000);
			ucon.setReadTimeout(readtout*1000);

			ucon.addRequestProperty("User-Agent", getUserAgent());

			if ( referer != null ) {
				ucon.addRequestProperty("Referer", referer);
			}

			if ( cookie != null ) {
				ucon.addRequestProperty("Cookie", cookie);
			}

			// Cookie処理（CookieManagerはなぜうまく動かないんだ…orz）
			if ( cookie_cache.size() > 0 ) {
				String cStr = "";
				for ( String key : cookie_cache.keySet() ) {
					cStr += key+"="+cookie_cache.get(key)+"; ";
				}
				ucon.addRequestProperty("Cookie", cStr);
			}

			if (pstr != null) {
				ucon.setRequestMethod("POST");
				ucon.setDoOutput(true);
				ucon.setDoInput(true);
			}
			else {
				ucon.setRequestMethod("GET");
				ucon.connect();
			}

			// POSTの場合は別途データを送る
			if (pstr != null && thisEncoding != null) {
				OutputStream writer = ucon.getOutputStream();
				writer.write(pstr.getBytes(thisEncoding));
				writer.flush();
				writer.close();
			}

			// Cookie処理
			if ( uri.matches(".*dimora.jp.*") || uri.matches(".*\\.yahoo\\.co\\.jp.*") ) {
				List<String> hf = ucon.getHeaderFields().get("Set-Cookie");
				if ( hf != null ) {
			    	for ( String rcookie :  hf ) {
			    		String[] rc1 = rcookie.split(";",2);
			    		String[] rc2 = rc1[0].split("=",2);
			    		cookie_cache.put(rc2[0], rc2[1]);
			    	}
				}
		    }

			return ucon;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected String getCookie(String key) {
		return cookie_cache.get(key);
	}
	protected void addCookie(String key, String val) {
		cookie_cache.put(key,val);
	}
	protected void delCookie(String key) {
		cookie_cache.remove(key);
	}
	protected void clrCookie() {
		cookie_cache.clear();
	}

	// Cookieの情報をキャッシュするよ
	private HashMap<String,String> cookie_cache = new HashMap<String, String>();


	/*******************************************************************************
	 * ここから下は該当機能が無効なプラグイン用のダミー
	 ******************************************************************************/

	public String getTVProgramId() { return "DUMMY"; }

	public String getDefaultArea() { return "東京"; }

	// 番組詳細キャッシュファイル名
	protected String getDetCacheFile() { return "";	}

	// 番組詳細をattachする
	protected void attachDetails(ArrayList<ProgList> plist, HashMap<String,String> oldcache, HashMap<String,String> newcache) {
		// ダミー
	}

	// フリーテキストによるオプション指定
	public boolean setOptString(String s) { return true; }	// ダミー
	public String getOptString() { return null; }			// ダミー

	/*
	 * 番組情報をキャッシュファイルに保存する
	 */
	public boolean saveToProgCache(String prefix){
		if (!useProgCache){
			System.out.println("高速キャッシュファイルは使用しません");
			return false;
		}

		// キャッシュファイルのパス名を生成する
		String file = String.format(getProgDir()+File.separator+prefix +"_ALL.txt");

		File f = new File(file);
		f.delete();

		// 番組情報を文字列に変換する
		StringBuilder sb = new StringBuilder();
		for ( ProgList p : pcenter ) {
			sb.append(p.toString());
			sb.append(PLEND);
		}

		// キャッシュファイルに保存する
		System.out.println("高速キャッシュファイルに保存しています(" + file + ")...");
		if ( ! CommonUtils.write2file(file, sb.toString()) ) {
			System.out.println("高速キャッシュファイルへの保存に失敗しました。");
			return false;
		}

		System.out.println("高速キャッシュファイルへの保存に成功しました。");
		return true;
	}

	/*
	 * 番組情報をキャッシュファイルから読み込む
	 */
	public boolean loadFromProgCache(String prefix){
		TatCount tc = new TatCount();

		if (!useProgCache || invalidProgCache){
			System.out.println("高速キャッシュファイルは使用しません");
			return false;
		}
		// キャッシュファイルのパス名を生成する
		String file = String.format(getProgDir()+File.separator+prefix +"_ALL.txt");
		System.out.println("高速キャッシュファイルを読み込んでいます(" + file + ")...");

		File f = new File(file);
		if (!f.exists()){
			System.out.println("高速キャッシュファイルはありません");
			return false;
		}

		// 当日保存されたものかどうかをチェックする
		String curDate = CommonUtils.getDate529(0,true);

		GregorianCalendar c = new GregorianCalendar();
		c.setTimeInMillis(f.lastModified());

		String fdate = CommonUtils.getDate529(c, false);
		String today = CommonUtils.getDate529(0,  false);
		if (!fdate.equals(today)){
			System.out.println("高速キャッシュファイルは古いです");
			return false;
		}

		String txt = CommonUtils.read4file(file, false);
		if ( txt == null ) {
			System.out.println("高速キャッシュファイルの読み込みに失敗しました");
			return false;
		}

		ArrayList<ProgList> pcl = new ArrayList<ProgList>();

		// 文字列から番組情報を読み込む
		int index = 0;
		while ( index < txt.length() ){
			// 放送局のターミネータを検索する
			int le = txt.indexOf(PLEND, index);

			// １放送局分を文字列から生成する
			ProgList pl = new ProgList(txt.substring(index, le));

			for ( Center cr : crlist ) {
				if ( pl.Area.equals(cr.getAreaCode()) && pl.CenterId.equals(cr.getLink()) ) {
					pl.SubArea = cr.getType();
					pl.Center = cr.getCenter();
					pl.BgColor = cr.getBgColor();
					break;
				}
			}

			pcl.add(pl);

			if (le == -1)
				break;

			// 残りの文字列のインデックスを計算する
			index = le + PLEND.length();
		}

		pcenter = pcl;

		String tm = String.format(" [%.2f秒]", tc.end());
		System.out.println("高速キャッシュファイルの読み込みに成功しました" + tm);
		return true;
	}
}
