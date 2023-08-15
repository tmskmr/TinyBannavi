package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.ContentIdDIMORA;
import tainavi.ContentIdEDCB;
import tainavi.ContentIdREGZA;
import tainavi.FieldUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.RecordedInfo;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.b64;
import tainavi.HDDRecorder.RecType;

// "cRECMODE" はBZT720用に追加したもの


public class PlugIn_RecDIGA_DMR_BWT2100 extends HDDRecorderUtils implements HDDRecorder,Cloneable {
	
	@Override
	public PlugIn_RecDIGA_DMR_BWT2100 clone() {
		return (PlugIn_RecDIGA_DMR_BWT2100) super.clone();
	}
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	@Override
	public String getRecorderId() { return "DIGA DMR-BWT2100"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }

	// 録画結果一覧を取得できる
	@Override
	public boolean isRecordedListSupported() { return true; }
	// 番組追従は編集できない
	@Override
	public boolean isPursuesEditable() { return false; }
	// タイトル自動補完はできない
	@Override
	public boolean isAutocompleteSupported() { return false; }
	// チャンネル操作が可能
	@Override
	public boolean isChangeChannelSupported() { return true; }
	// 
	@Override
	public boolean isThereAdditionalDetails() { return true; }
	
	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	@Override
	public String getLabel_Audiorate() { return "予約方法"; }
	
	@Override
	public String getChDatHelp() { return chdathelp; }
	
	private static final String chdathelp =
			"■放送局コード設定方法はプロジェクトwikiを参照してください。http://sourceforge.jp/projects/tainavi/wiki/DIGA#CHCODE"+
			"　■予約方法の読み替え：EPG=番組・ﾌﾟﾛｸﾞﾗﾑ=時間"+
			"　■未対応の曜日指定パターンは単日扱いになります";
	
	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	@Override
	public ChannelCode getChCode() {
		return cc;
	}
	
	private final ChannelCode cc = new ChannelCode(getRecorderId());
	
	@Override
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}
	
	private String errmsg = "";

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	protected static final String[] DIGA_WDPTNSTR  = { "cSUN", "cMON", "cTUE", "cWED", "cTHU", "cFRI", "cSAT", "cALL" };
	protected static final String[] DIGA_WDPTNCODE = { "2",    "4",    "8",    "10",   "20",   "40",   "80",   "fe"   };
	protected static final String[] DIGA_WDAYSTR   = { "日",   "月",   "火",   "水",   "木",   "金",   "土",   "毎日"  };
	
	private static final String ITEM_VIDEO_TYPE_NONE	= "LAN予約";
	
	private static final String VALUE_VIDEO_TYPE_NONE	= "#NONE#";
	
	private static final String DIGAMSG_WAITFORLOGIN	= "DIGAと通信中です。しばらくお待ちください。";
	private static final String DIGAMSG_PLEASELOGIN		= "ログインしてからアクセスしてください。";
	private static final String DIGAMSG_BUSYNOW			= "本体操作中、または現在実行できない操作です。";
	private static final String DIGAMSG_CANNOTRESERVE	= "予約が設定できませんでした。";
	private static final String DIGAMSG_PLEASEPOWON		= "電源をオンしてから操作してください。";
	private static final String DIGAMSG_POWOFF			= "電源切";
	private static final String DIGAMSG_LOGGEDIN		= "onLoadLoginNext";

	private static final String MISS_HDR = "★★DIGAが追跡に失敗★★　";
	private static final String MISS_TITLE = "★★番組詳細を取得しなおしてください★★";

	protected static final int RETCODE_SUCCESS = 0;
//	protected static final int RETCODE_BUSY = -1;
//	protectede static final int RETCODE_REDO = -2;
	protected static final int RETCODE_FATAL = -99;
	
	private static final int DIGAEVID_NONE = 0;
	private static final int DIGAEVID_CANNOTFOLLOW = 0xFFFE;
	private static final int DIGAEVID_PROGRSV = 0xFFFF;

	// 録画結果一覧関連
	private static final int RECITEMPERPAGE = 20;		// 予約結果の１ページあたりの件数
	private static final int RECLENGTHDEFAULT = 60;		// デフォルトの番組長

	public void setRecPageMax(int n) { recPageMax = n; }
	private static int recPageMax = 3;					// 予約結果を最大何ページ取得するか

	// ログ関連
	
	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	private String rsvedFile = "";
	private String vrateTFile = "";
	private String chValueTFile = "";
	
	private String DMY_TIME = "";
	private String NONCE = "";
	private String DIGEST = "";
	
	protected int get_com_try_count() { return 9; }
	//private int COM_TRY_COUNT = 5;
	private int WAIT_FOR_LOGIN = 7500;
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public PlugIn_RecDIGA_DMR_BWT2100() {
		super();
		this.setTunerNum(2);
	}
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	@Override
	public boolean ChangeChannel(String Channel) {
		//
		if (Channel == null) {
			return true;
		}
		
		errmsg = "";
		
		String chcode = cc.getCH_WEB2CODE(Channel);
		if ( chcode == null ) {
			System.err.println(errmsg = "no such ch: "+Channel);
			return false;
		}
		Matcher ma = Pattern.compile("^(.+?):(.+?)$").matcher(chcode);
		if (ma.find()) {
			{
				System.out.println("change channel.(1/2)");
				if ( doLogin() == false ) {
					errmsg = "ログインに失敗しました.";
					return false;
				}
			}
			{
				System.out.println("change channel.(2/2)");
				String url = "http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_chtune.cgi";
				String pstr = "cCHSRC="+ma.group(2)+"&cCHNUM="+ma.group(1)+"&cCMD_SET.x=49&cCMD_SET.y=8";
				String[] d = reqPOST(url, pstr, null);
				String response = d[1];
				if ( response == null ) {
					errmsg = "レコーダが無応答.";
					return false;
				}
				if ( response.contains(DIGAMSG_BUSYNOW) ) {
					errmsg = DIGAMSG_PLEASEPOWON;
					return false;
				}
				if ( response.contains(DIGAMSG_PLEASEPOWON) ) {
					errmsg = DIGAMSG_PLEASEPOWON;
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public void wakeup() {
		poweronoff(true);
	}
	
	@Override
	public void shutdown() {
		poweronoff(false);
	}
	
	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/
	
	@Override
	public boolean GetRdReserve(boolean force)
	{
		errmsg = "" ;
		
		System.out.println("Run: GetRdReserve("+force+")");
		
		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		vrateTFile = "env/videorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		setSettingEncoder(encoder);	// チューナー数は自動生成
		setSettingRecType(arate);	// 録画種別は自動生成

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			
			// キャッシュから読み出し（録画設定ほか）
			vrate = TVSload(vrateTFile);
			chvalue = TVSload(chValueTFile);
			
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && chvalue.size()>0) {
				return(true);
			}
		}
		
		// とりあえず再ログインしてみる
		if ( doLogin() == false ) {
			errmsg = "ログインに失敗しました.";
			return false;
		}

		// DIGAから情報取得
		
		// (1)録画設定の取得
		if ( getDigaRecordSetting() == false ) {
			errmsg = "録画設定が取得できませんでした.";
			return false;
		}
		
		// (2)予約一覧の取得・キャッシュへの保存
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		if ( getDigaReserveList(newReserveList,getReserves()) < 0 ) {
			return false;
		}
		
		// 詳細取得は分離されました

		// 入れ替えて保存
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);
		
		return(true);
	}
	
	@Override
	public boolean GetRdReserveDetails()
	{
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		if ( ! getDigaReserveDetails(newReserveList,getReserves()) ) {
			return false;	// ダミーだな
		}
		
		// 入れ替えて保存
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);
		
		return true;
	}

	/*******************************************************************************
	 * レコーダーから録画結果一覧を取得する
	 ******************************************************************************/
	
	/**
	 * @see #GetRdSettings(boolean)
	 */
	@Override
	public boolean GetRdRecorded(boolean force) {
		
		System.out.println("レコーダから録画結果一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");
		
		String recedFile = String.format("%s%s%s.%s_%s_%s.xml", "env", File.separator, "recorded", getIPAddr(), getPortNo(), getRecorderId());
		
		// 既存のログをチェック
		ArrayList<RecordedInfo> newRecordedList = RecordedFromFile(recedFile);
		
		File f = new File(recedFile);
		if ( ! force && f.exists() ) {
			
			// キャッシュから読み出し（録画結果一覧）
			setRecorded(newRecordedList);
			if (getDebug()) ShowRecorded(getRecorded());
	
			// 録画済みフラグを立てる（録画結果一覧→予約一覧）
			setRecordedFlag();
		
			return true;
		}
		
		if ( getDigaRecordedList(newRecordedList) != RETCODE_SUCCESS ) {
			return false;
		}
		setRecorded(newRecordedList);				// 置き換え
		RecordedToFile(getRecorded(), recedFile);	// キャッシュに保存
		
		// 録画済みフラグを立てる（録画結果一覧→予約一覧）
		setRecordedFlag();
		
		ShowRecorded(getRecorded());
		
		return true;
	}
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	@Override
	public boolean PostRdEntry(ReserveList reqr) {
		
		errmsg = "";
		
		int cntMax = 4;
		int cnt = 1;
		
		//
		System.out.println("Run: PostRdEntry("+reqr.getTitle()+")");
		
		//
		if (cc.getCH_WEB2CODE(reqr.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+reqr.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		if (reqr.getRec_mode().equals(ITEM_VIDEO_TYPE_NONE)) {
			errmsg = "【警告】画質指定が\""+ITEM_VIDEO_TYPE_NONE+"\"の新規登録はできません。" ;
			return(false) ;
		}
		
		// EPG判定
		
		int evid = -1;
		
		boolean rec_type_epg = true;
		if ( ITEM_REC_TYPE_EPG.equals(reqr.getRec_audio()) ) {
			if ( ContentIdEDCB.isValid(reqr.getContentId()) ) {
				ContentIdEDCB.decodeContentId(reqr.getContentId());
				evid = ContentIdEDCB.getEvId();
			}
			else if ( ContentIdDIMORA.isValid(reqr.getContentId()) ) {
				ContentIdDIMORA.decodeContentId(reqr.getContentId());
				evid = ContentIdDIMORA.getEvId();
				// 後続処理あり
			}
			else if ( ContentIdREGZA.isValid(reqr.getContentId()) ) {
				ContentIdREGZA.decodeContentId(reqr.getContentId());
				evid = ContentIdREGZA.getEvId();
				// 後続処理あり
			}
			
			if ( evid == -1 || evid == 0 || evid == 0xffff ) {
				errmsg = "番組表に予約IDがないためEPG予約は利用できません。プログラム予約を行ってください。";
				return false;
			}
		}
		else {
			rec_type_epg = false;
		}
		
		// EPG予約かどうかと連動です
		reqr.setPursues(rec_type_epg);
		
		// 繰り返し予約の開始日っぽい（EPG予約で必須）
		String prg_date = reqr.getRec_nextdate();
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		reqr.setRec_pattern_id(getRec_pattern_Id(reqr.getRec_pattern()));
		reqr.setRec_nextdate(CommonUtils.getNextDate(reqr));
		reqr.setRec_min(CommonUtils.getRecMin(reqr.getAhh(),reqr.getAmm(),reqr.getZhh(),reqr.getZmm()));
		getStartEndDateTime(reqr);
		
		// とりあえず再ログインしてみる
		if ( doLogin() == false ) {
			errmsg = "ログインに失敗しました.";
			return false;
		}
		
		// 登録前の予約IDの一覧を取得する
		reportProgress(String.format("登録前の予約IDのリストを取得します(%d/%d).",cnt++,cntMax));
		ArrayList<ReserveList> preReserveList = new ArrayList<ReserveList>();
		if ( getDigaReserveList(preReserveList,null) < 0 ) {
			errmsg = "登録前の予約IDのリストの取得に失敗しました";
			return false;
		}
		
		// POSTデータを変換する
		HashMap<String, String> pdat = modPostdata(reqr,prg_date);
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String response;
		{
			reportProgress(String.format("処理IDを取得します(%d/%d).",cnt++,cntMax));
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVADD.x=31&cCMD_RSVADD.y=8", null);
			response = d[1];
		}
		
		// 予約情報送信
		String[] keys = {"cRPG", "cRHEX", "cTSTR", "cRHEXEX"};
		for ( String c : keys ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				pdat.put(c,ma.group(1));
			}
		}
		pdat.put("RSV_FIX.x","29");
		pdat.put("RSV_FIX.y","7");
		String pstr = joinPoststr(PostMode.ADD_CMD, pdat);
		{
			reportProgress(String.format("予約情報を送信します(%d/%d).",cnt++,cntMax));
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_add.cgi", pstr, null);
			response = d[1];
		}
		
		// 予約実行
		HashMap<String, String> dgdat = new HashMap<String, String>();
		
		String[] keys2 = { "cRECMODE", "cRPG", "cRHEX", "cTSTR", "cRHEXEX" };
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				dgdat.put(c,ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","45");
		dgdat.put("RSV_EXEC.y","5");
		
		//
		if ( rec_type_epg ) {
			//String cRHEX = dgdat.get("cRHEX");
			String cRHEXEX = dgdat.get("cRHEXEX");
			if ( cRHEXEX != null ) {
				//dgdat.put("cRHEX",cRHEX.replaceFirst("....(....)$", reqr.));
				dgdat.put("cRHEXEX",cRHEXEX.replaceFirst("^......", String.format("10%04x",evid)));
			}
			else {
				errmsg = "番組IDを設定できませんでした。プログラム予約を行ってください。";
				return false;
			}
		}

		//
		pstr = joinPoststr(PostMode.ADD_EXEC, dgdat);
		{
			reportProgress(String.format("予約を確定します(%d/%d).",cnt++,cntMax));
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_addq.cgi", pstr, null);
			response = d[1];
		}
		
		// エラー対応
		Matcher ma = Pattern.compile("name=\"cERR\" value=\"13\"").matcher(response);
		if ( ma.find() ) {
			errmsg = "★★★　予約が重複しています。　★★★";
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			// 予約情報送信
			String[] keys3 = {"cRPG", "cRHEX", "cTSTR", "cRHEXEX"};
			for ( String c : keys3 ) {
				Matcher mb = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
				if ( mb.find() ) {
					pdat.put(c,mb.group(1));
				}
			}
			pdat.put("Image_BtnRyoukai.x","30");
			pdat.put("Image_BtnRyoukai.y","13");
			pstr = joinPoststr(PostMode.ERR_OK, pdat);
			{
				String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/apl_err.cgi", pstr, null);
				response = d[1];
			}
			
			reqr.setTunershort(true);
		}
		
		// 登録結果の返信
		if ( response.contains(DIGAMSG_BUSYNOW) ) {
			errmsg = DIGAMSG_BUSYNOW ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		if ( response.contains(DIGAMSG_CANNOTRESERVE) ) {
			errmsg = DIGAMSG_CANNOTRESERVE ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		
		// 予約リストを作り直したい！
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		// 予約リスト番号を取得（キャッシュに存在しない番号が新番号）
		{
			ArrayList<ReserveList> aftReserveList = new ArrayList<ReserveList>();
			if ( _getDigaReserveList(aftReserveList,response) < 0 ) {
				errmsg = "登録後の予約IDのリストの取得に失敗しました";
				return false;
			}
			ReserveList newr = refreshReserveList(newReserveList,preReserveList,aftReserveList,getReserves());
			if ( newr != null ) {
				// 新規あり！
				reqr.setId(newr.getId());
				reqr.setTunershort(newr.getTunershort());
			}
		}
		
		if ( reqr.getId() == null ) {
			errmsg = ERRID+"【致命的エラー】 予約は成功したと思われますが、該当する予約IDを見つけることができませんでした。";
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			return false;
		}
		
		if ( rec_type_epg ) {
			ReserveList newr = new ReserveList();
			FieldUtils.deepCopy(newr, reqr);
			if ( getDigaReserveDetail(newr, reqr.getId()) == null ) {
				// 情報を取得できなかったので、鯛ナビの情報をそのままで
				errmsg = "登録した予約の情報を取得しなおそうとしましたが、失敗しました。";
			}
			else if ( isModified(reqr, newr) ) {
				// 情報が取得できて、かつ強制変更されているならば置き換える
				errmsg = "DIGA番組表からの情報で内容が変更されました: "+newr.getStartDateTime()+"～"+newr.getZhh()+":"+newr.getZmm()+" "+newr.getTitle();
				reqr = newr;
			}
		}
		
		newReserveList.add(reqr);					// 予約リストに追加
		setReserves(newReserveList);				// 予約リストを更新
		ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}

	
	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	
	@Override
	public boolean UpdateRdEntry(ReserveList cacher, ReserveList reqr) {

		errmsg = "";
		
		System.out.println("Run: UpdateRdEntry()");
		
		//
		if (cc.getCH_WEB2CODE(reqr.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+reqr.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		if (cacher.getRec_mode().equals(ITEM_VIDEO_TYPE_NONE)) {
			if ( ! cacher.getRec_mode().equals(reqr.getRec_mode())) {
				errmsg = "【警告】画質指定を\""+ITEM_VIDEO_TYPE_NONE+"\"から別の設定に変更することはできません。" ;
				return(false) ;
			}
		}
		else if (reqr.getRec_mode().equals(ITEM_VIDEO_TYPE_NONE)) {
			errmsg = "【警告】画質指定を\""+ITEM_VIDEO_TYPE_NONE+"\"に変更することはできません。" ;
			return(false) ;
		}
		
		if ( ! cacher.getRec_audio().equals(reqr.getRec_audio()) ) {
			errmsg = String.format("異なる予約方式への更新は行えません（%s->%s）",cacher.getRec_audio(),reqr.getRec_audio());
			return false;
		}
		
		int pattern_id = getRec_pattern_Id(reqr.getRec_pattern());
		
		if ( ITEM_REC_TYPE_EPG.equals(reqr.getRec_audio()) ) {
			// EPG予約の場合の制限
			if ( (pattern_id != cacher.getRec_pattern_id()) ||
					! cacher.getRec_mode().equals(reqr.getRec_mode()) ) {
				errmsg = "EPG予約で変更出るのは予約のON/OFFのみです。";
				return false;
			}
		}
		
		// 予約パターンID
		// 次回予定日
		// 録画長
		// 開始日時・終了日時
		reqr.setRec_pattern_id(pattern_id);
		reqr.setRec_nextdate(CommonUtils.getNextDate(reqr));
		reqr.setRec_min(CommonUtils.getRecMin(reqr.getAhh(),reqr.getAmm(),reqr.getZhh(),reqr.getZmm()));
		getStartEndDateTime(reqr);
		
		// とりあえず再ログインしてみる
		if ( doLogin() == false ) {
			errmsg = "ログインに失敗しました.";
			return false;
		}
		
		// POSTデータを変換する
		HashMap<String, String> pdat = modPostdata(reqr,null);
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String response;
		{
			reportProgress("処理IDを取得します(1/3).");
			String[] d = reqDigaGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_list.cgi?ANC_RSVLSTNO="+reqr.getId()+"&cDMYDT="+DMY_TIME, null);
			response = d[1];
		}
		
		// 更新情報送信
		String[] keys = { "cRVID","cRVORG","cRVORGEX","cRVORGEX2","cRVORGEX3","cRPG","cRHEX","cTSTR","cRHEXEX" };
		for ( String c : keys ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				pdat.put(c,ma.group(1));
			}
		}
		pdat.put("RSV_EDIT.x","48");
		pdat.put("RSV_EDIT.y","14");
		String pstr = joinPoststr(PostMode.UPD_CMD, pdat);
		{
			reportProgress("更新情報を送信します(2/3).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_edit.cgi", pstr, null);
			response = d[1];
		}

		// 予約実行
		HashMap<String, String> dgdat = new HashMap<String, String>();
		
		String[] keys2 = { "cRECMODE", "cRVID", "cRVORG", "cRVORGEX", "cRVORGEX2", "cRVORGEX3", "cRPG", "cRHEX", "cTSTR", "cRHEXEX" };
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				dgdat.put(c,ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","29");
		dgdat.put("RSV_EXEC.y","12");
		pstr = joinPoststr(PostMode.UPD_EXEC, dgdat);
		{
			reportProgress("更新を確定します(3/3).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_editq.cgi", pstr, null);
			response = d[1];
		}

		// エラー対応
		Matcher ma = Pattern.compile("name=\"cERR\" value=\"13\"").matcher(response);
		if ( ma.find() ) {
			errmsg = "★★★　予約が重複しています。　★★★";
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			// 予約情報送信
			String[] keys3 = {"cRPG", "cRHEX", "cTSTR", "cRHEXEX"};
			for ( String c : keys3 ) {
				Matcher mb = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
				if ( mb.find() ) {
					pdat.put(c,mb.group(1));
				}
			}
			pdat.put("Image_BtnRyoukai.x","30");
			pdat.put("Image_BtnRyoukai.y","13");
			pstr = joinPoststr(PostMode.ERR_OK, pdat);
			{
				String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/apl_err.cgi", pstr, null);
				response = d[1];
			}
			
			reqr.setTunershort(true);
		}
		
		// 登録結果の返信
		if ( response.contains(DIGAMSG_BUSYNOW) ) {
			errmsg = DIGAMSG_BUSYNOW ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		if ( response.contains(DIGAMSG_CANNOTRESERVE) ) {
			errmsg = DIGAMSG_CANNOTRESERVE ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}

		// 予約リストを作り直したい！
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		// 予約リスト番号を取得
		{
			ArrayList<ReserveList> aftReserveList = new ArrayList<ReserveList>();
			if ( _getDigaReserveList(aftReserveList,response) < 0 ) {
				errmsg = "登録後の予約IDのリストの取得に失敗しました";
				return false;
			}
			refreshReserveList(newReserveList,null,aftReserveList,getReserves());
			for ( ReserveList newr : newReserveList ) {
				if ( newr.getId().equals(cacher.getId()) ) {
					reqr.setTunershort(newr.getTunershort());
					newReserveList.remove(newr);
					newReserveList.add(reqr);
					break;
				}
			}
			
			// リストにいねーじゃんチェックは未実装
		}
		
		// 情報置き換え
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に更新できました。");
		return(true);
	}

	
	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
	@Override
	public ReserveList RemoveRdEntry(String delid) {

		errmsg = "";
		
		System.out.println("Run: RemoveRdEntry()");

		// 削除対象を探す
		ReserveList delr = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				delr = reserve;
				break;
			}
		}
		if (delr == null) {
			return(null);
		}
		
		// とりあえず再ログインしてみる
		if ( doLogin() == false ) {
			errmsg = "ログインに失敗しました.";
			return null;
		}
		
		// POSTデータの入れ物を作る
		HashMap<String, String> pdat = modPostdata(delr,null);
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String response;
		{
			reportProgress("処理IDを取得します(1/3).");
			String[] d = reqDigaGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_list.cgi?ANC_RSVLSTNO="+delid+"&cDMYDT="+DMY_TIME, null);
			response = d[1];
		}
		
		// 削除情報送信
		String[] keys = { "cRVID","cRVORG","cRVORGEX","cRVORGEX2","cRVORGEX3","cRPG","cRHEX","cTSTR","cRHEXEX" };
		for ( String c : keys ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				pdat.put(c,ma.group(1));
			}
		}
		pdat.put("RSV_DEL.x","33");
		pdat.put("RSV_DEL.y","19");
		String pstr = joinPoststr(PostMode.DEL_CMD, pdat);
		{
			reportProgress("削除情報を送信します(2/3).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_edit.cgi", pstr, null);
			response = d[1];
		}
		
		// 削除実行
		HashMap<String, String> dgdat = new HashMap<String, String>();
		
		String[] keys2 = { "cRECMODE", "cRVID", "cRVORG", "cRVORGEX", "cRVORGEX2", "cRVORGEX3", "cRPG", "cRHEX", "cTSTR", "cRHEXEX" };
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response) ;
			if ( ma.find() ) {
				dgdat.put(c,ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","47");
		dgdat.put("RSV_EXEC.y","6");
		pstr = joinPoststr(PostMode.DEL_EXEC, dgdat);
		{
			reportProgress("削除を確定します(3/3).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_delq.cgi", pstr, null);
			response = d[1];
		}

		// 削除結果の返信
		if ( response.contains(DIGAMSG_BUSYNOW) ) {
			errmsg = DIGAMSG_BUSYNOW;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", DIGAMSG_BUSYNOW);
			return(null);
		}
		if ( response.contains(DIGAMSG_CANNOTRESERVE) ) {
			errmsg = DIGAMSG_CANNOTRESERVE ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(null);
		}
		

		// 予約リストを作り直したい！
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		// 予約リスト番号を取得
		{
			ArrayList<ReserveList> aftReserveList = new ArrayList<ReserveList>();
			if ( _getDigaReserveList(aftReserveList,response) < 0 ) {
				errmsg = "登録後の予約IDのリストの取得に失敗しました";
			}
			refreshReserveList(newReserveList,null,aftReserveList,getReserves());
			for ( ReserveList newr : newReserveList ) {
				if ( newr.getId().equals(delr.getId()) ) {
					errmsg = "削除されていません（失敗したようです）。";
				}
			}
			
			// リストにいねーじゃんチェックは未実装
		}
		
		// 情報置き換え
		setReserves(newReserveList);
		ReservesToFile(getReserves(), rsvedFile);	// キャッシュに保存
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に削除できました。");
		return(delr);
	}
	
	/* ここまで */
	
	
	
	
	
	
	/*******************************************************************************
	 * 電源ＯＮ／ＯＦＦ
	 ******************************************************************************/
	
	private void poweronoff(boolean isWakeup) {
		// とりあえず再ログインしてみる
		if ( doLogin() == false ) {
			errmsg = "ログインに失敗しました.";
			return;
		}
		// 電源断かどうか確認する
		String response;
		{
			String[] d = reqDigaGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dispframe.cgi?DISP_PAGE=1001&Radio_Drive=1", null);
			response = d[1];
			if ( response == null ) {
				errmsg = "レコーダが無応答.";
				return;
			}
		}
		boolean isPowOff = response.contains(DIGAMSG_POWOFF);
		if (isWakeup && ! isPowOff) {
			System.out.println("Already wakeup");
			return;
		}
		else if ( ! isWakeup && isPowOff) {
			System.out.println("Already shutdown");
			return;
		}
		
		// 電源入or切
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_POWER.x=61&cCMD_POWER.y=9", null);
			response = d[1];
			//System.out.println(response);
		}
	}

	/*******************************************************************************
	 * 予約で投入する各種設定を整理して一時保存する
	 ******************************************************************************/

	private HashMap<String, String> modPostdata(ReserveList r, String prg_date) {
		
		HashMap<String, String> newdat = new HashMap<String, String>();
		
		// 録画チャンネル（ここは良く落ちる）
		r.setChannel(cc.getCH_WEB2CODE(r.getCh_name()));
		Matcher ma = Pattern.compile("^20([123])$").matcher(r.getChannel());
		if ( ma.find() ) {
			newdat.put("cCHSRC","3");		// 外部入力
			newdat.put("cCHNUM","");
		}
		else {
			String[] s = r.getChannel().split(":");
			newdat.put("cCHSRC",s[1]);		// 外部入力
			newdat.put("cCHNUM",s[0]);
		}
		
		// 録画レート
		String vrval = text2value(vrate,r.getRec_mode());
		newdat.put("cRSPD1", (vrval.equals(VALUE_VIDEO_TYPE_NONE)?(""):(vrval)));
		
		// 日付
		_modPostdata_date( newdat, r.getRec_pattern(), (prg_date!=null)?prg_date:r.getRec_nextdate(), r.getAhh(), r.getPursues(), r.getId() );
		
		// 時刻
		newdat.put("cRVHM1", String.format("%02d%02d",Integer.valueOf(r.getAhh()), Integer.valueOf(r.getAmm())));
		newdat.put("cRVHM2", String.format("%02d%02d",Integer.valueOf(r.getZhh()), Integer.valueOf(r.getZmm())));
		
		// タイトル
		newdat.put("cTHEX", r.getTitle());
		
		// 予約実行
		newdat.put("cTIMER", (r.getExec())?("1"):("0"));
		
		// 作業用
		newdat.put("channel", r.getChannel());
		newdat.put("date", r.getRec_pattern());
		
		return newdat;
	}

	/*******************************************************************************
	 * 予約で投入する各種設定を整理して一時保存する（パターン部分だけ切り出したもの）
	 ******************************************************************************/
	
	private void _modPostdata_date(HashMap<String, String> newdat, String rec_pattern, String nextdate, String Ahh, boolean pursues, String id) {

		// 当日（単日）or最初の日付（繰り返し）
		Matcher ma = Pattern.compile("^(\\d\\d\\d\\d)/(\\d\\d)/(\\d\\d)").matcher(nextdate);
		if ( ma.find() ) {
			newdat.put("cRVMD", ma.group(2)+ma.group(3));
		}
		
		if ( pursues && (id != null && id.length() > 0) ) {
			// 番組追従ありの更新の場合、曜日フラグはdisabled
			return;
		}
		
		int i = 0;
		for ( ; i < HDDRecorder.RPTPTN.length && HDDRecorder.RPTPTN[i].equals(rec_pattern) == false; i++ ) {
			// 処理はないよ
		}
		
		if ( 0 <= i && i <= 10 ) {
			if ( i <= 6 ) {
				// 毎週予約
				newdat.put(DIGA_WDPTNSTR[i], DIGA_WDPTNCODE[i]);
			}
			else if ( 7 <= i && i <= 9 ) {
				// 帯予約（月～木・金・土）
				int d = ((CommonUtils.isLateNight(Ahh))?(1):(0));
				for ( int c=1; c <= i-3; c++ ) {
					newdat.put(DIGA_WDPTNSTR[(c+d)%7], DIGA_WDPTNCODE[(c+d)%7]);
				}
			}
			else {	// 毎日
				newdat.put(DIGA_WDPTNSTR[DIGA_WDPTNSTR.length-1],DIGA_WDPTNCODE[DIGA_WDPTNSTR.length-1]);
			}
		}
	}
	
	protected String[] joinArrays(String[] a1,String[] a2,String[] a3,String[] a4) {
		
		ArrayList<String> ar = new ArrayList<String>();
		
		if ( a1 != null ) {
			for ( String s : a1 ) {
				ar.add(s);
			}
		}
		if ( a2 != null ) {
			for ( String s : a2 ) {
				ar.add(s);
			}
		}
		if ( a3 != null ) {
			for ( String s : a3 ) {
				ar.add(s);
			}
		}
		if ( a4 != null ) {
			for ( String s : a4 ) {
				ar.add(s);
			}
		}
		
		return (String[])ar.toArray(new String[0]);
	}
	
	
	/*******************************************************************************
	 * 一時保存した各種設定をPOSTデータに変換する
	 ******************************************************************************/
	
	private String joinPoststr(PostMode mode, HashMap<String, String> pdat) {

		HashMap<PostMode, String[]> keymap = getPostKeys();
		String[] keys = keymap.get(mode);
		if ( keys == null ) {
			System.err.println("【エラー】未定義のPostMode: "+mode);
			return null;
		}
		
		String pstr = "";
		try {
			for ( String key : keys ) {
				for ( String s : DIGA_WDPTNSTR ) {
					if (pdat.get(key) != null) {
						if ( key.equals(s) ) {
							if ( pdat.get(key).equals("") ) {
								continue;
							}
						}
					}
				}
				if (pdat.get(key) != null) {
					if (key.equals("cTHEX")) {
						try {
							pdat.put(key, URLEncoder.encode(pdat.get(key),"MS932"));
						} catch (UnsupportedEncodingException e) {
							// 例外
						}
					}
					else {
						pdat.put(key, pdat.get(key));
					}
					Matcher ma = Pattern.compile(" ").matcher(pdat.get(key));
					pdat.put(key, ma.replaceAll("+"));
					pstr += key+"="+pdat.get(key)+"&";
				}
			}
			
			pstr = pstr.substring(0, pstr.length()-1);
		}
		catch ( Exception e) {
			e.printStackTrace();
		}
		System.out.println("POST data: "+pstr);
		return(pstr);
	}
	
	protected static enum PostMode { ADD_CMD, ADD_EXEC, UPD_CMD, UPD_EXEC, DEL_CMD, DEL_EXEC, ERR_OK };
	
	protected HashMap<PostMode, String[]> getPostKeys() {
		
		String[] pkeys1a = {"cRVMD","cRVHM1","cRVHM2","cCHSRC","cCHNUM","cRSPD1"};
		String[] pkeys1b = {"cTHEX","cTIMER"};
		String[] pkeys1 = joinArrays(pkeys1a, DIGA_WDPTNSTR, pkeys1b, null);
		String[] pkeys2 = { "cRVID","cRVORG","cRVORGEX","cRVORGEX2","cRVORGEX3" };
		String[] pkeys3 = {	"cRPG","cRHEX","cTSTR","cRHEXEX" };
		String[] pkeys4 = {	"RSV_FIX.x","RSV_FIX.y" };
		String[] pkeys5 = {	"RSV_EXEC.x","RSV_EXEC.y" };
		String[] pkeys6 = { "RSV_DEL.x","RSV_DEL.y" };
		String[] pkeys7 = { "RSV_EDIT.x","RSV_EDIT.y" };
		String[] pkeys8 = {	"cRPG","cERR","TTL_DRIVE","cRVID","cRHEX","cTSTR","cRHEXEX","Image_BtnRyoukai.x","Image_BtnRyoukai.y" };
		
		HashMap<PostMode, String[]> keys = new HashMap<PostMode, String[]>();
		keys.put(PostMode.ADD_CMD,  joinArrays( pkeys1, pkeys3, pkeys4, null ));
		keys.put(PostMode.ADD_EXEC, joinArrays( pkeys3, pkeys5, null, null ));
		keys.put(PostMode.UPD_CMD,  joinArrays( pkeys1, pkeys2, pkeys3, pkeys7 ));
		keys.put(PostMode.UPD_EXEC, joinArrays( pkeys2, pkeys3, pkeys5, null ));
		keys.put(PostMode.DEL_CMD,  joinArrays( pkeys1, pkeys2, pkeys3, pkeys6 ));
		keys.put(PostMode.DEL_EXEC, joinArrays( pkeys2, pkeys3, pkeys5, null ));
		keys.put(PostMode.ERR_OK,   joinArrays( pkeys8, null, null, null ));
		
		return keys;
	}
	
	/*******************************************************************************
	 * DIGAで利用できる各種選択肢を取得する
	 ******************************************************************************/
	
	private boolean getDigaRecordSetting() {
		
		System.out.println("Run: getDigaRecordSetting");
		
		int ret = 0 ;
		for ( int cnt=1; (ret = _getDigaRecordSetting()) < 0 && cnt <= get_com_try_count(); cnt++ ) {
			if ( cnt < get_com_try_count() ) {
				reportProgress(String.format("+設定情報の取得を再試行します (%d回中%d回目)",(get_com_try_count()-1),cnt));
				CommonUtils.milSleep(WAIT_FOR_LOGIN);
			}
		}
		return (ret == 0);
	}
	
	private int _getDigaRecordSetting() {
		
		// 旧リストは全部削除
		vrate.clear();
		chvalue.clear();
		
		// リクエスト発行 
		String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVADD.x=31&cCMD_RSVADD.y=8", null);
		String response = d[1];
			
		if (response == null) {
			// エラーになった場合
			System.out.println(ERRID+"レコーダから設定情報が返却されませんでした.");
			return(-99);
		}
		
		if ( response.contains(DIGAMSG_WAITFORLOGIN) ) {
			// リトライしてね
			if (getDebug()) System.out.println(DBGID+DIGAMSG_WAITFORLOGIN);
			return(-1);
		}
		if ( response.contains(DIGAMSG_PLEASELOGIN) ) {
			// 再ログインしてね
			if (getDebug()) System.out.println(DBGID+DIGAMSG_PLEASELOGIN);
			return(-2);
		}
		
		// 画質
		setSettingVrate(vrate,response);
		if ( vrate.size() == 0 ) {
			System.out.println(ERRID+"録画画質の選択肢を取得できませんでした。録画先ドライブにHDD以外が選択されているなどが考えられます.");
			return(-99);
		}
		TVSsave(vrate, vrateTFile);
		
		// チャンネルの種類
		setSettingChCodeValue(chvalue,response);
		TVSsave(chvalue, chValueTFile);
		
		return(0);
	}

	/*******************************************************************************
	 * HTMLから画質設定を抽出する
	 ******************************************************************************/

	protected void setSettingVrate(ArrayList<TextValueSet> vr, String response) {
		vr.clear();
		Matcher ma = Pattern.compile(" name=\"cRSPD1\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			//
			//ArrayList<TextValueSet> hdd = new ArrayList<TextValueSet>();
			Matcher mb = Pattern.compile("<option value=\"(.+?)\".*?>(&nbsp;)*(.+?)\\(HDD\\)</option>").matcher(ma.group(1));	// 謎の&nbsp;
			while (mb.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(3));
				t.setValue(mb.group(1));
				vr.add(t);
			}
			if ( vr.size() == 0 ) {
				return;
			}
			
			//
			ArrayList<TextValueSet> usb = new ArrayList<TextValueSet>();
			mb = Pattern.compile("<option value=\"(.+?)\".*?>(&nbsp;)*(.+?)\\(USB\\)</option>").matcher(ma.group(1));	// 謎の&nbsp;
			while (mb.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(3)+"(USB)");
				t.setValue(mb.group(1));
				usb.add(t);
			}
			if ( usb.size() == 0 ) {
				for ( TextValueSet h : vr ) {
					TextValueSet t = new TextValueSet();
					t.setText(h.getText()+"(USB)");
					t.setValue(String.valueOf(Integer.valueOf(h.getValue())+200));
					usb.add(t);
				}
			}
			for ( TextValueSet t : usb ) {
				vr.add(t);
			}
		}
		TextValueSet t = new TextValueSet();
		
		t.setText(ITEM_VIDEO_TYPE_NONE);
		t.setValue(VALUE_VIDEO_TYPE_NONE);
		vr.add(t);
	}
	
	/*******************************************************************************
	 * HTMLからCHコードを抽出する
	 ******************************************************************************/
	
	protected void setSettingChCodeValue(ArrayList<TextValueSet> cv, String response) {
		cv.clear();
		Matcher ma = Pattern.compile(" name=\"cCHSRC\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\".*?>(.+?)</option>").matcher(ma.group(1));
			while (mb.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(2));
				t.setValue(mb.group(1));
				cv.add(t);
			}
		}
	}
	
	/*******************************************************************************
	 * HTMLから各種設定を抽出する
	 ******************************************************************************/
	
	private void setSettingEncoder(ArrayList<TextValueSet> enc) {
		enc.clear();
		if ( getTunerNum() >= 2 ) {
			for ( int i=1; i<=getTunerNum(); i++ ) {
				TextValueSet t = new TextValueSet();
				t.setText("D"+i);
				t.setValue("D"+i);
				enc.add(t);
			}
		}
	}
	
	/*******************************************************************************
	 * 予約種別は固定
	 ******************************************************************************/
	
	private void setSettingRecType(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		add2tvs(tvs,ITEM_REC_TYPE_PROG,VALUE_REC_TYPE_PROG);
		add2tvs(tvs,ITEM_REC_TYPE_EPG,VALUE_REC_TYPE_EPG);
	}

	/*******************************************************************************
	 * 予約のタイトル一覧を取得する。詳細は{@link #getDigaReserveDetail}で。
	 ******************************************************************************/
	
	private int getDigaReserveList(ArrayList<ReserveList> newReserveList, ArrayList<ReserveList> oldReserveList) {
		
		System.out.println("Run: getDigaReserveList");

		reportProgress("予約一覧（詳細を除く）を取得します.");
		
		// リクエスト発行
		String[] d = reqDigaPOST("http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVLST.x=39&cCMD_RSVLST.y=16", null);
		String response = d[1];
		if (response == null) {
			// エラーになった場合
			errmsg = "予約一覧を取得できませんでした.";
			return(RETCODE_FATAL);
		}
		
		int n = _getDigaReserveList(newReserveList, response);

		// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
		if ( oldReserveList != null ) {
			for ( ReserveList oldr : oldReserveList ) {
				for ( ReserveList newr : newReserveList ) {
					if ( oldr.getId().equals(newr.getId()) ) {
						copyAttribute(newr,oldr);
						newr.setTitle(oldr.getTitle());
						newr.setTitlePop(oldr.getTitlePop());
						break;
					}
				}
			}
		}
		
		return n;
	}
	
	/**
	 * 予約全エントリを通しての処理
	 */
	private int _getDigaReserveList(ArrayList<ReserveList> newReserveList, String response) {
		// 予約詳細を作る
		Matcher mx = Pattern.compile("<table class=\"reclist\">.+?<tbody>(.+?)</tbody>",Pattern.DOTALL).matcher(response);
		if ( mx.find() ) {
			// BWT650
			Matcher ma = Pattern.compile("<tr>(.+?)</tr>",Pattern.DOTALL).matcher(mx.group(1));
			while ( ma.find() ) {
				_getDigaReserveProg(newReserveList, ma.group(1));
			}
		}
		else {
			Matcher ma = Pattern.compile("<tr class=\".*?\">(.+?)</tr>",Pattern.DOTALL).matcher(response);
			while ( ma.find() ) {
				_getDigaReserveProg(newReserveList, ma.group(1));
			}
		}
		return(RETCODE_SUCCESS);
	}
	
	/**
	 * 予約１エントリずつの処理
	 */
	private int _getDigaReserveProg(ArrayList<ReserveList> newReserveList, String progdata) {
		
		ReserveList reqr = new ReserveList();
		
		// 予約内容の部分を切り出す
		String id = null;
		String date = null;
		int wday = 0;
		String channel = null;
		String ch_name = null;
		String ahh = null;
		String amm = null;
		String zhh = null;
		String zmm = null;
		String rec_min = null;
		String rec_mode = null;
		
		boolean pursues = false;
		boolean exec = true;
		boolean tunershort = false;
		String rec_type = ITEM_REC_TYPE_PROG;
		
		boolean repeat_everyday = false;
		boolean repeat_mon2fri = false;
		boolean repeat_mon2sat = false;
		boolean repeat_wday = false;
		boolean repeat_none = false;

		Matcher mb = Pattern.compile("<td(.*?)</td>",Pattern.DOTALL).matcher(progdata);
		Matcher mc = null;
		for ( int i=0; mb.find(); i++ ) {
			switch (i) {
			case 0:		// 予約ID
				mc = Pattern.compile("\\?ANC_RSVLSTNO=(\\d+)&",Pattern.DOTALL).matcher(mb.group(1));
				if ( mc.find() ) {
					id = mc.group(1);
				}
				break;
			case 2:		// 日付
				mc = Pattern.compile(">\\s*(\\d+)/(\\d+)\\((.)\\)",Pattern.DOTALL).matcher(mb.group(1));
				if ( mc.find() ) {
					wday = CommonUtils.getWday(mc.group(3));
					date = CommonUtils.getDateByMD(Integer.valueOf(mc.group(1)), Integer.valueOf(mc.group(2)), wday, true);
				}
				break;
			case 3:		// CH
				mc = Pattern.compile(">\\s*(.+?)\\s*$",Pattern.DOTALL).matcher(mb.group(1));
				if ( mc.find() ) {
					String recChName = CommonUtils.unEscape(mc.group(1)).trim();
					channel = cc.getCH_REC2CODE(recChName);
					if ( channel != null ) {
						ch_name = cc.getCH_CODE2WEB(channel);
					}
					else {
						ch_name = recChName;
					}
				}
				break;
			case 4:		// 時刻
				mc = Pattern.compile(">\\s*(\\d+):(\\d+)～((\\d+):(\\d+)|未定)",Pattern.DOTALL).matcher(mb.group(1));
				if ( mc.find() ) {
					if ( mc.group(4) == null ) {
						// 終了時刻＝'未定(fefe)'の場合はcopyattributes()で過去情報から補完するか、または２時間後に仮置き
						System.out.println(DBGID+"終了時刻がみつかりません");
						String[] db = _hhmm2hhmm_min(mc.group(1)+":"+mc.group(2),"00:00");
						ahh = db[0];
						amm = db[1];
					}
					else {
						String[] db = _hhmm2hhmm_min(mc.group(1)+":"+mc.group(2),mc.group(4)+":"+mc.group(5));
						ahh = db[0];
						amm = db[1];
						zhh = db[2];
						zmm = db[3];
						rec_min = db[4];
					}
				}
				break;
			case 5:		// 録画モード
				mc = Pattern.compile(">\\s*([^<]+?)\\s*<",Pattern.DOTALL).matcher(mb.group(1));
				if ( mc.find() ) {
					rec_mode = CommonUtils.unEscape(mc.group(1)).trim().replaceFirst("\\(HDD\\)", "");
				}
				else {
					// BWT650
					mc = Pattern.compile(">\\s*(.+?)\\s*$",Pattern.DOTALL).matcher(mb.group(1));
					if ( mc.find() ) {
						rec_mode = CommonUtils.unEscape(mc.group(1)).trim().replaceFirst("\\(HDD\\)", "");
					}
				}
				break;
			case 6:		// 補足情報
				if ( mb.group(1).contains("毎日") ) {
					repeat_everyday = true;
				}
				else if ( mb.group(1).contains("月金") ) {
					repeat_mon2fri = true;
				}
				else if ( mb.group(1).contains("月土") ) {
					repeat_mon2sat = true;
				}
				else if ( mb.group(1).contains("毎週") || mb.group(1).contains("曜日指定") ) {
					repeat_wday = true;
				}
				else {
					repeat_none = true;
				}
				
				if ( mb.group(1).contains("実行切") ) {
					exec = false;
				}
				if ( mb.group(1).contains("重複") ) {
					tunershort = true;
				}
				if ( mb.group(1).contains("番組予約") ) {
					pursues = true;
					rec_type = ITEM_REC_TYPE_EPG;
				}
				break;
			}
		}
		
		// 予約情報更新
		reqr.setId(id);
		
		reqr.setAhh(ahh);
		reqr.setAmm(amm);
		
		reqr.setZhh(zhh);
		reqr.setZmm(zmm);
		reqr.setRec_min(rec_min);
		
		if ( repeat_everyday ) {
			// 毎日
			reqr.setRec_pattern(HDDRecorder.RPTPTN[HDDRecorder.RPTPTN_ID_EVERYDAY]);
			reqr.setRec_pattern_id(HDDRecorder.RPTPTN_ID_EVERYDAY);
		}
		else if ( repeat_mon2sat ) {
			// 毎月～土
			reqr.setRec_pattern(HDDRecorder.RPTPTN[HDDRecorder.RPTPTN_ID_MON2SAT]);
			reqr.setRec_pattern_id(HDDRecorder.RPTPTN_ID_MON2SAT);
		}
		else if ( repeat_mon2fri ) {
			// 毎月～金
			reqr.setRec_pattern(HDDRecorder.RPTPTN[HDDRecorder.RPTPTN_ID_MON2FRI]);
			reqr.setRec_pattern_id(HDDRecorder.RPTPTN_ID_MON2FRI);
		}
		else if ( repeat_wday && wday > 0 ) {
			// 毎週
			reqr.setRec_pattern(HDDRecorder.RPTPTN[wday-1]);
			reqr.setRec_pattern_id(wday-1);
		}
		else {
			// 単日
			reqr.setRec_pattern(date);
			reqr.setRec_pattern_id(HDDRecorder.RPTPTN_ID_BYDATE);
		}
		if ( zhh != null && zmm != null ) {
			reqr.setRec_nextdate(CommonUtils.getNextDate(reqr));
			getStartEndDateTime(reqr);
		}
		else {
			// 終了時刻="未定"の場合（setRec_nextdateとかは↓の中で）
			setAttributesDiga(reqr,null);
		}
		
		if ( pursues && ! repeat_none ) {
			reqr.setAutoreserved(true);
		}
		
		reqr.setRec_mode(rec_mode);
		reqr.setTitle(MISS_TITLE);
		reqr.setTitlePop("");
		reqr.setChannel(channel);
		reqr.setCh_name(ch_name);
		
		reqr.setPursues(pursues);
		reqr.setExec(exec);
		reqr.setContentId(null);
		reqr.setRec_audio(rec_type);

		reqr.setTunershort(tunershort);

		// 予約情報を保存
		newReserveList.add(reqr);
		
		return 0;
	}
	
	/*******************************************************************************
	 * 予約の詳細情報を取得する（全部）
	 ******************************************************************************/
	private boolean getDigaReserveDetails(ArrayList<ReserveList> newReserveList, ArrayList<ReserveList> oldReserveList) {
		
		reportProgress(String.format("予約一覧（詳細）を取得します(%d)",oldReserveList.size()));
		
		// 詳細情報の取得
		System.out.println("========");
		for (int i=0; i<oldReserveList.size(); i++) {
			
			ReserveList oldr = oldReserveList.get(i);
			ReserveList newr = new ReserveList();
			
			getDigaReserveDetail(newr, oldr.getId());
			
			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributes(newr, getReserves());
			
			//
			if ( newr.getContentId() != null ) {
				StringBuilder dt = new StringBuilder();
				dt.append("■番組ID：");
				dt.append(newr.getContentId());
				dt.append("\n");
				String cid = dt.toString();
				if ( newr.getDetail() != null ) {
					if ( newr.getDetail().contains("■番組ID：") ) {
						newr.setDetail(newr.getDetail().replaceFirst("■番組ID：.*?\n", Matcher.quoteReplacement(cid)));
					}
					else {
						newr.setDetail(newr.getDetail()+cid);
					}
				}
				else {
					newr.setDetail(cid);
				}
			}
			
			newReserveList.add(newr);

			reportProgress(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s\t%s\t%s",
					(i+1), newr.getId(), newr.getRec_pattern(), newr.getRec_nextdate(), newr.getAhh(), newr.getAmm(), newr.getZhh(),	newr.getZmm(),	newr.getRec_min(), newr.getRec_mode(), newr.getTitle(), newr.getChannel(), newr.getCh_name(), newr.getContentId()));
		}
		System.out.println("========");
		
		return true;
	}
	
	/*******************************************************************************
	 * 予約の詳細情報を取得する（個別）
	 ******************************************************************************/
	
	private ReserveList getDigaReserveDetail(ReserveList reserve_d, String id) {

		String response;
		{
			String url = "http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/reserve_list.cgi";
			String pstr = "ANC_RSVLSTNO="+id+"&cDMYDT="+DMY_TIME;
			//String[] d = reqPOST(url,pstr,null);
			String[] d = reqGET(url+"?"+pstr,null);
			response = d[1];
		}
		if ( response == null ) {
			System.out.println(ERRID+"★");
			System.out.println(ERRID+"★ 予約情報を取得できませんでした。");
			System.out.println(ERRID+"★");
			return(null);
		}
		
		// 予約日付
		Matcher ma = Pattern.compile("name=\"cRVMD\" value=\"(\\d\\d)(\\d\\d)\"").matcher(response);
		if (! ma.find()) {
			System.err.println(ERRID+"日付情報がみつかりません");
			return(null);
		}
		String[] da = _mmdd2yyyymmdd(ma.group(1), ma.group(2));
		String yy = da[0];
		String mm = da[1];
		String dd = da[2];
		
		// 開始終了時刻と録画時間
		ma = Pattern.compile("name=\"cRVHM1\" value=\"(\\d\\d)(\\d\\d)\"").matcher(response);
		if (! ma.find()) {
			System.err.println(ERRID+"開始時刻がみつかりません");
			return(null);
		}
		String ahh = ma.group(1);
		String amm = ma.group(2);
		
		String zhh = null;
		String zmm = null;
		String rec_min = null;
		ma = Pattern.compile("name=\"cRVHM2\" value=\"(.+?)\"",Pattern.DOTALL).matcher(response);
		if ( ! ma.find()) {
			System.err.println(ERRID+"終了時刻がみつかりません");
			return(null);
		}
		else {
			Matcher mb = Pattern.compile("^(\\d\\d)(\\d\\d)$").matcher(ma.group(1));
			if ( mb.find() ) {
				zhh = mb.group(1);
				zmm = mb.group(2);
				
				String[] db = _hhmm2hhmm_min(ahh+":"+amm,zhh+":"+zmm);
				ahh = db[0];
				amm = db[1];
				zhh = db[2];
				zmm = db[3];
				rec_min = db[4];
			}
			else {
				// 終了時刻＝'未定(fefe)'の場合はcopyattributes()で過去情報から補完するか、または２時間後に仮置き
				System.out.println(DBGID+"終了時刻がみつかりません");
			}
		}
		
		// 画質
		boolean pursues = false;
		String rec_mode = getRsvInfoVrateDT(vrate, response);
		if (rec_mode == null) {
			rec_mode = getRsvInfoVrateTR(vrate, response);
			if (rec_mode != null) {
				// 追跡あり？
				pursues = true;
			}
		}
		if ( rec_mode == null ) {
			System.err.println(ERRID+"画質情報がみつかりません");
			return(null);
		}
		
		// チャンネル
		String ch_source = "";
		String ch_number = "";
		String ch_name = "";
		String channel = "";
		ma = Pattern.compile(" name=\"cCHSRC\">(.+?)</select>",Pattern.DOTALL).matcher(response);
		if (ma.find()) {
			// 手動
			Matcher mb = Pattern.compile("<option value=\"([^\"]+?)\" selected>",Pattern.DOTALL).matcher(ma.group(1));
			Matcher mc = Pattern.compile("name=\"cCHNUM\" value=\"(.+?)\"",Pattern.DOTALL).matcher(response);
			if ( ! mb.find() || ! mc.find()) {
				System.err.println(ERRID+"チャンネル設定がみつかりません");
				return(null);
			}
			else {
				ch_source = mb.group(1);
				ch_number = mc.group(1);
				
				String ch_source_text = value2text(chvalue, ch_source);
				if ( ! ch_source_text.equals("") ) {
					channel = ch_source_text+" "+ch_number;
					ch_name = cc.getCH_REC2WEB(channel);
				}
				else {
					System.out.println(DBGID+"認識できない放送波種別: "+ch_source);
				}
			}
		}
		else {
			// 自動
			Matcher mb = Pattern.compile("<input type=\"hidden\" name=\"cCHSRC\" value=\"(.+?)\">\\s*(.+?)(&nbsp;)*\\s*<").matcher(response);
			Matcher mc = Pattern.compile("name=\"cCHNUM\" value=\"(.+?)\"").matcher(response);
			if ( ! mb.find() || ! mc.find()) {
				System.err.println(ERRID+"チャンネル設定がみつかりません");
				return(null);
			}
			else {
				ch_source = mb.group(1);
				ch_number = mc.group(1);
				
				String ch_source_text = value2text(chvalue, ch_source);
				if ( ! ch_source_text.equals("") ) {
					channel = ch_source_text+" "+ch_number;
					ch_name = cc.getCH_REC2WEB(channel);
				}
				else {
					System.out.println(DBGID+"認識できない放送波種別: "+ch_source);
					
					{
						// 通常は選択できない特殊なチャンネルコード
						TextValueSet t = new TextValueSet();
						t.setText(mb.group(2));
						t.setValue(mb.group(1));
						chvalue.add(t);
						TVSsave(chvalue, chValueTFile);
					}
					
					channel = mb.group(2)+" "+mc.group(1);
					ch_name = cc.getCH_REC2WEB(channel);
				}
			}
		}
		
		// パターン
		ma = Pattern.compile("<td width=\"92\" class=\"s_F16_Cffffff\">毎週</td>(.+?)</tr>",Pattern.DOTALL).matcher(response);
		if ( ! ma.find()) {
			// BWT650
			ma = Pattern.compile("<td width=\"20%\">毎週</td>(.+?)</tr>",Pattern.DOTALL).matcher(response);
			if ( ! ma.find()) {
				System.err.println(ERRID+"予約パターン情報がみつかりません");
				return(null);
			}
		}
		String rec_pattern = _recpatternDG2RDstr(ma.group(1), yy, mm, dd, ahh);
		
		// タイトル
		ma = Pattern.compile("name=\"cTHEX\" value=\"([^\"]*?)\"").matcher(response);
		if ( ! ma.find()) {
			System.err.println(ERRID+"タイトルがみつかりません");
			return(null);
		}
		String title = CommonUtils.unEscape(ma.group(1));
		
		// 予約実行
		boolean exec = true;
		ma = Pattern.compile(" name=\"cTIMER\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\" selected>").matcher(ma.group(1));
			if ( ! mb.find()) {
				System.err.println(ERRID+"実行情報がみつかりません");
				return(null);
			}
			else {
				 exec = (mb.group(1).equals("0"))?(false):(true);
			}
		}
		
		// 番組ID
		String contentid = null;
		boolean miss = false;
		ma = Pattern.compile(" name=\"cRHEXEX\" value=\"..([0-9a-fA-Z]{4})([0-9a-fA-Z]{12})").matcher(response);
		if ( ma.find() ) {
			int evid = Integer.decode("0x"+ma.group(1));
			if ( evid != DIGAEVID_NONE && evid != DIGAEVID_PROGRSV ) {
				if ( ContentIdEDCB.decodeChId(ma.group(2)) ) {
					contentid = ContentIdEDCB.getContentId(evid);
				}
				if ( evid == DIGAEVID_CANNOTFOLLOW ) {
					// DIGAの番組表が更新されていないか、マッチする番組が見つかっていない。
					miss = true;
				}
			}
		}
		
		// 予約情報更新
		reserve_d.setId(id);
		
		reserve_d.setAhh(ahh);
		reserve_d.setAmm(amm);
		
		reserve_d.setZhh(zhh);
		reserve_d.setZmm(zmm);
		reserve_d.setRec_min(rec_min);
		
		reserve_d.setRec_pattern(rec_pattern);
		reserve_d.setRec_pattern_id(getRec_pattern_Id(rec_pattern));
		if ( zhh != null && zmm != null ) {
			reserve_d.setRec_nextdate(CommonUtils.getNextDate(reserve_d));
			getStartEndDateTime(reserve_d);
		}
		
		reserve_d.setRec_mode(rec_mode);
		reserve_d.setTitle((miss?MISS_HDR:"")+title);
		reserve_d.setTitlePop(TraceProgram.replacePop(title));
		reserve_d.setChannel(channel);
		reserve_d.setCh_name(ch_name);
		
		reserve_d.setPursues(pursues);
		reserve_d.setExec(exec);
		reserve_d.setContentId(contentid);
		if ( contentid != null ) {
			reserve_d.setRec_audio(ITEM_REC_TYPE_EPG);
			if ( reserve_d.getRec_pattern_id() != HDDRecorder.RPTPTN_ID_BYDATE ) {
				// EPG予約で、かつ単日予約でないものは自動予約と思われる
				reserve_d.setAutoreserved(true);
			}
		}
		else {
			reserve_d.setRec_audio(ITEM_REC_TYPE_PROG);
		}
		
		return(reserve_d);
	}

	/*******************************************************************************
	 * 録画結果の一覧を取得する
	 ******************************************************************************/
	
	private int getDigaRecordedList(ArrayList<RecordedInfo> newRecordedList) {
		
		System.out.println("Run: getDigaRecordedList");

		String baseurl = "http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/vttl_list.cgi?VT_TITLEID=&cCMD_VT_SELECT=";
		
		// リクエスト発行
		int pagecnt = 0;
		String topResult = null;
		{
			reportProgress("録画結果一覧を取得します.(1/-)");
			
			String url = baseurl+String.valueOf(0);;
			String[] d = reqDigaGET(url, null);
			if (d[1] == null) {
				// エラーになった場合
				errmsg = "録画結果一覧を取得できませんでした.";
				return(RETCODE_FATAL);
			}
			
			topResult = d[1];
			
			int reccnt = 0;
			Matcher ma = Pattern.compile("番組数：.*?(\\d+)",Pattern.DOTALL).matcher(d[1]);
			if ( ma.find() ) {
				reccnt = Integer.valueOf(ma.group(1));
				int n = reccnt % RECITEMPERPAGE;
				pagecnt = (reccnt-n)/RECITEMPERPAGE + (n==0?0:1);
			}
		}
		
		for ( int p=0; p<pagecnt && p<recPageMax; p++ ) {
			
			int curpage = pagecnt-p-1;
			
			String result = null;
			if ( curpage == 0 ) {
				reportProgress(String.format("取得済みの録画結果一覧を利用します.(%d/%d)",pagecnt-p,pagecnt));
				result = topResult;
			}
			else {
				reportProgress(String.format("録画結果一覧を取得します.(%d/%d)",pagecnt-p,pagecnt));
				
				String url = baseurl+String.valueOf(curpage*RECITEMPERPAGE);
				String[] d = reqDigaGET(url, null);
				if (d[1] == null) {
					// エラーになった場合
					errmsg = "録画結果一覧を取得できませんでした.";
					return(RETCODE_FATAL);
				}
				
				result = d[1];
			}
			
			if ( _getDigaRecordedList(newRecordedList,result) == 0 ) {
				// 追加が０件なら終わってもいいかな
				break;
			}
		}
		
		return RETCODE_SUCCESS;
	}
	
	private int _getDigaRecordedList(ArrayList<RecordedInfo> newRecordedList, String response) {
		
		ArrayList<RecordedInfo> tmpRecordedList = new ArrayList<RecordedInfo>();
		
		Matcher ma = Pattern.compile("<input type=\"checkbox\" name=\"chk(.+?)</tr>",Pattern.DOTALL).matcher(response);
		while ( ma.find() ) {
			String date = null;
			String start = null;
			String recChName = null;
			String title = null;
			Matcher mb = Pattern.compile("<td [^>]*?>(<div [^>]*?>)?<font style=\".*?\">\\s*(.*?)\\s*</font>",Pattern.DOTALL).matcher(ma.group(1));
			for ( int i=1; mb.find(); i++ ) {
				String val = CommonUtils.unEscape(mb.group(2));
				switch ( i ) {
				case 1:
					date = "20"+val;
					break;
				case 3:
					recChName = val;
					break;
				case 4:
					start = val;
					break;
				case 5:
					title = val;
					break;
				default:
					break;
				}
			}
			
			RecordedInfo entry = new RecordedInfo();
			
			GregorianCalendar ca = CommonUtils.getCalendar(date+" "+start);
			if ( ca == null ) {
				// もうエントリがないっぽい
				break;
			}
			
			entry.setDate(CommonUtils.getDate(ca));
			entry.setAhh(String.format("%02d",ca.get(Calendar.HOUR_OF_DAY)));
			entry.setAmm(String.format("%02d",ca.get(Calendar.MINUTE)));
			entry.setLength(RECLENGTHDEFAULT);
			ca.add(Calendar.MINUTE, entry.getLength());
			entry.setZhh(String.format("%02d",ca.get(Calendar.HOUR_OF_DAY)));
			entry.setZmm(String.format("%02d",ca.get(Calendar.MINUTE)));
			
			String chid = cc.getCH_REC2CODE(recChName);
			if ( chid == null ) {
				// CHコードにできなければ、HTMLから取得した放送局名をそのまま使う
				entry.setChannel(null);
				entry.setCh_name(recChName);
			}
			else {
				entry.setChannel(chid);
				String webChName = cc.getCH_CODE2WEB(chid);
				if ( webChName == null ) {
					// CHコード設定がうまくないようですよ？
					entry.setCh_name(recChName);
				}
				else {
					entry.setCh_name(webChName);
				}
			}
			entry.setCh_orig(recChName);
			
			entry.setTitle(title);
			
			entry.setResult("DIGAでは終了～MPEGの値が取得できません");
			
			entry.setSucceeded(true);
			
			tmpRecordedList.add(0,entry);	// テンポラリは日時昇順に構築
		}
		
		int addcnt = 0;
		for ( RecordedInfo e : tmpRecordedList ) {
			boolean isExists = false;
			for ( RecordedInfo d : newRecordedList ) {
				// 既存エントリか？
				if ( d.getDate().equals(e.getDate()) &&
						d.getAhh().equals(e.getAhh()) &&
						d.getAmm().equals(e.getAmm()) &&
						d.getTitle().equals(e.getTitle()) ) {
					isExists = true;
					break;
				}
			}
			if ( ! isExists ) {
				// 既存でなければ挿入（日時降順）
				newRecordedList.add(e);
				++addcnt;
			}
		}
		
		return addcnt;	// 追加0件なら即終了してほしい
	}
	
	/*******************************************************************************
	 * 番組の画質設定を取得する（番組追従なし）
	 ******************************************************************************/

	protected String getRsvInfoVrateDT(ArrayList<TextValueSet> vr, String response) {
		// 手動予約
		Matcher ma = Pattern.compile(" name=\"cRSPD1\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\" selected>").matcher(ma.group(1));
			if ( ! mb.find()) {
				return (null);
			}
			else {
				String rec_mode = value2text(vr, mb.group(1));
				if ( rec_mode.equals("") ) {
					System.err.println("未定義の画質設定が検出されました(手動予約): "+mb.group(1));
					return null;
				}
				return rec_mode;
			}
		}
		return null;
	}
	
	/*******************************************************************************
	 * 番組の画質設定を取得する（番組追従あり）
	 ******************************************************************************/
	
	protected String getRsvInfoVrateTR(ArrayList<TextValueSet> vr, String response) {
		// 番組表予約（追跡あり）
		Matcher ma = Pattern.compile("<input type=\"hidden\" name=\"cRSPD1\" value=\"(.+?)\">").matcher(response);
		if (ma.find()) {
			String rec_mode = value2text(vr, ma.group(1));
			if ( rec_mode.equals("") ) {
				System.err.println("未定義の画質設定が検出されました(番組表予約): "+ma.group(1));
				return null;
			}
			return rec_mode;
		}
		ma = Pattern.compile("<input type=\"hidden\" name=\"cRSPD1\">").matcher(response);
		if (ma.find()) {
			// スカパーＨＤ予約
			return ITEM_VIDEO_TYPE_NONE;
		}
		return(null);
	}
	

	/*******************************************************************************
	 * 1.予約登録前と予約登録後の予約一覧を比較して、追加された予約IDをみつける（新規予約のみ）
	 * 2.既存の予約一覧にないものがあったら取り込んでおく
	 * 
	 * @param newReserveList 新しく作る予約一覧
	 * @param preReserveList 登録前予約一覧
	 * @param aftReserveList 登録後予約一覧
	 * @param curReserveList 既存の予約一覧
	 * @return 新規予約があればそれを返す。それ以外はnull。
	 ******************************************************************************/
	private ReserveList refreshReserveList(ArrayList<ReserveList> newReserveList, ArrayList<ReserveList> preReserveList, ArrayList<ReserveList> aftReserveList, ArrayList<ReserveList> curReserveList) {
		
		ReserveList newr = null;
		
		for ( ReserveList ar : aftReserveList ) {
			
			ReserveList nr = null;
			
			if ( preReserveList != null ) {
				// 新規登録の場合
				for ( ReserveList pr : preReserveList ) {
					if ( ar.getId().equals(pr.getId()) ) {
						nr = ar;
						break;
					}
				}
			}
			else {
				// 更新の場合
				nr = ar;
			}
			
			if ( nr != null ) {
				// 新規に登録した予約ではないね
				for ( ReserveList cr : curReserveList ) {
					if ( ar.getId().equals(cr.getId()) ) {
						// 前から鯛ナビにあった予約だね
						nr = cr;
						
						// 実行ON/OFF
						nr.setExec(ar.getExec());
						// 重複
						nr.setTunershort(ar.getTunershort());
						/*
						// 開始終了日時（そのうち追加したい）
						String nrdt = nr.getRec_nextdate()+" "+nr.getAhh()+":"+nr.getAmm();
						String ardt = ar.getRec_nextdate()+" "+ar.getAhh()+":"+ar.getAmm();
						if ( CommonUtils.getCompareDateTime(nrdt,ardt) != 0 || ! nr.getRec_min().equals(ar.getRec_min()) ) {
							nr.setAhh(ar.getAhh());
							nr.setAmm(ar.getAmm());
							nr.setZhh(ar.getZhh());
							nr.setZmm(ar.getZmm());
							nr.setRec_min(ar.getRec_min());
							getStartEndDateTime(nr);
							nr.setRec_nextdate(cr.getRec_nextdate());
						}
						*/
						
						break;
					}
				}
				
				if ( nr == ar ) {
					// 詳細とってくるわー
					reportProgress("DIGAで登録された予約の詳細情報を１件取り込んでいます。");
					if ( getDigaReserveDetail(nr, nr.getId()) == null ) {
						// 情報を取得できなかったので、鯛ナビの情報をそのままで
					}
				}
				newReserveList.add(nr);
			}
			else {
				// これは新規！
				newr = ar;
			}
		}
		
		return newr;
	}

	/*******************************************************************************
	 * DIGAから取得した繰り返し予約の値を鯛ナビの内部形式に変換する
	 ******************************************************************************/
	
	private static final int RPTN_SUN = 0x01;
	private static final int RPTN_MON = 0x02;
	private static final int RPTN_TUE = 0x04;
	private static final int RPTN_WED = 0x08;
	private static final int RPTN_THU = 0x10;
	private static final int RPTN_FRI = 0x20;
	private static final int RPTN_SAT = 0x40;
	private static final int RPTN_ALL = 0x80;
	private static final int RPTN_MON2THU = (RPTN_MON|RPTN_TUE|RPTN_WED|RPTN_THU);
	private static final int RPTN_MON2FRI = (RPTN_MON2THU|RPTN_FRI);
	private static final int RPTN_MON2SAT = (RPTN_MON2FRI|RPTN_SAT);
	private static final int RPTN_MON2SUN = (RPTN_MON2SAT|RPTN_SUN);
	private static final int RPTN_MON2THU_LN = (RPTN_TUE|RPTN_WED|RPTN_THU|RPTN_FRI);
	private static final int RPTN_MON2FRI_LN = (RPTN_MON2THU_LN|RPTN_SAT);
	private static final int RPTN_MON2SAT_LN = (RPTN_MON2FRI_LN|RPTN_SUN);
	
	private String _recpatternDG2RDstr(String source, String yy, String mm, String dd, String Ahh)
	{
		int rptn = 0;
		int rptcnt = 0;
		for ( int c=0; c < DIGA_WDPTNSTR.length; c++ ) {
			boolean checked = false;
			Matcher ma = Pattern.compile("name=\""+DIGA_WDPTNSTR[c]+"\" value=\"[^\"]+?\" checked").matcher(source);
			if ( checked = ma.find() ) {
				//
			}
			else {
				// BWT650
				ma = Pattern.compile("value=\"[^\"]+?\" name=\""+DIGA_WDPTNSTR[c]+"\" checked").matcher(source);
				if ( checked = ma.find() ) {
					//
				}
			}
			if ( checked ) {
				rptn |= (0x01<<c);
				rptcnt++;
			}
		}
		
		if ( rptn != 0 ) {
			// 24:00～28:59開始は深夜帯
			boolean isln = CommonUtils.isLateNight(Ahh);
			
			if ( rptn == RPTN_MON2SAT || (isln && rptn == RPTN_MON2SAT_LN) ) {
				return HDDRecorder.RPTPTN[9];	// "毎月～土";
			}
			else if ( rptn == RPTN_MON2FRI || (isln && rptn == RPTN_MON2FRI_LN) ) {
				return HDDRecorder.RPTPTN[8];	// "毎月～金";
			}
			else if ( rptn == RPTN_MON2THU || (isln && rptn == RPTN_MON2THU_LN) ) {
				return HDDRecorder.RPTPTN[7];	// "毎月～木";
			}
			else if ( rptcnt == 1 && rptn < RPTN_ALL ) {
				for ( int c=0; c<7; c++ ) {
					if ( (rptn & (0x01<<c)) != 0 ) {
						return HDDRecorder.RPTPTN[c];	// "毎＊曜日";
					}
				}
			}
			else if ( (rptn & RPTN_ALL) == RPTN_ALL || (rptn & RPTN_MON2SUN) == RPTN_MON2SUN ) {
				return HDDRecorder.RPTPTN[10];	// 毎日
			}
			
			// 未対応のパターンは単日扱いとなる
		}

		GregorianCalendar cal = new GregorianCalendar(
				Integer.valueOf(yy),
				Integer.valueOf(mm)-1,
				Integer.valueOf(dd));
		
		return CommonUtils.getDate(cal, true);
	}

	/*******************************************************************************
	 * DIGAにログインする
	 ******************************************************************************/
	
	private boolean doLogin() {
		
		reportProgress("DIGAにログインします("+getIPAddr()+":"+getPortNo()+").");

		boolean isLoggedin = false;
		for ( int cnt=1; (isLoggedin = _doLogin()) == false && cnt <= get_com_try_count(); cnt++ ) {
			if ( cnt < get_com_try_count() ) {
				reportProgress(String.format("+ログインを再試行します (%d回中%d回目)",(get_com_try_count()-1),cnt));
				CommonUtils.milSleep(WAIT_FOR_LOGIN);
			}
		}
		if ( ! isLoggedin ) {
			return false;
		}
		return true;
	}
	
	private boolean _doLogin() {
		
		// ホームページ
 		String response;
 		{
 			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/", null);
			response = d[1];
			
			if ( response == null ) {
				return false;
			}
		}
		Matcher ma = Pattern.compile("DMY_TIME=(\\d+?)'").matcher(response);
		if (ma.find()) {
			DMY_TIME = ma.group(1);
			//System.out.println("DMY_TIME="+DMY_TIME);
		}

		// ログインフレーム
		{
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/prevLogin.cgi?DMY_TIME="+DMY_TIME, null);
			response = d[1];
		}
		ma = Pattern.compile("nonce=\"(\\d+)\"").matcher(response);
		if (ma.find()) {
			NONCE = ma.group(1);
			//System.out.println("nonce="+NONCE);
			
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] bd = new String(NONCE+getPasswd()).getBytes();
				md.update(bd);
				DIGEST = b64.enc(md.digest());
				DIGEST = DIGEST.replaceAll("\\x2B", "%2B"); // replaces +
				DIGEST = DIGEST.replaceAll("\\x2F", "%2F"); // replaces /
				DIGEST = DIGEST.replaceAll("\\x3D", "%3D"); // replaces =
				//System.out.println("digest="+DIGEST);
			}
			catch (Exception e) {
				// 例外
				System.out.println("Exception: doLogin()");
			}
		}
		
		// ログイン実行
		{
			String[] d = reqPOST("http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/loginPsWd.cgi", "passwd=&nonce="+NONCE+"&digest="+DIGEST+"&cmd.x=42&cmd.y=10", null);
			response = d[1];
		}
		if (response.contains(DIGAMSG_LOGGEDIN)) {
			return(true);
		}

		return(false);
	}
	
	/*******************************************************************************
	 * HTTPアクセスをまとめてみた
	 ******************************************************************************/
	//
	private String[] reqDigaGET(String uri, Hashtable<String, String>property) {
		return reqDigaPOST(uri,null,property);
	}
	
	private String[] reqDigaPOST(String uri, String pstr, Hashtable<String, String>property) {
		String header = null;
		String response = null;
		for ( int cnt=1; cnt <= get_com_try_count(); cnt++ ) {

			String h = null;
			String r = null;
			if ( pstr == null ) {
				String[] d = reqGET(uri, property);
				h = d[0];
				r = d[1];
			}
			else {
				String[] d = reqPOST(uri, pstr, property);
				h = d[0];
				r = d[1];
			}

			if ( r == null || r.contains(DIGAMSG_PLEASELOGIN) ) {
				// ログイン失敗だからもうだめぽ
				break;
			}
			if ( ! r.contains(DIGAMSG_WAITFORLOGIN) && ! r.contains(DIGAMSG_BUSYNOW) ) {
				// いそがしいのでちょっとまって
				header = h;
				response = r;
				break;
			}

			if (getDebug()) {
				if (r.contains(DIGAMSG_WAITFORLOGIN)) System.out.println(DBGID+DIGAMSG_WAITFORLOGIN);
				else if (r.contains(DIGAMSG_BUSYNOW)) System.out.println(DBGID+DIGAMSG_BUSYNOW);
			}

			// リトライしてね
			if ( cnt < get_com_try_count() ) {
				reportProgress(String.format("+通信を再試行します  (%d回中%d回目)",(get_com_try_count()-1),cnt));
				CommonUtils.milSleep(WAIT_FOR_LOGIN);
			}
		}
		
		return new String[] { header, response };
	}
	
	
	// 予約情報同士を比較する
	private boolean isModified(ReserveList o, ReserveList n) {
		if (
				! o.getAhh().equals(n.getAhh()) ||
				! o.getAmm().equals(n.getAmm()) ||
				! o.getZhh().equals(n.getZhh()) ||
				! o.getZmm().equals(n.getZmm()) ||
				! o.getTitle().equals(n.getTitle()) ||
				false
				) {
			return true;
		}
		return false;
	}
}
