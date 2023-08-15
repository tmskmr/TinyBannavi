package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.AribCharMap;
import tainavi.ChannelCode;
import tainavi.ChapterInfo;
import tainavi.CommonUtils;
import tainavi.DeviceInfo;
import tainavi.GetRDStatus;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TVProgram.ProgGenre;
import tainavi.TextValueSet;
import tainavi.TitleInfo;
import tainavi.TraceProgram;

/**
 * REGZA DBR-T2007用のレコーダプラグインです。
 * @author original:tmskmr
 * @version x.xx.xx
 */
public class PlugIn_RecDBR_T2007 extends HDDRecorderUtils implements HDDRecorder,Cloneable {
	public PlugIn_RecDBR_T2007() {
		super();
		this.setTunerNum(3);
		setSettingFixed();
	}

	public PlugIn_RecDBR_T2007 clone() {
		return (PlugIn_RecDBR_T2007) super.clone();
	}

	private static final String thisEncoding = "UTF-8";

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/

	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA DBR-T2007"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }

	// chvalueを使っていいよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }

	// 録画タイトルに対応している
	@Override
	public boolean isTitleListSupported() { return true; }

	// 持ち出しに対応している
	@Override
	public boolean isPortableSupported() { return true; }

	// フォルダー作成に対応している
	@Override
	public boolean isFolderCreationSupported() { return true; }

	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/

	@Override
	public String getChDatHelp() { return
			"「レコーダの放送局名」はネットdeナビの録画予約一覧で表示される「CH」の欄の値を設定してください。\n"+
			"予約一覧取得が正常に完了していれば設定候補がコンボボックスで選択できるようになります。"
			;
	}

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	protected final String MSGID = "["+getRecorderId()+"] ";
	protected final String ERRID = "[ERROR]"+MSGID;
	protected final String DBGID = "[DEBUG]"+MSGID;

	protected final String NETWORK_UVD = "0";
	protected final String NETWORK_BSD = "1";
	protected final String NETWORK_CSD = "2";
	protected final String NETWORK_L1 = "3";
	protected final String NETWORK_L2 = "4";
	protected final String NETWORK_L3 = "5";
	protected final String NETWORK_L4 = "6";
	protected final String NETWORK_NET = "7";

	protected final String CHTYPE_UVD = "uvd";
	protected final String CHTYPE_BSD = "bsd";
	protected final String CHTYPE_CSD = "csd";
	protected final String CHTYPE_L1  = "L1";
	protected final String CHTYPE_L2  = "L2";
	protected final String CHTYPE_L3  = "L3";
	protected final String CHTYPE_L4  = "L4";
	protected final String CHTYPE_NET = "NET";

	protected final String CHPREFIX_BS = "BS";
	protected final String CHPREFIX_CS = "CS";
	protected final String CH_L1 = "L1";
	protected final String CH_L2 = "L2";
	protected final String CH_L3 = "L3";
	protected final String CH_L4 = "L4";
	protected final String CH_NET = "NET";

	protected final String OPTION_DATETIME = "1";
	protected final String OPTION_PROGRAM = "2";

	protected final String REPEAT_NONE = "0";

	protected final int RPTPTN_ID_TUE2SAT = 12;

	protected final String EXEC_YES = "1";
	protected final String EXEC_NO = "0";

	protected final String BRANCH_NO = "0";
	protected final String BRANCH_YES = "1";

	protected final String DEVICE_HDD = "0";
	protected final String DEVICE_DISC = "1";
	protected final String FOLDER_NONE = "0";
	protected final String FOLDER_NAME_NONE = "指定なし";

	protected final String RECMODE_DR = "0";
	protected final String RECMODE_NAME_DR = "[DR]";

	protected final String MOCHIDASHI_NONE = "0";

	protected final String RESULT_OK = "0";
	protected final String RESULT_OTHER = "1";
	protected final String RESULT_BUSY = "2";
	protected final String RESULT_INVALID_TITLE = "17";

	protected final String ERRMSG_NORESPONSE = "レコーダーが反応しません";
	protected final String ERRMSG_INVALIDRESPONSE = "レコーダーからの応答が不正です。";
	protected final String ERRMSG_USB_BUSY = "USB HDDが使用中です。";

	protected final double MIN_PER_MB = 171.777;

	protected final String TITLE_NOCHANGED = "__NETdeNAVI_TitleNameNotChanged__";

	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/

	public ChannelCode getChCode() {
		return cc;
	}

	private ChannelCode cc = new ChannelCode(getRecorderId());

	protected void setErrmsg(String s) { errmsg = s; }

	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}

	private String errmsg = "";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private GetRDStatus gs = new GetRDStatus();

	private String rsvedFile = "";
	private String folderTFile = "";
	private String titleFile = "";
	private String devinfoTFile = "";
	private ArrayList<TextValueSet> tvsBranch = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> tvsPatternCode = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> tvsPatternName = new ArrayList<TextValueSet>();

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/

	/*******************************************************************************
	 * レコーダーから各種設定情報を取得する
	 ******************************************************************************/
	@Override
	public boolean GetRdSettings(boolean force) {

		System.out.println("レコーダの各種設定情報を取得します.");

		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();

		String deviceTFile		= "env/device."+myFileId+".xml";
		devinfoTFile			= "env/devinfo."+myFileId+".xml";
		folderTFile				= "env/folders."+myFileId+".xml";
		String chValueTFile	= "env/chvalue."+myFileId+".xml";
		String chTypeTFile		= "env/chtype."+myFileId+".xml";
		String branchTFile		= "env/branch."+myFileId+".xml";

		// 固定の各種設定情報を初期化する
		setSettingFixed();

		File f = new File(deviceTFile);
		if ( !force){
			if (!f.exists())
				return(false);

			// キャッシュから読み出し（録画設定ほか）
			device = TVSload(deviceTFile);
			setDeviceInfos(DeviceInfosFromFile(devinfoTFile));
			folder = TVSload(folderTFile);
			chvalue = TVSload(chValueTFile);
			chtype = TVSload(chTypeTFile);
			tvsBranch = TVSload(branchTFile);

			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (device.size() > 0 && chvalue.size() > 0 && chtype.size() > 0 && tvsBranch.size() > 0) {
				return(true);
			}
		}

		// 各種設定情報をレコーダから取得する
		if (!setSettingVariable()){
			return (false);
		}

		TVSsave(device, deviceTFile);
		saveDeviceInfos();
		saveFolders();
		TVSsave(chvalue, chValueTFile);
		TVSsave(chtype, chTypeTFile);
		TVSsave(tvsBranch, branchTFile);

		return(true);
	}

	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/

	public boolean GetRdReserve(boolean force)
	{
		System.out.println("レコーダから予約一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");

		errmsg = "";

		//
		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();

		rsvedFile	= "env/reserved."+myFileId+".xml";

		File f = new File(rsvedFile);
		if ( !force && f.exists()) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));

			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (getReserves().size() > 0) {
				return(true);
			}
		}

		// 録画予約の一覧をレコーダから取得する
		ArrayList<ReserveList> ra = getReserveList();
		if (ra == null){
			return(false);
		}

		setReserves(ra);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		// 取得した情報の表示
		if (getDebug()){
			System.out.println("---Reserved List Start---");
			for ( int i = 0; i<getReserves().size(); i++ ) {
				// 詳細情報の取得
				ReserveList e = getReserves().get(i);
				System.out.println(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s(%s)\t%s\t%s",
						(i+1), e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(),	e.getZmm(),	e.getRec_min(), e.getRec_mode(), e.getTitle(), e.getTitlePop(), e.getChannel(), e.getCh_name()));
			}
			System.out.println("---Reserved List End---");
		}

		return(true);
	}

	/*******************************************************************************
	 * 予約詳細情報の取得
	 ******************************************************************************/
	@Override
	public boolean isThereAdditionalDetails() {
		return true;
	}

	/*
	 *  予約の詳細情報を取得する
	 */
	@Override
	public boolean GetRdReserveDetails(){
		int rno = 0;
		ArrayList<ReserveList> ra = getReserves();
		for (ReserveList entry : ra) {

			reportProgress("+番組詳細を取得します("+rno+"/"+ra.size()+").");
			getReserveDetail(entry);

			// 放送局名変換
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));

			// TS->DR
			translateAttributeTuner(entry);

			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributesT2007(entry, getReserves());

			rno++;
		}

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		return(true);
	}

	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	@Override
	public boolean PostRdEntry(ReserveList r)	{
		errmsg = "";

		String chcode = cc.getCH_WEB2CODE(r.getCh_name());
		if (chcode == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}

		//　RDから予約登録画面の初期情報を取り出す。これを呼ばないと ERR_EXCLUSIVEエラーになる
		if ( !loadDialogInitData("0")){
			return(false);
		}

		// RDへの情報作成
		String pstr = createPostData(r, "");

		// RDへ情報送信
		reportProgress(MSGID+"レコーダーに新規予約を要求します.");
		String [] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/DoReserve.php", pstr, null, thisEncoding);
//		String header = d[0];
		String response = d[1];

		// 登録結果の確認
		if ( !checkReserveResponse(r, response) ){
			return(false);
		}

		// 予約情報の調整
		adjustReserve(r);

		// 予約リストを更新
		getReserves().add(r);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		reportProgress(MSGID+"正常に登録できました。");

		return(true);
	}

	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	@Override
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		errmsg = "";

		String chcode = cc.getCH_WEB2CODE(r.getCh_name());
		if (chcode == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}

		String id = o.getId();

		//　RDから予約登録画面の初期情報を取り出す。これを呼ばないと ERR_EXCLUSIVEエラーになる
		if ( !loadDialogInitData(id) ){
			return(false);
		}

		// RDへの情報作成
		String pstr = createPostData(r, id);

		// RDへ情報送信
		reportProgress(MSGID+"レコーダーに予約更新を要求します.");
		String [] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/DoReserve.php", pstr, null, thisEncoding);
//		String header = d[0];
		String response = d[1];

		// 登録結果の確認
		if ( !checkReserveResponse(r, response) ){
			return(false);
		}

		// 予約情報の調整
		adjustReserve(r);

		// 情報置き換え
		getReserves().remove(o);
		getReserves().add(r);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		reportProgress(MSGID+"正常に更新できました。");

		return(true);
	}

	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	@Override
	public ReserveList RemoveRdEntry(String delid) {
		errmsg = "";

		// 削除対象を探す
		ReserveList r = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				r = reserve;
				break;
			}
		}
		if (r == null) {
			return(null);
		}

		//　RDから予約登録画面の初期情報を取り出す。これを呼ばないと ERR_EXCLUSIVEエラーになる
		if ( !loadDialogInitData(delid)){
			return(null);
		}

		// RDへの情報作成
		String pstr = createPostData(r, delid);

		// RDへ情報送信
		reportProgress(MSGID+"レコーダーに予約削除を要求します.");
		String [] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/DeleteReserve.php", pstr, null, thisEncoding);
//		String header = d[0];
		String response = d[1];

		if ( !checkGeneralResponse( "予約削除", response) ){
			return(null);
		}

		// 予約リストを更新
		getReserves().remove(r);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		reportProgress(MSGID+"正常に削除できました。");
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		return(r);
	}

	/*******************************************************************************
	 * フォルダー作成
	 ******************************************************************************/
	@Override
	public boolean CreateRdFolder(String device_id, String folder_name) {
		return setFolderName( device_id, null, folder_name );
	}

	/*******************************************************************************
	 * フォルダー名更新
	 ******************************************************************************/
	@Override
	public boolean UpdateRdFolderName(String device_id, String folder_id, String folder_name) {
		return setFolderName( device_id, folder_id, folder_name );
	}

	/*******************************************************************************
	 * フォルダー削除
	 ******************************************************************************/
	@Override
	public boolean RemoveRdFolder(String device_id, String fol_id) {
		errmsg = "";

		String action = "フォルダ削除";
		String folder_id = extractFolderID(fol_id);

		String pstr =
			"drive_id=" + device_id + "&" +
			"folder_id=" + folder_id;

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへ情報送信
			reportProgress(MSGID+"レコーダーに" + action + "を要求します.");
			String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/folderset/DeleteFolder.php?" + pstr, null, thisEncoding);
//			String header = d[0];
			String response = d[1];

			// レスポンスから処理結果を取得する
			String [] rc = getReasonFromResponse( response );
			if (rc == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(false);
			}

			String result = rc[0];
			String reason = rc[1];

			if (result.equals(RESULT_OK))
				break;

			if (result.equals(RESULT_OTHER) || result.equals(RESULT_BUSY)){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = ERRID+action+"に失敗しました。(result=" + result + ",reason=" + reason + ")";
			return(false);
		}

		// フォルダ一覧を取得し直す
		setSettingFolders();
		saveFolders();

		return true;
	}

	/*******************************************************************************
	 * タイトル一覧取得
	 ******************************************************************************/
	@Override
	public boolean GetRdTitles(String device_id, boolean force, boolean detail, boolean mountedOnly) {
		System.out.println("レコーダからタイトル一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");

		errmsg = "";

		DEVICE_ID = device_id;

		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();

		ArrayList<TitleInfo> list = new ArrayList<TitleInfo>();

		for (DeviceInfo di : getDeviceInfos() ) {
			String devid = di.getId();
			if (devid.equals(DEVICE_ALL))
				continue;
			if (!device_id.equals(DEVICE_ALL) && !device_id.equals(devid))
				continue;

			titleFile		= "env/title."+myFileId+"." + devid + ".xml";

			if (!force){
				File f = new File(titleFile);
				if (!f.exists())
					return(false);

				// キャッシュから読み出し（タイトル一覧）
				ArrayList<TitleInfo> ta = TitlesFromFile(titleFile);

				list.addAll(ta);
			}
			else{
				// タイトル一覧をレコーダから取得する
				ArrayList<TitleInfo> ta = getTitleList(devid, mountedOnly);
				if (ta == null){
					if (errmsg.length() > 0)
						return(false);

					File f = new File(titleFile);
					// キャッシュから読み出し（タイトル一覧）
					if (f.exists())
						ta = TitlesFromFile(titleFile);
				}
				else{
					if (ta.isEmpty()){
						reportProgress("０件だったので念のため再度取得します...");
						ta = getTitleList(devid, mountedOnly);
						if (ta == null)
							return false;
					}

					// タイトルの詳細情報を取得し、内容を調整する
					for (TitleInfo entry : ta) {
						// 放送局名変換
						entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
					}

					if (detail){
						getTitleDetails(devid, ta, false);
					}
				}

				list.addAll(ta);
			}
		}

		setTitles(list);

		// キャッシュに保存
		if (force){
			saveTitles(device_id);

			// 取得した情報の表示
			if (getDebug()){
				System.out.println("---Title List Start---");
				for ( int i = 0; i<getTitles().size(); i++ ) {
					// 詳細情報の取得
					TitleInfo t = getTitles().get(i);
					System.out.println(String.format("[%s] %s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s",
							(i+1), t.getId(), t.getRec_date(), t.getAhh(), t.getAmm(), t.getZhh(),	t.getZmm(),	t.getRec_min(),
							t.getTitle(), t.getChannel(), t.getCh_name()));
				}
				System.out.println("---Title List End---");
			}

			// 各種設定情報をレコーダから取得する
			if (setSettingDevice()){
				saveDeviceInfos();
			}
		}

		return(true);
	}

	/*******************************************************************************
	 * タイトル詳細情報取得
	 ******************************************************************************/
	@Override
	public boolean GetRdTitleDetails(String devid, boolean force){
		errmsg = "";

		ArrayList<TitleInfo> ta = getTitles();

		getTitleDetails(devid, ta, force);

		// キャッシュに保存
		saveTitles(devid);

		return(true);
	}

	/*
	 * タイトル詳細情報を取得する
	 */
	private boolean getTitleDetails(String devid, ArrayList<TitleInfo>ta, boolean force){
		int tno = 0;

		for (TitleInfo ti : ta){
			tno++;

			if (ti.getDetailLoaded() && !ti.getRecording() && !force)
				continue;

			reportProgress("+タイトル詳細を取得します("+tno+"/"+ta.size()+").");
			getTitleDetail(ti);
		}

		return(true);
	}

	/*******************************************************************************
	 * タイトル詳細情報取得
	 ******************************************************************************/
	@Override
	public boolean GetRdTitleDetail(TitleInfo t) {
		errmsg = "";

		if (t == null)
			return(false);

		if (!getTitleDetail(t))
			return(false);

		// キャッシュに保存
		saveTitles(t.getRec_device());

		return(true);
	}

	/*******************************************************************************
	 * タイトル更新
	 ******************************************************************************/
	@Override
	public boolean UpdateRdTitleInfo(String device_id, TitleInfo o, TitleInfo t) {
		errmsg = "";

		if (t == null) {
			return(false);
		}

		for (int n=0; n<3; n++){
			// タイトルの編集をレコーダに通知する
			notifyTitleEdit(device_id, t.getId());

			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへの情報作成
			String pstr = createTitlePostData(t, o, device_id);

			// RDへ情報送信
			reportProgress(MSGID+"レコーダーにタイトル更新を要求します:"+device_id);
			String [] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/UpdateTitleInfo.php", pstr, null, thisEncoding);
//			String header = d[0];
			String response = d[1];

			// レスポンスから処理結果を取得する
			String [] rc = getReasonFromResponse( response );
			if (rc == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(false);
			}

			String result = rc[0];
			String reason = rc[1];

			if (result.equals(RESULT_OK))
				break;

			if (result.equals(RESULT_OTHER)){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = ERRID+"タイトル更新に失敗しました。(result=" + result + ",reason=" + reason + ")";
			return(false);
		}

		// 録画タイトルリストを更新
		ArrayList<TitleInfo> list = getTitles();
		list.remove(o);
		list.add(t);
		list.sort(new TitleInfoComparator());

		// キャッシュに保存
		saveTitles(t.getRec_device());

		// 各種設定情報をレコーダから取得する
		if (setSettingDevice()){
			saveDeviceInfos();
		}

		reportProgress(MSGID+"正常に更新できました。");
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");

		return(true);
	}

	/*******************************************************************************
	 * タイトル削除
	 ******************************************************************************/
	@Override
	public TitleInfo RemoveRdTitle(String device_id, String title_id) {
		errmsg = "";

		// 削除対象を探す
		TitleInfo t = null;
		for (  TitleInfo ttl : getTitles() )  {
			if (ttl.getId().equals(title_id)) {
				t = ttl;
				break;
			}
		}
		if (t == null) {
			return(null);
		}

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへの情報作成
			String pstr = "drive_id=" + device_id + "&title_id=" + title_id;

			// RDへ情報送信
			reportProgress(MSGID+"レコーダーにタイトル削除を要求します.");
			String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/DeleteTitle.php?"+pstr, null, thisEncoding);
//			String header = d[0];
			String response = d[1];

			// レスポンスから処理結果を取得する
			String [] rc = getReasonFromResponse( response );
			if (rc == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(null);
			}

			String result = rc[0];
			String reason = rc[1];

			if (result.equals(RESULT_OK))
				break;

			if (result.equals(RESULT_OTHER) || result.equals(RESULT_INVALID_TITLE)){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = ERRID+"タイトル削除に失敗しました。(result=" + result + ",reason=" + reason + ")";
			return(null);
		}

		// タイトルリストを更新
		getTitles().remove(t);

		// キャッシュに保存
		saveTitles(t.getRec_device());

		setSettingDevice();
		saveDeviceInfos();

		reportProgress(MSGID+"正常に削除できました。");
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		return(t);
	}

	/*******************************************************************************
	 * タイトル再生の開始・終了
	 ******************************************************************************/
	@Override
	public boolean StartStopPlayRdTitle(String device_id, String title_id, boolean start) {
		errmsg = "";

		// RDへのURL
		String action = start ? "タイトル再生開始" : "タイトル再生終了";
		String url = start ? "/titlelist/PlayTitle.php" : "/titlelist/PlayStop.php";

		// RDへの情報作成
		String pstr = "drive_id=" + device_id + "&title_id=" + title_id;

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへ情報送信
			reportProgress(MSGID+"レコーダーに" + action + "を要求します.");
			String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+url+ "?"+pstr, null, thisEncoding);
//			String header = d[0];
			String response = d[1];

			// レスポンスから処理結果を取得する
			String [] rc = getReasonFromResponse( response );
			if (rc == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(false);
			}

			String result = rc[0];
			String reason = rc[1];

			if (result.equals(RESULT_OK))
				break;

			if (result.equals(RESULT_OTHER)){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = ERRID+action+"に失敗しました。(result=" + result + ",reason=" + reason + ")";
			return(false);
		}

		reportProgress(MSGID+"正常に" + action + "できました。");

		return(true);
	}

	/* ここまで */

	/* 個別コード－ここから最後まで */
	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/
	/*
	 *  録画予約の一覧を取得する
	 */
	protected 	ArrayList<ReserveList> getReserveList() {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		//　RDから予約一覧を取り出す
		reportProgress(MSGID+"予約一覧を取得します.");
		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/LoadReserveList.php",null, thisEncoding);
//		String header = d[0];
		String response= d[1];

		if (response == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return(null);
		}

		// 先頭部分をチェックする
		Matcher mr = Pattern.compile("\\{\"NETdeNAVI\"").matcher(response);
		if ( ! mr.find()) {
			errmsg = ERRID+ERRMSG_INVALIDRESPONSE;
			return (null);
		}

		ArrayList<ReserveList> list = new ArrayList<ReserveList>();

		Matcher ma = Pattern.compile("\\{" +
				"\"num\":(\\d+)," +			// 1
				"\"id\":(\\d+)," +			// 2
				"\"exec\":(\\d+)," +		// 3
				"\"option\":(\\d+)," +		// 4
				"\"eventname\":\"([^\"]+)\"," +	// 5
				"\"network\":(\\d+)," +		// 6
				"\"ch\":\"([^\"]+)\"," +	// 7
				"\"repeat\":(\\d+)," +		// 8
				"\"datetime\":(\\d+)," +	// 9
				"\"duration\":(\\d+)," +	// 10
				"\"conflictstart\":(\\d+)," +	// 11
				"\"conflictend\":(\\d+)," +	// 12
				"\"recording\":(\\d+)\\}")	// 13
				.matcher(response);

		while ( ma.find() ) {
			// 個々のデータを取り出す
			ReserveList entry = new ReserveList();

			String id = ma.group(2);
			String exec = ma.group(3);
			String option = ma.group(4);
			String eventname = ma.group(5);
			String network = ma.group(6);
			String ch = ma.group(7);
			String repeat = ma.group(8);
			String datetime = ma.group(9);
			String duration = ma.group(10);

			// 予約ID
			entry.setId(id);

			// 基本情報をセットする
			setReserveBasicInfo(entry, exec, option, eventname, network, ch, repeat, datetime, duration);

			// 予約情報を保存
			list.add(entry.clone());
		}

		return(list);
	}

	/***
	 *  予約の基本情報をセットする
	 */
	protected void setReserveBasicInfo(ReserveList entry, String exec, String option, String eventname,
			String network, String ch, String repeat, String datetime, String duration){
		// 実行ON/OFF
		entry.setExec(exec.equals(EXEC_YES));

		// オプション(1=日付指定, 2=PGM指定）
		entry.setRec_option(option);

		// 開始日、終了日
		int nbsecs = Integer.parseInt(datetime);		// 開始日時(UNIX時間/1000)
		int secs = Integer.parseInt(duration);			// 録画時間(sec)
		int nesecs = nbsecs + secs;						// 終了日時(UNIX時間/1000)
		Date bdate = new Date(nbsecs*1000L);			// 開始日時(Date)
		Date edate = new Date(nesecs*1000L);			// 終了日時(Date)

		SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd(E)", Locale.JAPAN);
		String pattern = sfd.format(bdate);

		// 繰り返しパターン
		String pid = String.valueOf(RPTPTN_ID_BYDATE);

		if (!repeat.equals(REPEAT_NONE)){
			pattern = value2text(tvsPatternName, repeat);
			pid = value2text(tvsPatternCode, repeat);
		}

		entry.setRec_pattern_id(Integer.parseInt(pid));
		entry.setRec_pattern(pattern);

		// 開始、終了時刻
		SimpleDateFormat sfh = new SimpleDateFormat("HH");
		SimpleDateFormat sfm = new SimpleDateFormat("mm");
		String ahh = sfh.format(bdate);
		String amm = sfm.format(bdate);
		String zhh = sfh.format(edate);
		String zmm = sfm.format(edate);
		entry.setAhh(ahh);
		entry.setAmm(amm);
		entry.setZhh(zhh);
		entry.setZmm(zmm);

		// 次の録画日などを計算する
		entry.setRec_nextdate(CommonUtils.getNextDate(entry));
		entry.setRec_min(String.valueOf(secs/60));
		getStartEndDateTime(entry);

		// チューナーは固定
		entry.setTuner("R1");

		// 録画モードもとりあえず固定（後で詳細情報で上書きする）
		entry.setRec_mode(RECMODE_NAME_DR);

		// タイトル
		String title = unescapeJavaString(eventname);
		entry.setTitle(title);
		entry.setTitlePop(TraceProgram.replacePop(title));

		// チャンネル
		switch(network){
		case NETWORK_UVD:
			break;
		case NETWORK_BSD:
			ch = CHPREFIX_BS + ch;
			break;
		case NETWORK_CSD:
			ch = CHPREFIX_CS + ch;
			break;
		case NETWORK_L1:
			ch = CH_L1;
			break;
		case NETWORK_L2:
			ch = CH_L2;
			break;
		case NETWORK_L3:
			ch = CH_L3;
			break;
		case NETWORK_L4:
			ch = CH_L4;
			break;
		case NETWORK_NET:
			ch = CH_NET;
			break;
		}

		entry.setChannel(ch);

		// 放送局名変換
		entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
	}

	/*
	 * 予約詳細情報を取得する
	 */
	protected boolean getReserveDetail( ReserveList r) {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String id = r.getId();

		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/LoadReserveDetailData.php?reserve_id=" + id, null, thisEncoding);
//		String header = d[0];
		String response= d[1];

		if (response == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			System.out.println(errmsg);
			return(false);
		}

		// {"NETdeNAVI":{"reserveInfo":{"id":103,"exec":1,"option":2,"eventname":"Ｒｅ：ＣＲＥＡＴＯＲＳ（レクリエイターズ）",
		// "network":0,"chnum":91,"branchexists":false,"branchnum":0,"ch":"091","repeat":64,"datetime":1495290600,"duration":1800,
		// "conflictstart":0,"conflictend":0,"drive_id":512,"folder_id":33,"recmode":0,"mochidashi":0,"video_es":0,"audio_es":0,
		// "data_es":0,"recording":0}}}
		Matcher ma = Pattern.compile("\\{" +
				"\"id\":(\\d+)," +				// 1
				"\"exec\":(\\d+)," +			// 2
				"\"option\":(\\d+)," +			// 3
				"\"eventname\":\"([^\"]+)\"," +	// 4
				"\"network\":(\\d+)," +			// 5
				"\"chnum\":(\\d+)," +			// 6
				"\"branchexists\":(false|true)," +	// 7
				"\"branchnum\":(\\d+)," +	// 8
				"\"ch\":\"([^\"]+)\"," +	// 9
				"\"repeat\":(\\d+)," +		// 10
				"\"datetime\":(\\d+)," +	// 11
				"\"duration\":(\\d+)," +	// 12
				"\"conflictstart\":(\\d+)," +	// 13
				"\"conflictend\":(\\d+)," +	// 14
				"\"drive_id\":(\\d+)," +	// 15
				"\"folder_id\":(\\d+)," +	// 16
				"\"recmode\":(\\d+)," +		// 17
				"\"mochidashi\":(\\d+)," +	// 18
				"\"video_es\":(\\d+)," +	// 19
				"\"audio_es\":(\\d+)," +	// 20
				"\"data_es\":(\\d+)," +		// 21
				"\"recording\":(\\d+)\\}")	// 22
				.matcher(response);

		if ( !ma.find() ) {
			reportProgress(ERRID+"予約詳細情報が取得できません.ID=" + id);
			return (false);
		}

		String exec = ma.group(2);
		String option = ma.group(3);
		String eventname = ma.group(4);
		String network = ma.group(5);
		// chnum
		// branchexists
		// branchnum
		String ch = ma.group(9);
		String repeat = ma.group(10);
		String datetime = ma.group(11);
		String duration = ma.group(12);
		// conflictstart
		// conflictend
		String device_id = ma.group(15);
		String folder_id = ma.group(16);
		String recmode = ma.group(17);
		String mochidashi = ma.group(18);
		// video_es
		// audio_es
		// data_es
		// recording

		// 基本情報をセットする
		String tuner = r.getTuner();
		setReserveBasicInfo(r, exec, option, eventname, network, ch, repeat, datetime, duration);
		// チューナはそのまま
		r.setTuner(tuner);

		// 保存先ドライブ
		String device_name = value2text(device, device_id);
		r.setRec_device(device_name);

		// 保存先フォルダー
		String folder_name = value2text(folder, device_id + ":" + folder_id);
		r.setRec_folder(folder_name);

		// 録画モード
		String recmode_name = value2text(vrate, recmode);
		r.setRec_mode(recmode_name);

		// 持ち出し
		String portable_name = value2text(portable, mochidashi);
		r.setRec_portable(portable_name);

		return(true);
	}

	/**
	 * レコーダーから取得できない情報は直接コピー（既存のリストから探して）
	 */
	protected void copyAttributesT2007(ReserveList to, ArrayList<ReserveList> fromlist) {
		ReserveList olde = null;
		for ( ReserveList from : fromlist ) {
			if ( from.getId() != null && from.getId().equals(to.getId()) ) {
				copyAttributeT2007(to, olde = from);
				break;
			}
		}

		// DIGAの終了時間"未定"対応だけど、別にDIGAかどうか確認したりはしない。
		setAttributesDiga(to,olde);
	}

	/**
	 * レコーダーから取得できない情報は直接コピー（既存エントリから直に）
	 */
	protected void copyAttributeT2007(ReserveList to, ReserveList from) {
		// 鯛ナビの内部フラグ
		to.setAutocomplete(from.getAutocomplete());

		// 予約一覧からは取得できない情報
		to.setDetail(from.getDetail());
		to.setRec_genre(from.getRec_genre());
		to.setRec_autodel(from.getRec_autodel());

		// BZ700以降の取得一覧から取得できない画質の対応
		if (to.getRec_mode().equals("")) {
			to.setRec_mode(from.getRec_mode());
		}
	}

	//
	protected void translateAttributeTuner(ReserveList entry) {
		if (entry.getTuner().startsWith("TS")) {
			entry.setTuner(entry.getTuner().replaceFirst("^TS", "DR"));
		}
	}

	/*
	 * 登録/削除要求のPOSTデータを生成する
	 */
	protected String createPostData(ReserveList r, String idBefore) {

		String id = idBefore;
		String exec = EXEC_YES;
		String option = OPTION_PROGRAM;
		String eventname = "";
		String network = NETWORK_UVD;
		String chnum = "0";
		String branchexists = BRANCH_NO;
		String branchnum = "0";
		String ch = "";
		String repeat = REPEAT_NONE;
		String datetime = "";
		String duration = "";
		String conflictstart = "0";
		String conflictend = "0";
		String drive_id = DEVICE_HDD;
		String folder_id = FOLDER_NONE;
		String recmode = RECMODE_DR;
		String mochidashi = MOCHIDASHI_NONE;
		String video_es = "0";
		String audio_es = "0";
		String data_es = "0";
		String recording = "0";

		boolean bUpdate = !idBefore.equals("");

		// スキップ
		if (!r.getExec())
			exec = EXEC_NO;

		// オプション
		if (bUpdate)
			option = r.getRec_option();

		// タイトル(eventname)
		try {
			if (!r.getAutocomplete())
				eventname = URLEncoder.encode(CommonUtils.substringrb(r.getTitle(),80), thisEncoding);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// チャンネル(ch)
		String channel = cc.getCH_WEB2CODE(r.getCh_name());
		ch = cc.getCH_CODE2REC(channel);

		// 枝番有(branchexists,branchnum)
		branchnum = text2value(tvsBranch, channel);
		try{
			branchexists = Integer.parseInt(branchnum) > 0 ? BRANCH_YES : BRANCH_NO;
		}
		catch(NumberFormatException e){
			branchnum = "0";
		}

		// ネットワーク(network)
		String typ = text2value(chtype, channel);
		switch(typ){
		case CHTYPE_UVD:
			network = NETWORK_UVD;		// 地上デジタル
			break;
		case CHTYPE_BSD:
			network = NETWORK_BSD;		// BSデジタル
			ch = ch.substring(CHPREFIX_BS.length());
			break;
		case CHTYPE_CSD:
			network = NETWORK_CSD;		// 110度CSデジタル
			ch = ch.substring(CHPREFIX_CS.length());
			break;
		case CHTYPE_L1:
			network = NETWORK_L1;		// 外部入力(L1)
			break;
		case CHTYPE_L2:
			network = NETWORK_L2;		// 外部入力(L2)
			break;
		case CHTYPE_L3:
			network = NETWORK_L3;		// 外部入力(L3)
			break;
		case CHTYPE_L4:
			network = NETWORK_L4;		// 外部入力(L4)
			break;
		case CHTYPE_NET:
			network = NETWORK_NET;		// 外部入力(NET)
			break;
		default:
			// 普通ここには落ちない
			if (ch.startsWith("C")) {
				network = NETWORK_L1;	// "C***"は外部入力(L1)
			}
			else if (ch.startsWith("SP")) {
				network = NETWORK_L3; 	// "SP***"は外部入力(L3)
			}
			else {
				network = NETWORK_L2;	// 未定義は全部外部入力(L2)
			}
			break;
		}

		chnum = ch;

		// 繰り返しパターン(repeat)
		String pattern = r.getRec_pattern();
		repeat = text2value(tvsPatternName, pattern);
		if (repeat.equals(""))
			repeat = REPEAT_NONE;	// 繰り返しなし

		// 開始日時(datetime)
		if (!repeat.equals(REPEAT_NONE)){
			String pid = value2text(tvsPatternCode, repeat);
			r.setRec_pattern_id(Integer.parseInt(pid));

			pattern = CommonUtils.getNextDate(r);
		}

		Matcher ma = Pattern.compile("^(\\d+)/(\\d+)/(\\d+)").matcher(pattern);
		if (ma.find()){
			String startDateTime = String.format("%04d%02d%02d %02d:%02d:00",
					Integer.parseInt(ma.group(1)),
					Integer.parseInt(ma.group(2)),
					Integer.parseInt(ma.group(3)),
					Integer.parseInt(r.getAhh()),
					Integer.parseInt(r.getAmm()));

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

	        // Date型に変換する
			Date date = new Date();
			try {
				date = sdf.parse(startDateTime);
			} catch (ParseException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

			// UNIX時刻/1000を取得する（単位秒）
			datetime = String.valueOf(date.getTime()/1000);
		}

		// 録画時間(duration)（単位秒）
		String min = CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm());
		duration = String.valueOf(Integer.parseInt(min)*60);

		// ドライブ(drive_id)/フォルダ(folder_id)
		drive_id = text2value(device, r.getRec_device());
		String s = text2value(folder, r.getRec_folder());

		// <drive_id>:<folder_id> から<folder_id>を取り出す。ただし「指定なし」は"0"
		int idx = s.indexOf(':');
		if (idx != -1)
			folder_id = s.substring(idx+1);

		// 録画モード(recmode)
		recmode = text2value(vrate, r.getRec_mode());

		// 持ち出し
		mochidashi = text2value(portable, r.getRec_portable());

		String postData =
			"reserveInfo[id]=" + id + "&" +
			"reserveInfo[exec]=" + exec + "&" +
			"reserveInfo[option]=" + option + "&" +
			"reserveInfo[eventname]=" + eventname + "&" +
			"reserveInfo[network]=" + network + "&" +
			"reserveInfo[chnum]=" + chnum + "&" +
			"reserveInfo[branchexists]=" + branchexists + "&" +
			"reserveInfo[branchnum]=" + branchnum + "&" +
			"reserveInfo[ch]=" + ch + "&" +
			"reserveInfo[repeat]=" + repeat + "&" +
			"reserveInfo[datetime]=" + datetime + "&" +
			"reserveInfo[duration]=" + duration + "&" +
			"reserveInfo[conflictstart]=" + conflictstart + "&" +
			"reserveInfo[conflictend]=" + conflictend + "&" +
			"reserveInfo[drive_id]=" + drive_id + "&" +
			"reserveInfo[folder_id]=" + folder_id + "&" +
			"reserveInfo[recmode]=" + recmode + "&" +
			"reserveInfo[mochidashi]=" + mochidashi + "&" +
			"reserveInfo[video_es]=" + video_es + "&" +
			"reserveInfo[audio_es]=" + audio_es + "&" +
			"reserveInfo[data_es]=" + data_es + "&" +
			"reserveInfo[recording]=" + recording;

		if (getDebug())
			System.out.println("PostData=[" + postData + "]");

		return postData;
	}

	/*
	 * 予約登録画面の初期化情報を取得する。これを呼ばないと、後で登録要求した時にERR_EXCLUSIVEエラーになる
	 */
	protected boolean loadDialogInitData( String id) {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		reportProgress(MSGID+"予約登録画面を初期化します.");
		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/LoadDialogInitializationData.php?schId=0", null, thisEncoding);
//		String header = d[0];
		String response= d[1];

		if (response == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			System.out.println(errmsg);
			return(false);
		}

		return(true);
	}

	/*
	 * 登録要求の応答をチェックする
	 */
	protected boolean checkReserveResponse( ReserveList r, String response ) {
		if (response == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return(false);
		}

		// {"result":24,"reason":"ERR_EXCLUSIVE","schid":"0"}
		Matcher ma = Pattern.compile("\\{" +
			"\"result\":(\\d+)," +
			"\"reason\":\"([^\"]*)\"," +
			"\"schid\":\"(\\d+)\"\\}").matcher(response);

		if ( ma.find() ) {
			String result = ma.group(1);
			String reason = ma.group(2);
			String schid = ma.group(3);

			switch(result){
			case RESULT_OK:
				break;
			case "3":
				reportProgress("3:番組が見つからなかったため、日時指定予約として登録しました。");
				break;
			case "4":
				reportProgress("4:予約が重複しています。");
				break;
			case "5":
				reportProgress("5:持ち出し変換の上限を超えているため、持ち出し設定を「なし」として登録しました。");
				break;
			case "6":
				reportProgress("6:持ち出し変換の上限を超えているため、持ち出し設定を「なし」として登録しました。");
				break;
			case "7":
				reportProgress("7:番組が見つからなかったため、日時指定予約として登録しました。");
				break;
			case "8":
				reportProgress("8:番組が見つからなかったため、日時指定予約として登録しました。\n" +
					"持ち出し変換の上限を超えているため、持ち出し設定を「なし」として登録しました。");
				break;
			case "9":
				reportProgress("9:番組が見つからなかったため、日時指定予約として登録しました。\n" +
					"持ち出し変換の上限を超えているため、持ち出し設定を「なし」として登録しました。");
				break;
			default:
				errmsg = ERRID+"予約登録に失敗しました。(result=" + result + ",reason=" + reason + ")";
				reportProgress(errmsg);
				return(false);
			}

			if (! schid.equals("0")){
				r.setId(schid);
				reportProgress(MSGID+"予約IDは"+r.getId()+"です。");
			}

			if (!result.equals(RESULT_OK))
				getReserveDetail(r);
		}
		else{
			errmsg = ERRID+ERRMSG_INVALIDRESPONSE;
			reportProgress(errmsg);
			return(false);
		}

		return(true);
	}

	/*
	 * 予約内容を調整する
	 */
	protected void adjustReserve(ReserveList r) {
		// 予約パターンID
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));

		// 次回予定日
		r.setRec_nextdate(CommonUtils.getNextDate(r));

		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));

		// 開始日時・終了日時
		getStartEndDateTime(r);
	}

	/*
	 * フォルダの作成ないしフォルダ名称を変更する
	 */
	protected boolean setFolderName(String device_id, String fol_id, String folder_name) {
		String action = fol_id != null ? "フォルダ名更新" : "フォルダ作成";
		String folder_id = extractFolderID(fol_id);

		String fnameEnc = "";
		try {
			fnameEnc = URLEncoder.encode(CommonUtils.substringrb(folder_name,80), thisEncoding);
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		String pstr =
				"drive_id=" + device_id + "&" +
				"folder_id=" + (folder_id != null ? folder_id : "NEWFOLDER") + "&" +
				"folder_name=" + fnameEnc;

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへ情報送信
			reportProgress(MSGID+"レコーダーに" + action + "を要求します.");
			String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/folderset/SetFolderName.php?" + pstr, null, thisEncoding);
//			String header = d[0];
			String response = d[1];

			// レスポンスから処理結果を取得する
			String [] rc = getReasonFromResponse( response );
			if (rc == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(false);
			}

			String result = rc[0];
			String reason = rc[1];

			if (result.equals(RESULT_OK))
				break;

			if (result.equals(RESULT_OTHER) || result.equals(RESULT_BUSY)){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = ERRID+action+"に失敗しました。(result=" + result + ",reason=" + reason + ")";
			return(false);
		}

		// フォルダ一覧を取得し直す
		setSettingFolders();
		saveFolders();

		// タイトルに含まれるフォルダ名を更新する
		updateFolderNameOfTitles();

		return (true);
	}

	/*
	 * タイトルに含まれるフォルダ名を更新する
	 */
	protected void updateFolderNameOfTitles(){
		ArrayList<TitleInfo> list = getTitles();

		for (TitleInfo ti : list){
			ArrayList<TextValueSet> ts = ti.getRec_folder();

			for (TextValueSet t : ts){
				t.setText(value2text(folder, t.getValue()));
			}

			ti.setRec_folder(ts);
		}

		setTitles(list);

		saveTitles(DEVICE_ID);
	}
	/*
	 * USB-HDDをマウントする
	 */
	protected boolean mountUsbDrive(String device_id) {
		reportProgress(MSGID+"ドライブをマウントします:"+device_id);

		String pstr = "drive_id=" + device_id;

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		// RDへ情報送信
		String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/ChangeMountedUSB.php?" + pstr, null, thisEncoding);
//		String header = d[0];
		String response = d[1];

		// 結果の確認
		if ( !checkGeneralResponse( "USB HDDのマウント", response) ){
			return(false);
		}

		return true;
	}

	/*
	 * 一般的な応答をチェックする
	 */
	protected boolean checkGeneralResponse( String action, String response){
		String [] rc = getReasonFromResponse( response );

		if (rc == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return(false);
		}

		String result = rc[0];
		String reason = rc[1];

		if (! result.equals(RESULT_OK)){
			errmsg = action + "に失敗しました。(result=" + result + ",reason=" + reason + ")";
			reportProgress(errmsg);
			return(false);
		}

		return(true);
	}

	/*
	 * 応答メッセージから結果を取得する
	 */
	protected String[] getReasonFromResponse( String response){
		if (response == null) {
			return(null);
		}

		// 先頭部分をチェックする
		Matcher mh = Pattern.compile("\\{\"NETdeNAVI\"").matcher(response);
		if ( ! mh.find()) {
			return (null);
		}

		// 応答メッセージをパースする
		// {"result":24,"reason":"ERR_EXCLUSIVE"}
		Matcher ma = Pattern.compile("\\{" +
			"\"result\":(\\d+)," +
			"\"reason\":\"([^\"]*)\"\\}").matcher(response);
		if ( !ma.find() )
			return(null);

		String [] rc = new String[2];

		rc[0] = ma.group(1);
		rc[1] = ma.group(2);

		return(rc);
	}

	/*
	 * TitleInfoを startDateTime, cotent_id順にソートする
	 *
	 */
	public class TitleInfoComparator implements Comparator<TitleInfo>{

		@Override
		public int compare(TitleInfo p1, TitleInfo p2) {
			int rc = p1.getStartDateTime().compareTo(p2.getStartDateTime());
			if (rc != 0){
				return rc;
			}

			return p1.getSerial() - p2.getSerial();
		}
	}
	/*
	 *  タイトル一覧を取得する
	 */
	protected 	ArrayList<TitleInfo> getTitleList(String device_id, boolean mountedOnly) {
		String str = "drive=" + device_id + "&org_pl=0";

		String response = "";

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			//　RDからタイトル一覧を取り出す
			reportProgress(MSGID+"タイトル一覧を取得します:"+device_id);
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/LoadTitleList.php?" + str, null, thisEncoding);
//			String header = d[0];
			response= d[1];

			if (response == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return(null);
			}

			// 先頭部分をチェックする
			Matcher mh = Pattern.compile("\\{\"NETdeNAVI\"").matcher(response);
			if ( ! mh.find()) {
				errmsg = ERRID+ERRMSG_INVALIDRESPONSE;
				return(null);
			}

			// 応答メッセージをパースする
			// {"result":24,"reason":"ERR_EXCLUSIVE"}
			Matcher mr = Pattern.compile("\\{" +
				"\"result\":(\\d+)," +
				"\"reason\":\"([^\"]*)\"\\}").matcher(response);

			if ( !mr.find() )
				break;

			String result = mr.group(1);
			String reason = mr.group(2);

			if (result.equals("1")){
				if (mountedOnly){
					reportProgress(MSGID+"ドライブがマウントされていないので取得を中止します:"+device_id);
					return(null);
				}

				if (mountUsbDrive(device_id))
					continue;

				return(null);
			}

			errmsg = "録画タイトルの取得に失敗しました。(result=" + result + ",reason=" + reason + ")";
			reportProgress(errmsg);
			return(null);
		}

		ArrayList<TitleInfo> list = new ArrayList<TitleInfo>();

		// {"num":4,"id":"51a6a4900016","titlename":"[映][SS]スティーヴン・キング　骨の袋［前編］",
		// "folders":[{"folder_id":9}],"genres":[{"genrecode":15}],"ch":"BS201",
		// "datetime":1369843200,"duration":4858,"newflag":true,"autorecflag":false},
		Matcher ma = Pattern.compile("\\{" +
				"\"num\":(\\d+)," +					// 1
				"\"id\":\"([^\"]+)\"," +			// 2
				"\"titlename\":\"([^\"]+)\"," +		// 3
				"\"folders\":\\[([^\\]]*)\\]," +	// 4
				"\"genres\":\\[([^\\]]*)\\]," +		// 5
				"\"ch\":\"([^\"]+)\"," +			// 6
				"\"datetime\":(\\d+)," +			// 7
				"\"duration\":(\\d+)," +			// 8
				"\"newflag\":(false|true)," +		// 9
				"\"autorecflag\":(false|true)\\}")	// 10
				.matcher(response);

		int serial = 0;

		while ( ma.find() ) {
			// 個々のデータを取り出す
			TitleInfo entry = new TitleInfo();

			String id = ma.group(2);
			String titlename = unescapeJavaString(ma.group(3));
			String folders = ma.group(4);
			String genres = ma.group(5);
			String ch = ma.group(6);
			String datetime = ma.group(7);
			String duration = ma.group(8);
//			String newflag = ma.group(9);
//			String autorecflag = ma.group(10);

			// タイトルID
			entry.setId(id);

			// すでに同じIDのタイトルがある場合はその情報を引き継ぐ
			TitleInfo tiOld = getTitleInfo(id);
			if (tiOld != null)
				entry = tiOld.clone();

			// 基本情報をセットする
			entry.setSerial(++serial);
			setTitleBasicInfo(entry, titlename, ch, datetime, duration, device_id, folders, genres);

			// 予約情報を保存
			list.add(entry.clone());
		}

		list.sort(new TitleInfoComparator());

		return(list);
	}

	/***
	 *  タイトルの基本情報をセットする
	 */
	protected void setTitleBasicInfo(TitleInfo entry, String titlename,
			String ch, String datetime, String duration, String device_id, String folders, String genres){

		// 開始日、終了日
		int nbsecs = Integer.parseInt(datetime);		// 開始日時(UNIX時間/1000)
		int secs = Integer.parseInt(duration);			// 録画時間(sec)
		int nesecs = nbsecs + secs;						// 終了日時(UNIX時間/1000)
		Date bdate = new Date(nbsecs*1000L);			// 開始日時(Date)
		Date edate = new Date(nesecs*1000L);			// 終了日時(Date)

		SimpleDateFormat sfdm = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN);
		entry.setStartDateTime(sfdm.format(bdate));
		entry.setEndDateTime(sfdm.format(edate));

		// 開始、終了時刻
		SimpleDateFormat sfh = new SimpleDateFormat("HH");
		SimpleDateFormat sfm = new SimpleDateFormat("mm");
		String ahh = sfh.format(bdate);
		String amm = sfm.format(bdate);
		String zhh = sfh.format(edate);
		String zmm = sfm.format(edate);
		entry.setAhh(ahh);
		entry.setAmm(amm);
		entry.setZhh(zhh);
		entry.setZmm(zmm);


		// 次の録画日などを計算する
		SimpleDateFormat sfd = new SimpleDateFormat("yyyy/MM/dd(E)", Locale.JAPAN);
		entry.setRec_date(sfd.format(bdate));
		entry.setRec_min(String.valueOf(secs/60));

		// タイトル
		entry.setTitle(titlename);

		// チャンネル
		entry.setChannel(ch);

		// デバイス
		String device_name = value2text(device, device_id);
		entry.setRec_device(device_name);

		// フォルダー
		entry.setRec_folder(parseFolders(device_id, folders));

		// ジャンル
		entry.setRec_genre(parseGenres(genres));
	}

	/*
	 * タイトル詳細情報を取得する
	 */
	protected boolean getTitleDetail( TitleInfo t) {

		String id = t.getId();
		String device_name = t.getRec_device();
		String device_id = text2value(device, device_name);
		String title_id = t.getId();
		String str = "drive_id=" + device_id + "&title_id=" + title_id;
		String response = "";

		for (int n=0; n<3; n++){
			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/LoadTitleDetailData.php?" + str, null, thisEncoding);
//			String header = d[0];
			response= d[1];

			if (response == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				System.out.println(errmsg);
				return(false);
			}

			// 先頭部分をチェックする
			Matcher mh = Pattern.compile("\\{\"NETdeNAVI\"").matcher(response);
			if ( ! mh.find()) {
				errmsg = ERRID+ERRMSG_INVALIDRESPONSE;
				return (false);
			}

			// 応答メッセージをパースする
			// {"result":24,"reason":"ERR_EXCLUSIVE"}
			Matcher mr = Pattern.compile("\\{" +
				"\"result\":(\\d+)," +
				"\"reason\":\"([^\"]*)\"\\}").matcher(response);

			if ( !mr.find() )
				break;

			String result = mr.group(1);
			String reason = mr.group(2);

			if (result.equals("1")){
				if (mountUsbDrive(device_id))
					continue;
			}

			errmsg = "タイトル詳細情報の取得に失敗しました。(result=" + result + ",reason=" + reason + ")";
			reportProgress(errmsg);
			return(false);
		}

		// {"NETdeNAVI":{"titleDetailInfo":{"id":"589285bc000b","unique_id":"589285bc000b",
		// "titlename":"この素晴らしい世界に祝福を！２　第４話「この貴族の令嬢に良縁を！」",
		// "folders":[{"folder_id":7}],"ch":"091","recmode":"DR","genres":[{"genrecode":7}],
		// "datetime":1485965100,"duration":1807,"copycount":10,"dlnaObjectID":"RD_0001000B_0000010200",
		// "recording":false,
		// "chapter":[{"chaptername":"@","duration":39,"changeFlag":0},
		// {"chaptername":"","duration":168,"changeFlag":0},{"chaptername":"@","duration":61,"changeFlag":0},
		// {"chaptername":"","duration":509,"changeFlag":0},{"chaptername":"@","duration":61,"changeFlag":0},
		// {"chaptername":"","duration":708,"changeFlag":0},{"chaptername":"@","duration":61,"changeFlag":0},
		// {"chaptername":"","duration":17,"changeFlag":0},{"chaptername":"@","duration":179,"changeFlag":0}]}}}
		Matcher ma = Pattern.compile("\\{" +
				"\"id\":\"([^\"]+)\"," +			// 1
				"\"unique_id\":\"([^\"]+)\"," +		// 2
				"\"titlename\":\"([^\"]+)\"," +		// 3
				"\"folders\":\\[([^\\]]*)\\]," +	// 4
				"\"ch\":\"([^\"]+)\"," +			// 5
				"\"recmode\":\"([^\"]+)\"," +		// 6
				"\"genres\":\\[([^\\]]*)\\]," +		// 7
				"\"datetime\":(\\d+)," +			// 8
				"\"duration\":(\\d+)," +			// 9
				"\"copycount\":(\\d+)," +			// 10
				"\"dlnaObjectID\":\"([^\"]*)\"," +	// 11
				"\"recording\":(false|true)," +		// 12
				"\"chapter\":\\[([^\\]]*)\\]\\}")	// 13
				.matcher(response);

		if ( !ma.find() ) {
			reportProgress(ERRID+"タイトル詳細情報が取得できません.ID=" + id);
			return (false);
		}

		String unique_id = ma.group(2);
		String titlename = unescapeJavaString(ma.group(3));
		String folders = ma.group(4);
		String ch = ma.group(5);
		String recmode = ma.group(6);
		String genres = ma.group(7);
		String datetime = ma.group(8);
		String duration = ma.group(9);
		String copycount = ma.group(10);
		String dlnaObjectID = ma.group(11);
		Boolean recording = ma.group(12).equals("true");
//		String recording  = ma.group(12);
		String chapter = ma.group(13);

		// 基本情報をセットする
		setTitleBasicInfo(t, titlename, ch, datetime, duration, device_id, folders, genres);

		// チャプター情報
		t.setChapter(parseChapters(chapter));
		t.setContentId(unique_id);

		// 録画モード
//		String recmode_name = value2text(vrate, recmode);
		t.setRec_mode("[" + recmode + "]");

		// それ以外の詳細情報
		HashMap<String,String> hmap = new HashMap<String,String>();
		hmap.put("copycount",  copycount);
		hmap.put("dlnaObjectID",  dlnaObjectID);
		t.setHidden_params(hmap);

		t.setRecording(recording);

		t.setDetailLoaded(true);

		return(true);
	}

	/**
	 * フォルダーのJSONテキストを解析する
	 */
	protected ArrayList<TextValueSet> parseFolders(String device_id, String s) {

		ArrayList<TextValueSet> list = new ArrayList<TextValueSet>();

		// "folders":[{"folder_id":9}],
		Matcher ma = Pattern.compile("\\{" +
				"\"folder_id\":(\\d+)\\}")	// 1
				.matcher(s);

		while ( ma.find() ) {
			String folder_id = device_id + ":" + ma.group(1);
			String folder_name = value2text(folder, folder_id);

			TextValueSet t = new TextValueSet();
			t.setText(folder_name);
			t.setValue(folder_id);
			list.add(t);
		}

		return(list);
	}

	/**
	 * ジャンルのJSONテキストを解析する
	 */
	protected ArrayList<TextValueSet> parseGenres(String s) {

		ArrayList<TextValueSet> list = new ArrayList<TextValueSet>();

		// "genres":[{"genrecode":7}],
		Matcher ma = Pattern.compile("\\{" +
				"\"genrecode\":(\\d+)\\}")	// 1
				.matcher(s);

		while ( ma.find() ) {
			String genre_code = Integer.toHexString(Integer.parseInt(ma.group(1))).toUpperCase();
			ProgGenre pg = ProgGenre.getByIEPG(genre_code);
			String genre_name = pg != null ? pg.toString() : "";

			TextValueSet t = new TextValueSet();
			t.setText(genre_name);
			t.setValue(genre_code);
			list.add(t);
		}

		return(list);
	}

	/**
	 * チャプターのJSONテキストを解析する
	 */
	protected ArrayList<ChapterInfo> parseChapters(String s) {

		ArrayList<ChapterInfo> list = new ArrayList<ChapterInfo>();

		// {"chaptername":"","duration":168,"changeFlag":0},
		Matcher ma = Pattern.compile("\\{" +
				"\"chaptername\":\"([^\"]*)\"," +
				"\"duration\":(\\d+)," +
				"\"changeFlag\":(\\d+)\\}")
				.matcher(s);

		while ( ma.find() ) {
			String chaptername = unescapeJavaString(ma.group(1));
			String duration = ma.group(2);
			String changeFlag = ma.group(3);

			ChapterInfo c = new ChapterInfo();
			c.setName(chaptername);
			c.setDuration(Integer.parseInt(duration));
			c.setChangeFlag(changeFlag.equals("1"));

			list.add(c);
		}

		return(list);
	}

	/**
	 * タイトルの編集をレコーダに通知する。これを先に呼ばないとタイトル情報更新時にエラーになる
	 */
	private boolean notifyTitleEdit(String devid, String ttlid) {
		errmsg = "";

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		// RDへの情報作成
		String pstr = "device_id=" + devid + "&title_id=" + ttlid;

		// RDへ情報送信
		reportProgress(MSGID+"レコーダーにタイトル編集を通知します.");
		String [] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/NotifyTitleEdit.php?" + pstr, null, thisEncoding);
//		String header = d[0];
		String response = d[1];

		if ( !checkGeneralResponse( "タイトル編集通知", response) ){
			return(false);
		}

		return(true);
	}

	/*
	 * タイトル更新要求のPOSTデータを生成する
	 */
	protected String createTitlePostData(TitleInfo t, TitleInfo o, String devId) {
		String postData = "";
		try {
			String title = o != null && t.getTitle().equals(o.getTitle()) ? TITLE_NOCHANGED :
				// ARIB外字を変換した文字列があれば元の外字に戻す
				URLEncoder.encode(AribCharMap.ConvStringToArib(t.getTitle()), thisEncoding);

			postData =
				"drive_id=" + devId + "&" +
				"title_id=" + t.getId() + "&" +
				"titleName=" + title + "&" +
				encodeFolders(t.getRec_folder()) + "&" +
				"folder_change=1&" +
				encodeChapters(t.getChapter());
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return postData;
	}

	/**
	 * フォルダー情報をJSONから変換したPOSTデータの形式にエンコードする
	 */
	protected String encodeFolders(ArrayList<TextValueSet> tvs){
		String s = "";
		int n=0;
		for (TextValueSet t : tvs){
			if ( !s.equals("") )
				s += "&";
			s += "folders[" + String.valueOf(n) + "][folder_id]=" + extractFolderID( t.getValue() );
			n++;
		}

		return s;
	}

	/**
	 * チャプター情報をJSONから変換したPOSTデータの形式にエンコードする
	 */
	protected String encodeChapters(ArrayList<ChapterInfo> ci){
		String s = "";
		int n=0;
		for (ChapterInfo  t : ci){
			if ( !s.equals("") )
				s += "&";
			try {
				s += "chapters[" + String.valueOf(n) + "][chnum]=" + String.valueOf(n) + "&" +
					"chapters[" + String.valueOf(n) + "][chname]=" + URLEncoder.encode(t.getName(), thisEncoding);
			} catch (UnsupportedEncodingException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			n++;
		}

		return s;
	}

	/*
	 * 固定の各種設定情報を初期化する
	 */
	protected void setSettingFixed() {
		// 録画設定
		setSettingVrate(vrate);

		// パターン情報
		setSettingPattern(tvsPatternCode, tvsPatternName);

		// エンコーダー
		setSettingEncoder(encoder);

		// 持ち出し情報
		setSettingPortable(portable);
	}

	/*
	 * 録画モードを初期化する
	 */
	protected void setSettingVrate(ArrayList<TextValueSet> tvs)	{
		// {"str":"DR","val":0}, {"str":"AF","val":64}, {"str":"AN","val":65}, {"str":"AS","val":66},
		// {"str":"AL","val":67}, {"str":"AE","val":68},
		// {"str":"XP","val":32}, {"str":"SP","val":33}, {"str":"LP","val":34}, {"str":"EP","val":35},
		// {"str":"自動(HD 4.7GB)","val":192}, {"str":"自動(HD 8.5GB)","val":193}, {"str":"自動(HD 25GB)","val":194},
		// {"str":"自動(HD 50GB)","val":195}, {"str":"自動(標準 4.7GB)","val":197}

		String texts[] =  {
			RECMODE_NAME_DR, "[AF]", "[AN]", "[AS]", "[AL]", "[AE]",
			"[XP]", "[SP]", "[LP]", "[EP]",
			"[自動(HD 4.7GB)]", "[自動(HD 8.5GB)]", "[自動(HD 25GB)]", "[自動(HD 50GB)]",
			"[自動(標準 4.7GB)]",
			null};
		String values[] = {
			RECMODE_DR, "64", "65", "66", "67", "68",
			"32", "33", "34", "35",
			"192", "193", "194", "195", "197",
			null};

		tvs.clear();

		for (int n=0; values[n] != null; n++){
			TextValueSet t = new TextValueSet();
			t.setText(texts[n]);
			t.setValue(values[n]);
			tvs.add(t);
		}
	}

	/*
	 * 繰り返しパターンを初期化する
	 */
	protected void setSettingPattern(ArrayList<TextValueSet> tvsC, ArrayList<TextValueSet> tvsN)	{
		// {"num":0,"str":"しない"},{"num":1,"str":"毎週日"},{"num":2,"str":"毎週月"},{"num":4,"str":"毎週火"},
		// {"num":8,"str":"毎週水"},{"num":16,"str":"毎週木"},{"num":32,"str":"毎週金"},{"num":64,"str":"毎週土"},
		// {"num":62,"str":"月～金"},{"num":126,"str":"月～土"},{"num":124,"str":"火～土"},{"num":127,"str":"毎日"}
		int codesRd[]={
				0, 1, 2, 3, 4, 5, RPTPTN_ID_SAT,
				RPTPTN_ID_MON2THU, RPTPTN_ID_MON2FRI, RPTPTN_ID_MON2SAT, RPTPTN_ID_TUE2SAT, RPTPTN_ID_EVERYDAY, -1};
		String codes[] = {
				"1", "2", "4", "8", "16", "32", "64",
				"30", "62", "126", "124", "127", null};
		String names[] = {
				"毎日曜日", "毎月曜日", "毎火曜日", "毎水曜日", "毎木曜日", "毎金曜日", "毎土曜日",
				"毎月～木", "毎月～金", "毎月～土", "毎火～土", "毎日", null};

		tvsC.clear();
		tvsN.clear();

		for (int n=0; codes[n] != null; n++){
			TextValueSet tc = new TextValueSet();
			tc.setText(String.valueOf(codesRd[n]));
			tc.setValue(codes[n]);
			tvsC.add(tc);

			TextValueSet tn = new TextValueSet();
			tn.setText(names[n]);
			tn.setValue(codes[n]);
			tvsN.add(tn);
		}
	}

	/**
	 * エンコーダー情報を自動生成する
	 */
	protected void setSettingEncoder(ArrayList<TextValueSet> tvs) {
		tvs.clear();

		// チューナー情報を自動生成する
		if (getTunerNum() >= 2){
			for (int i=1; i<=getTunerNum(); i++){
				TextValueSet t = new TextValueSet();
				t.setText("R" + i);
				t.setValue("R" + i);
				tvs.add(t);
			}
		}
	}

	/*
	 * 持ち出し情報を初期化する
	 */
	protected void setSettingPortable(ArrayList<TextValueSet> tvs)	{
		// {"portableId":0,"portableStr":"しない"},{"portableId":1,"portableStr":"スマホ持ち出し"},
		// {"portableId":3,"portableStr":"DVD持ち出し（VR）"},{"portableId":2,"portableStr":"SeeQVault対応SDカード転送"}
		String codes[] = {
				MOCHIDASHI_NONE, "1", "3", "2", null};
		String names[] = {
				"しない", "スマホ持ち出し", "DVD持ち出し(VR)", "SeeQVault対応SDカード転送", null};

		tvs.clear();

		for (int n=0; codes[n] != null; n++){
			TextValueSet t = new TextValueSet();
			t.setText(names[n]);
			t.setValue(codes[n]);
			tvs.add(t);
		}
	}

	/*
	 * 各種設定情報をレコーダーから取得する
	 */
	protected boolean setSettingVariable() {
		// 設定情報
//		ArrayList<TextValueSet>discs = new ArrayList<TextValueSet>();
//		ArrayList<TextValueSet>repeats = new ArrayList<TextValueSet>();
//		setSettingSelect(repeats, discs, folder, vrate);

		// 記録先デバイス
		setSettingDevice();

		// フォルダ一覧
		setSettingFolders();

		// チャンネルコードバリュー  - uva、bsaは廃止 -
		ArrayList<TextValueSet> tvsCV = new ArrayList<TextValueSet>();
		ArrayList<TextValueSet> tvsCT = new ArrayList<TextValueSet>();
		ArrayList<TextValueSet> tvsBR = new ArrayList<TextValueSet>();
		setSettingChCodeValue(tvsCV, tvsCT, tvsBR);
		if ( tvsCV.size() == 0 && tvsCT.size() == 0 && tvsBR.size() == 0) {
			System.err.println(errmsg = ERRID+"【致命的エラー】 チャンネルコードバリューが取得できません");
			return (false);
		}
		chvalue = tvsCV;
		chtype = tvsCT;
		tvsBranch = tvsBR;

		return (true);
	}

	/*
	 * デバイス情報を取得する
	 */
	protected boolean setSettingDevice() {
		ArrayList<TextValueSet>tvsD = new ArrayList<TextValueSet>();
		ArrayList<DeviceInfo>tvsDI = new ArrayList<DeviceInfo>();

		reportProgress(MSGID+"ドライブ一覧を取得します.");

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/titlelist/LoadSelectInfo.php",null, thisEncoding);
		String res = d[1];

		if (res == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return (false);
		}

		//{"drive_id":0,"drivename":"HDD","drivetype":0,"playlist_enable":true,"folder_enable":true,
		// "allsize":1897740,"freesize":1187093,"connected":true,"canwrite":true,"protected":false,
		// "mounted":true,"ready":true,"formatType":12},
		Matcher ma = Pattern.compile("\\{" +
				"\"drive_id\":(\\d+)," +			// 1
				"\"drivename\":\"([^\"]+)\"," +		// 2
				"\"drivetype\":(\\d+)," +			// 3
				"\"playlist_enable\":(true|false)," +	// 4
				"\"folder_enable\":(true|false)," +	// 5
				"\"allsize\":(\\d+)," +				// 6
				"\"freesize\":(\\d+)," +			// 7
				"\"connected\":(true|false)," +		// 8
				"\"canwrite\":(true|false)," +		// 9
				"\"protected\":(true|false)," +		// 10
				"\"mounted\":(true|false)," +		// 11
				"\"ready\":(true|false)," +			// 12
				"\"formatType\":(\\d+)\\}")			// 13
				.matcher(res);

		int allsizeALL = 0;
		int freesizeALL = 0;

		while ( ma.find() ) {
			String drive_id = ma.group(1);
			String drive_name = ma.group(2);
			String drive_type = ma.group(3);
			boolean playlist_enable = ma.group(4).equals("true");
			boolean folder_enable = ma.group(5).equals("true");
			int allsize = Integer.parseInt(ma.group(6));
			int freesize = Integer.parseInt(ma.group(7));
			boolean connected = ma.group(8).equals("true");
			boolean canwrite = ma.group(9).equals("true");
			boolean isprotected = ma.group(10).equals("true");
			boolean mounted = ma.group(11).equals("true");
			boolean ready = ma.group(12).equals("true");
			int formatType = Integer.parseInt(ma.group(13));

			TextValueSet t = new TextValueSet();
			t.setValue(drive_id);
			// デバイス名のコロン以降は無視する
			t.setText(GetDevicePrefix(drive_name));
			tvsD.add(t);

			DeviceInfo di = new DeviceInfo();
			di.setId(drive_id);
			di.setName(drive_name);
			di.setType(drive_type);
			di.setPlaylistEnable(playlist_enable);
			di.setFolderEnable(folder_enable);
			di.setAllSize(allsize);
			di.setFreeSize(freesize);
			di.setFreeMin((int)Math.round((double)freesize/MIN_PER_MB));
			di.setConnected(connected);
			di.setCanWrite(canwrite);
			di.setProtected(isprotected);
			di.setMounted(mounted);
			di.setReady(ready);
			di.setFormatType(formatType);
			tvsDI.add(di);

			allsizeALL += allsize;
			freesizeALL += freesize;
		}

		TextValueSet t = new TextValueSet();
		t.setValue(DEVICE_ALL);
		t.setText(DEVICE_NAME_ALL);
		tvsD.add(t);

		DeviceInfo di = new DeviceInfo();
		di.setId(DEVICE_ALL);
		di.setName(DEVICE_NAME_ALL);
		di.setAllSize(allsizeALL);
		di.setFreeSize(freesizeALL);
		di.setFreeMin((int)Math.round((double)freesizeALL/MIN_PER_MB));
		tvsDI.add(di);

		device = tvsD;
		setDeviceInfos(tvsDI);

		return (true);
	}

	/*
	 * デバイス情報を保存する
	 */
	protected void saveDeviceInfos(){
		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();
		devinfoTFile	= "env/devinfo."+myFileId+".xml";

		DeviceInfosToFile(getDeviceInfos(), devinfoTFile);
	}
	/*
	 * フォルダ一覧を取得する
	 */
	protected boolean setSettingFolders() {
		ArrayList<TextValueSet> tvsD = device;
		ArrayList<TextValueSet> tvsF = new ArrayList<TextValueSet>();

		reportProgress(MSGID+"フォルダ一覧を取得します.");

		TextValueSet t0 = new TextValueSet();
		t0.setText(FOLDER_NAME_NONE);
		t0.setValue(FOLDER_NONE);
		tvsF.add(t0);

		for (int n=0; n<tvsD.size(); n++){
			TextValueSet tvs = tvsD.get(n);

			// RDへの情報作成
			String drive_id =  tvs.getValue();
			String pstr = "drive_id=" + drive_id;

			// おまじない
			Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

			// RDへ情報送信
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/ReloadFolderInfo.php?" + pstr, null, thisEncoding);
			String res = d[1];

			if (res == null) {
				errmsg = ERRID+ERRMSG_NORESPONSE;
				return (false);
			}

			Matcher ma = Pattern.compile("\\{" +
					"\"folder_id\":(\\d+)," +		// 1
					"\"str\":\"([^\"]+)\"\\}")		// 2
					.matcher(res);

			while ( ma.find() ) {
				TextValueSet t = new TextValueSet();

				String device_name = tvs.getText();
				String folder_id = ma.group(1);
				String folder_name = "[" + device_name + "] " + unescapeJavaString(ma.group(2));

				t.setText(folder_name);
				t.setValue(drive_id + ":" + folder_id);
				tvsF.add(t);
			}
		}

		folder = tvsF;

		return (true);
	}

	/*
	 * フォルダー一覧をファイルに保存する
	 */
	private void saveFolders(){
		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();
		folderTFile				= "env/folders."+myFileId+".xml";

		TVSsave(folder, folderTFile);
	}

	/*
	 * タイトル一覧をファイルに保存する
	 */
	private void saveTitles(String devId){
		String myFileId = getIPAddr()+"_"+getPortNo()+"_"+getRecorderId();

		if (!DEVICE_ID.equals(DEVICE_ALL)){
			titleFile		= "env/title."+myFileId+"." + DEVICE_ID + ".xml";

			TitlesToFile(getTitles(), titleFile);
		}
		else{
			for (DeviceInfo di : getDeviceInfos() ) {
				if (di.getId().equals(DEVICE_ALL))
					continue;

				ArrayList<TitleInfo> list = new ArrayList<TitleInfo>();

				for (TitleInfo ti : getTitles()){
					if (di.getName().startsWith(ti.getRec_device()))
						list.add(ti);
				}

				titleFile		= "env/title."+myFileId+"." + di.getId() + ".xml";
				TitlesToFile(list, titleFile);
			}
		}
	}

	/*
	 * チャンネル一覧を取得する
	 */
	private void setSettingChCodeValue(ArrayList<TextValueSet> tvsvalue, ArrayList<TextValueSet> tvstype,
			ArrayList<TextValueSet> tvsbranch) {
		tvsvalue.clear();
		tvstype.clear();
		tvsbranch.clear();

		reportProgress(MSGID+"チャンネル一覧を取得します.");

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/LoadChannelList.php",null, thisEncoding);
		String res = d[1];

		if (res == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return;
		}

		// {"network":"地上","service":[... ]}
		Matcher ma = Pattern.compile("\\{\"network\":\"([^\"]+)\",\"service\":\\[([^\\]]+)\\]\\}").matcher(res);
		while ( ma.find() ) {
			String network = ma.group(1);
			String chlist = ma.group(2);
			String prefix = "";

			// "uvd","bsd","csd","l1","l2","l3"
			String typ = "";
			switch(network){
			case "地上":
				typ = CHTYPE_UVD;
				break;
			case "BS":
				typ = CHTYPE_BSD;
				prefix = CHPREFIX_BS;
				break;
			case "CS":
				typ = CHTYPE_CSD;
				prefix = CHPREFIX_CS;
				break;
			}

			// {"channelid":"G7FE00400","channelNr":"011","channelName":"ＮＨＫ総合１・東京","chnum":"011","branch":0,
			// "branchNumExists":0,"networkId":32736,"serviceId":1024,"multiSub":false},
			Matcher mb = Pattern.compile("\\{" +
					"\"channelid\":\"([^\"]+)\"," +		// 1
					"\"channelNr\":\"([^\"]+)\"," +		// 2
					"\"channelName\":\"([^\"]+)\"," +	// 3
					"\"chnum\":\"([^\"]+)\"," +			// 4
					"\"branch\":(\\d+)," +				// 5
					"\"branchNumExists\":(\\d+)," +		// 6
					"\"networkId\":(\\d+)," +			// 7
					"\"serviceId\":(\\d+)," +			// 8
					"\"multiSub\":([^}]+)}")			// 9
					.matcher(chlist);

			// var uvd_ch_text    = new Array(
			// "011-1",
			// "012-1",
			// var uvd_ch_value   = new Array(
			//		"011:32736:1024",
			//		"012:32736:1025",

			while ( mb.find() ) {
				String chno = prefix + mb.group(2);
				String chid = mb.group(4) + ":" + mb.group(7) + ":" + mb.group(8);

				TextValueSet t = new TextValueSet();
				t.setText(chno);
				t.setValue(chid);
				tvsvalue.add(t);

				TextValueSet x = new TextValueSet();
				x.setText(chid);
				x.setValue(typ);
				tvstype.add(x);

				TextValueSet b = new TextValueSet();
				b.setText(chid);
				b.setValue(mb.group(5));
				tvsbranch.add(b);
			}
		}
	}

	/*
	 * 録画設定情報を取得する（未使用）
	 */
	protected void setSettingSelect(ArrayList<TextValueSet> tvsR, ArrayList<TextValueSet> tvsD,
			ArrayList<TextValueSet> tvsF, ArrayList<TextValueSet> tvsV) {
		tvsR.clear();
		tvsD.clear();
		tvsF.clear();
		tvsV.clear();

		reportProgress(MSGID+"録画設定情報を取得します.");

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/torunavi/LoadDialogInitializationData.php?schId=0", null, thisEncoding);
		String res = d[1];

		if (res == null) {
			errmsg = ERRID+ERRMSG_NORESPONSE;
			return;
		}

		Matcher ma = Pattern.compile("\\{\"day_select\":\\[([^\\]]+)\\],").matcher(res);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("\\{\"str\":\"([^\"]+)\",\"val\":\"([^\"]+)\"\\}").matcher(ma.group(1));

			while( mb.find() ){
				TextValueSet t = new TextValueSet();
				String repeat_id = ma.group(2);
				String repeat_name = ma.group(1);
				t.setValue(repeat_id);
				t.setText(repeat_name);
				tvsR.add(t);
			}
		}

		Matcher mc = Pattern.compile("\\{\"disc_select\":\\[([^\\]]+)\\],").matcher(res);
		if ( mc.find() ) {
			Matcher md = Pattern.compile("\\{" +
				"\"driveid\":\"([^\"]+)\"," +				// 1
				"\"str\":\"([^\"]+)\"," +					// 2
				"\"folder_select\":\\[([^\\]]+)\\]," +		// 3
				"\"pqmode_select\":\\[([^\\]]+)\\]")		// 4
				.matcher(mc.group(1));

			while( md.find() ){
				String drive_id = md.group(1);
				String drive_name = md.group(2);
				String folders = md.group(3);
				String vrates = md.group(4);

				TextValueSet t = new TextValueSet();
				t.setValue(drive_id);
				t.setText(drive_name);
				tvsD.add(t);

				Matcher me = Pattern.compile("\\{" +
					"\"folderid\":\"([^\"]+)\"," +				// 1
					"\"str\":\"([^\"]+)\"\\}")					// 2
					.matcher(folders);

				while( me.find() ){
					String folder_id = mc.group(1);
					String folder_name = "[" + drive_name + "] " + unescapeJavaString(me.group(2));

					TextValueSet tc = new TextValueSet();
					tc.setValue(folder_id);
					tc.setText(folder_name);
					tvsF.add(tc);
				}

				if (!drive_id.equals(DEVICE_HDD)){
					continue;
				}

				Matcher mf = Pattern.compile("\\{" +
					"\"str\":\"([^\"]+)\"," +				// 1
					"\"pqmode\":\\[([^\\]]+)\\]\\}")		// 2
					.matcher(vrates);

				while( mf.find() ){
					String vrate_id = mf.group(1);
					String vrate_name = "[" + drive_name + "] " + unescapeJavaString(me.group(2));

					TextValueSet tc = new TextValueSet();
					tc.setValue(vrate_id);
					tc.setText(vrate_name);
					tvsV.add(tc);
				}
			}
		}
	}

	/*
	 * <device_id>:<folder_id> から<folder_id>を切り出す
	 */
	protected String extractFolderID( String fol_id ) {
		if (fol_id == null)
			return null;

		int idx = fol_id.indexOf(':');
		if (idx == -1)
			return fol_id;

		return fol_id.substring(idx+1);
	}

	public String unescapeJavaString(String st) {
	    StringBuilder sb = new StringBuilder(st.length());

	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st
	                    .charAt(i + 1);
	            // Octal escape?
	            if (nextChar >= '0' && nextChar <= '7') {
	                String code = "" + nextChar;
	                i++;
	                if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                        && st.charAt(i + 1) <= '7') {
	                    code += st.charAt(i + 1);
	                    i++;
	                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
	                            && st.charAt(i + 1) <= '7') {
	                        code += st.charAt(i + 1);
	                        i++;
	                    }
	                }
	                sb.append((char) Integer.parseInt(code, 8));
	                continue;
	            }
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            case '/':
	            	ch = '/';
	            	break;
	            // Hex Unicode: u????
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                int code = Integer.parseInt(
	                        "" + st.charAt(i + 2) + st.charAt(i + 3)
	                                + st.charAt(i + 4) + st.charAt(i + 5), 16);
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
	}

	/*
	 * デバイス名からコロンの前の部分だけを取り出す
	 */
	static String GetDevicePrefix(String device_name){
		Matcher ma = Pattern.compile("^(.*):").matcher(device_name);
		if (ma.find()){
			return ma.group(1);
		}

		return device_name;
	}
}
