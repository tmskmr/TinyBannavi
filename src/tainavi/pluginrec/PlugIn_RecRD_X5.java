package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TVProgram;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.HDDRecorder.RecType;
import tainavi.HDDRecorderUtils.MyAuthenticator;
import tainavi.TVProgram.ProgGenre;

/**
 * RD-X5だよー
 */
public class PlugIn_RecRD_X5 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_X5 clone() {
		return (PlugIn_RecRD_X5) super.clone();
	}
	
	private static final String thisEncoding = "MS932";

	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	@Override
	public String getRecorderId() { return "RD-X5"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }

	// chvalueをつかってもいーよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }
	
	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	// 予約ダイアログのラベル
	public String getLabel_XChapter() { return "無音部分ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_MsChapter() { return "DVD時ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_MvChapter() { return "音多連動ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_DVDCompat() { return "DVD互換モード"; }
	public String getLabel_Aspect() { return "DVD記録時画面比"; }
	public String getLabel_BVperf() { return "高ﾚｰﾄ節約"; }
	public String getLabel_LVoice() { return "ﾗｲﾝ音声選択"; }
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	// ログ関連
	
	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;
	
	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	public ChannelCode getChCode() {
		return cc;
	}
	
	private ChannelCode cc = new ChannelCode(getRecorderId());
	
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}
	
	protected void setErrmsg(String s) { errmsg = s; }
	
	private String errmsg = "";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	protected void setRsvedFile(String s) { rsvedFile = s; }
	protected String getRsvedFile() { return rsvedFile; }

	private String rsvedFile = "";
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	// ないよ
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	public boolean ChangeChannel(String Channel) {
		return false;
	}
	

	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/

	public boolean GetRdReserve(boolean force)
	{
		System.out.println("Run: GetRdReserve("+force+")");
		
		errmsg = "";
		
		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String vrateTFile = "env/videorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String arateTFile = "env/audiorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String folderTFile = "env/folders."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String encoderTFile = "env/encoders."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String dvdcompatTFile = "env/dvdcompat."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String deviceTFile = "env/device."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String xChapterTFile = "env/xchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String msChapterTFile = "env/mschapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String mvChapterTFile = "env/mvchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String aspectTFile = "env/aspect."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String bvperfTFile = "env/bvperf."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String lvoiceTFile = "env/lvoice."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（録画設定ほか）
			vrate = TVSload(vrateTFile);
			arate = TVSload(arateTFile);
			folder = TVSload(folderTFile);
			encoder = TVSload(encoderTFile);
			dvdcompat = TVSload(dvdcompatTFile);
			device = TVSload(deviceTFile);
			
			// 自動チャプタ関連
			xchapter = TVSload(xChapterTFile);
			mschapter = TVSload(msChapterTFile);
			mvchapter = TVSload(mvChapterTFile);
			
			// その他
			aspect = TVSload(aspectTFile);
			bvperf = TVSload(bvperfTFile);
			lvoice = TVSload(lvoiceTFile);
			
			// チャンネルコードバリュー
			chvalue = TVSload(chValueTFile);
			
			// キャッシュから読み出し（予約一覧）
			setReservesV1(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && arate.size()>0 &&
					folder.size()>0 && encoder.size()>0 &&
					xchapter.size()>0 && mschapter.size()>0 && mvchapter.size()>0) {
				return(true);
			}
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String response;
		{
			reportProgress("get reserved list(1/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get reserved list(2/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}
		
		// (1)録画設定の取得
		{
			// ハングさせないためのおまじない
			reportProgress("get reserved list(3/4).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?0",null);
		}
		{
			reportProgress("get reserved list(4/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/proginfo.htm",null);
			String res = d[1];
			
			// (1-1)画質設定
			setSettingEtc(vrate, "\"vrate\"", 1, res, vrateTFile);
			
			// (1-2)音質設定
			setSettingEtc(arate, "amode", 0, res, arateTFile);
			
			// (1-3)フォルダ一覧
			setSettingFolder(folder, res, folderTFile);
			
			// (1-4)エンコーダ
			setSettingEncoder(encoder, res, encoderTFile);
			
			// (1-5)DVD互換モード
			setSettingEtc(dvdcompat, "dvdr", 1, res, dvdcompatTFile);

			// (1-6)記録先デバイス
			setSettingEtc(device, "disc", 0, res, deviceTFile);

			// (1-7)自動チャプタ関連
			setSettingEtc(xchapter, "bAutoChapter", 0, res, xChapterTFile);
			
			setSettingEtc(mschapter, "bDvdAutoChapter", 0, res, msChapterTFile);

			setSettingEtc(mvchapter, "bAudioAutoChapter", 0, res, mvChapterTFile);
			
			// (1-8)チャンネルコードバリュー
			setSettingEtc(chvalue, "channel", 1, res, chValueTFile);
			
			// (1-11)記録時画面比
			setSettingEtc(aspect, "aspect", 0, res, aspectTFile);
			
			// (1-12)高レート節約
			setSettingEtc(bvperf, "bVPerform", 0, res, bvperfTFile);
			
			// (1-13)ライン音声選択
			setSettingEtc(lvoice, "lVoice", 0, res, lvoiceTFile);
		}
		
		// (2)予約一覧データの分析
		setReservesV1(GetRdReservedList(response));
		
		// 取得した情報の表示
		System.out.println("---Reserved List Start---");
		for ( int i = 0; i<getReserves().size(); i++ ) {
			// 詳細情報の取得
			ReserveList e = getReserves().get(i);
			System.out.println(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s\t%s",
					(i+1), e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(),	e.getZmm(),	e.getRec_min(), e.getRec_mode(), e.getTitle(), e.getChannel(), e.getCh_name()));
		}
		System.out.println("---Reserved List End---");
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		return(true);
	}
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/

	public boolean PostRdEntry(ReserveList r)
	{
		//
		if (getChCode().getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = ERRID+"【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");
		
		errmsg = "";

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String response;
		{
			reportProgress("get reserved list(1/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get reserved list(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}
		// 新規予約の行番号を取得する
		int lineno = 0;
		ma = Pattern.compile("\"b_proginfo.htm\\?(\\d+?)\"").matcher(response);
		while (ma.find()) {
			lineno = Integer.valueOf(ma.group(1));
		}
		//r.setNo(no);
		
		// RDに新規登録要請
		{
			// ハングさせないためのおまじない
			reportProgress("get program.(3/5)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?"+lineno,null);
			
			reportProgress("get program.(4/5)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/proginfo.htm",null);
		}

		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);

		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{		
			reportProgress("send request.(5/5)");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/entry.htm", pstr, null);
			response = d[1];
		}

		//System.out.println("#"+response);

		// 登録結果の確認
		ma = Pattern.compile("msg=\"(.+?)\"").matcher(response);
		if ( ma.find() ) {
			errmsg = ma.group(1);
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			
			ma = Pattern.compile("(正常に登録できました。|予約時間が一部重複しています。|現在のHDD残量では入りません。|画質を.*に修正しました。)").matcher(errmsg);
			if ( ! ma.find()) {
				return false;
			}
		}
		
		/*
		 * 予約情報の調整 
		 */
		
		// キャッシュ上の予約情報を更新する（ライン入力放送局名引継対応）(2.6β)
		getReserves().add(r);
		
		// 新しい予約一覧を取得する
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}
		
		setReservesV1(GetRdReservedList(response));
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		
		return(true);
	}

	
	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/

	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		//
		if (getChCode().getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = ERRID+"【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: UpdateRdEntry()");
		
		errmsg = "";

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		/*
		// 更新対象を探す（内部）
		ReserveList rx = null;
		for ( ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(o.getId())) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			errmsg = "キャッシュ上に操作対象が見つかりません。";
			return(false);
		}
		*/

		// 更新準備
		Matcher ma = null;
		String idx = "";
		String response;
		{
			reportProgress("get identifier(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}
		
		// 更新対象を探す（外部）
		String id = null;
		for ( ReserveList cr : GetRdReservedList(response) ) {
			if (matchReserveV1(cr, o)) {
				System.out.println("次のエントリを: "+cr.getTitle()+"; "+cr.getChannel()+"; "+cr.getRec_pattern()+"; "+cr.getAhh()+":"+cr.getAmm());
				id = cr.getId();
				break;
			}
		}
		if (id == null) {
			errmsg = ERRID+"レコーダ上に操作対象が見つかりません。";
			return(false);
		}
		System.out.println("次の情報に更新します: "+r.getTitle()+"; "+r.getChannel()+"; "+r.getRec_pattern()+"; "+r.getAhh()+":"+r.getAmm());

		// RDに更新要請
		{
			// ハングさせないためのおまじない
			reportProgress("get program.(3/7)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?"+id,null);
			
			reportProgress("get program.(4/7)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/proginfo.htm",null);
		}

		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);
		
		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{		
			reportProgress("send request.(5/7)");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/entry.htm", pstr, null);
			response = d[1];
		}

		// 更新結果の確認
		ma = Pattern.compile("msg=\"(.+?)\"").matcher(response);
		if ( ma.find() ) {
			errmsg = ma.group(1);
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			
			ma = Pattern.compile("(正常に登録できました。|予約時間が一部重複しています。|現在のHDD残量では入りません。|画質を.*に修正しました。)").matcher(errmsg);
			if ( ! ma.find()) {
				return false;
			}
		}

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");

		/*
		 * 予約情報の調整 
		 */
		// キャッシュ上の予約情報を更新する（ライン入力放送局名引継対応）
		getReserves().remove(o);
		getReserves().add(r);
		
		// 新しい予約一覧を取得する
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}

		setReservesV1(GetRdReservedList(response));

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		return(true);
	}


	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
	public ReserveList RemoveRdEntry(String delid) {
		System.out.println("Run: RemoveRdEntry()");
		
		errmsg = "";

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		// 削除対象を探す
		ReserveList rx = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			errmsg = ERRID+"キャッシュ上に操作対象が見つかりません。";
			return(null);
		}

		// 削除準備
		Matcher ma = null;
		String idx = "";
		String response;
		{
			reportProgress("get identifier(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}
		
		// 削除対象を探す（外部）
		String id = null;
		for ( ReserveList cr : GetRdReservedList(response) ) {
			if (matchReserveV1(cr, rx)) {
				System.out.println("次のエントリを削除します: "+cr.getTitle()+"; "+cr.getChannel()+"; "+cr.getRec_pattern()+"; "+cr.getAhh()+":"+cr.getAmm());
				id = cr.getId();
				break;
			}
		}
		if (id == null) {
			errmsg = ERRID+"レコーダ上に操作対象が見つかりません。";
			return(null);
		}

		// RDに削除要請
		{
			// ハングさせないためのおまじない
			reportProgress("get program.(3/7)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?"+id,null);
			
			reportProgress("get program.(4/7)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/proginfo.htm",null);
		}
		
		{		
			reportProgress("send request.(5/7)");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/delete.htm", null);
			response = d[1];
		}

		// 削除結果の確認
		ma = Pattern.compile("msg=\"(.+?)\"").matcher(response);
		if ( ma.find() ) {
			errmsg = ma.group(1);
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			
			ma = Pattern.compile("(正しく削除できました。)").matcher(errmsg);
			if ( ! ma.find()) {
				return null;
			}
		}
		
		/*
		 * 予約情報の調整 
		 */
		// キャッシュ上の予約情報を更新する（ライン入力放送局名引継対応）
		getReserves().remove(rx);
	
		// 新しい予約一覧を取得する
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
		}

		setReservesV1(GetRdReservedList(response));
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		
		return(rx);
	}

	
	
	/* ここまで */
	
	
	

	
	
	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/

	protected String joinPoststr(Hashtable<String, String> pdat)
	{
		String[] pkeys = {
			"bExec",
			"title_name",
			"genre",
			"channel",
			"date",
			"start_hour",
			"start_minute",
			"end_hour",
			"end_minute",
			"enc_type",				// for X5
			"disc",
			"vrate",
			"amode",
			"folder",				// for X5
			"dvdr",
			"lVoice",				// for X5
			"bVPerform",
			"aspect",
			"bAutoChapter",
			"bDvdAutoChapter",		// for X5
			"bAudioAutoChapter",	// for X5
			"detail",
			"dtv_sid",
			"dtv_nid",
			"net_link",				// for X5
			"end_form"
		};
		
		String pstr = "";
		for ( String key : pkeys ) {
			if (pdat.get(key) == null) {
				pstr += key+"=&";
			}
			else {
				pstr += key+"="+pdat.get(key)+"&";
			}
		}
		pstr = pstr.substring(0, pstr.length()-1);
		
		return(pstr);
	}
	
	private Hashtable<String, String> modPostdata(ReserveList r) {

		Hashtable<String, String> newdat = new Hashtable<String, String>();
		
		// 実行するよ
		newdat.put("bExec", (r.getExec())?("ON"):("OFF"));

		// 予約名
		try {
			newdat.put("title_name", URLEncoder.encode(r.getTitle(),thisEncoding));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		// 予約詳細
		try {
			newdat.put("detail", URLEncoder.encode(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")),thisEncoding));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		// ジャンル
		newdat.put("genre", _recstr2genre(r.getRec_genre()));
		
		// DVD互換モード
		newdat.put("dvdr", text2value(dvdcompat,r.getRec_dvdcompat()));
		
		// 録画チャンネル
		String channel = getChCode().getCH_WEB2CODE(r.getCh_name());
		newdat.put("channel", channel);
		String ch_no = getChCode().getCH_CODE2REC(channel);
		r.setChannel(ch_no);
		
		// エンコーダ
		newdat.put("enc_type", text2value(encoder,r.getTuner()));
		
		// 録画レート
		newdat.put("vrate", text2value(vrate,r.getRec_mode()));

		// 録音レート
		newdat.put("amode", text2value(arate,r.getRec_audio()));

		// 繰り返し
		Matcher ma = Pattern.compile("^\\d").matcher(r.getRec_pattern());
		if (ma.find()) { 
			newdat.put("date", r.getRec_pattern());
		}
		else {
			int i = 1;
			for ( String s : RPTPTN ) {
				if ( s.equals(r.getRec_pattern()) == true ) {
					newdat.put("date", String.valueOf(i));
				}
				i++;
			}
		}
		
		// 番組詳細
		//newdat.put("detail", r.getDetail()); // 上書きしてた…('д`)

		// フォルダ
		try {
			newdat.put("folder", URLEncoder.encode(text2value(folder,r.getRec_folder()),thisEncoding));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		

		newdat.put("start_hour", r.getAhh());
		newdat.put("start_minute", r.getAmm());
		newdat.put("end_hour", r.getZhh());
		newdat.put("end_minute", r.getZmm());
		
		newdat.put("bAutoChapter"		, text2value(xchapter, r.getRec_xchapter()));
		newdat.put("bDvdAutoChapter"	, text2value(mschapter, r.getRec_mschapter()));
		newdat.put("bAudioAutoChapter"	, text2value(mvchapter, r.getRec_mvchapter()));

		String val;
		
		val = text2value(device, r.getRec_device());
		newdat.put("disc", (val!=null)?(val):("0"));
		
		val = text2value(aspect, r.getRec_aspect());
		newdat.put("aspect", (val!=null)?(val):("1"));
		
		val = text2value(lvoice, r.getRec_lvoice());
		newdat.put("lVoice", (val!=null)?(val):("1"));
		
		val = text2value(bvperf, r.getRec_bvperf());
		newdat.put("bVPerform", (val!=null)?(val):("0"));
		
		// 追加
		//newdat.put("disc"				, "0");
		//newdat.put("aspect"				, "1");
		//newdat.put("lVoice"				, "1");
		//newdat.put("bVPerform"			, "0");
		//newdat.put("bAutoChapter"		, "0");
		//newdat.put("bDvdAutoChapter"	, "0");
		//newdat.put("bAudioAutoChapter"	, "0");
		newdat.put("dtv_sid"			, "0");
		newdat.put("dtv_nid"			, "0");
		newdat.put("net_link"			, "0");
		newdat.put("end_form"			, "0");
		
		return(newdat);
	}

	private String _recstr2genre(String genre)
	{
		if (genre.equals(TVProgram.ProgGenre.MOVIE.toString())) {
			return "0";
		}
		else if (genre.equals(TVProgram.ProgGenre.MUSIC.toString())) {
			return "1";
		}
		else if (genre.equals(TVProgram.ProgGenre.DORAMA.toString())) {
			return "2";
		}
		else if (genre.equals(TVProgram.ProgGenre.ANIME.toString())) {
			return "3";
		}
		else if (genre.equals(TVProgram.ProgGenre.SPORTS.toString())) {
			return "4";
		}
		else if (genre.equals(TVProgram.ProgGenre.DOCUMENTARY.toString())) {
			return "5";
		}
		else if (genre.equals(TVProgram.ProgGenre.THEATER.toString())) {
			return "6";
		}
		else if (genre.equals(TVProgram.ProgGenre.HOBBY.toString())) {
			return "7";
		}
		else if (genre.equals(TVProgram.ProgGenre.VARIETY.toString())) {
			return "8";
		}
		else {
			return "10";
		}
	}
	
	
	
	//
	protected ArrayList<ReserveList> GetRdReservedList(String response) {
		
		System.out.println("X5's GetRdReservedList()");
		
		//
		response = response.replaceAll("\n", "");
	
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		String buf = "";
		Matcher ma = Pattern.compile("<tr [^>]*?>([\\s\\S]*?)</tr>").matcher(response);
		while (ma.find()) {

			// 個々のデータを取り出す
			buf = ma.group(1);

			ReserveList entry = new ReserveList();
			
			String[] d = new String[10];
			Matcher mb = Pattern.compile("<td.*?>(.*?)</td>").matcher(buf);
			for (int i=0; i<d.length; i++) {
				if ( mb.find()) {
					d[i] = mb.group(1);
				}
				//System.out.println(i+") "+d[i]);
			}
			
			Matcher mc = Pattern.compile(">新規予約<").matcher(buf);
			if (mc.find()) {
				break;
			}
			
			// 予約ID
			entry.setId(String.valueOf(Integer.valueOf(d[0])-1));
			
			// 予約実行ON/OFF
			mb = Pattern.compile("check_off\\.gif").matcher(d[1]);
			if (mb.find()) {
				entry.setExec(false);
			}
			
			// 予約名のエスケープを解除する
			String title = CommonUtils.unEscape(d[2]).replaceAll("<[bB][rR]>","");
			mb = Pattern.compile("<[aA] .*?>(.+?)</[aA]>").matcher(title);
			if (mb.find()) title = mb.group(1);
			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));
			
			// エンコーダ
			mb = Pattern.compile("act_(.+?)\\.gif").matcher(d[3]);
			if (mb.find()) {
				entry.setTuner(mb.group(1));
			}
			else {
				entry.setTuner("R1");
			}
			
			// 日時
			entry.setRec_pattern(d[5]);
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[6]);
			if (mb.find()) {
				entry.setAhh(mb.group(1));
				entry.setAmm(mb.group(2));
				entry.setZhh(mb.group(3));
				entry.setZmm(mb.group(4));
			}
			entry.setRec_nextdate(CommonUtils.getNextDate(entry));
			entry.setRec_min(CommonUtils.getRecMin(entry.getAhh(), entry.getAmm(), entry.getZhh(), entry.getZmm()));
			getStartEndDateTime(entry);
			
			entry.setChannel(d[4]);
			entry.setCh_name(getChCode().getCH_REC2WEB(d[4]));

			entry.setRec_mode(d[8]);
			entry.setRec_audio(d[9]);
			
			//entry.rec_folder = data.get();	// 予約一覧からはとれない
			//entry.rec_genre = data.get();		//　予約一覧からはとれない
			
			// 予約情報を保存
			newReserveList.add(entry);
		}
		
		return(newReserveList);
	}
	
	
	
	/*
	 * X5/H1/H2で共有するぜ
	 */
	
	protected boolean setSettingFolder(ArrayList<TextValueSet> tvs, String res, String savefile) {
		ArrayList<TextValueSet> newtvs = new ArrayList<TextValueSet>();
		Matcher mb = Pattern.compile("obj\\.folder\\[n\\]\\.text=\"(.+?)\";\\s*?obj\\.folder\\[n\\]\\.value=\"(.+?)\";").matcher(res);
		while (mb.find()) {
			boolean dub = false;
			for ( TextValueSet t : newtvs ) {
				if (t.getText().equals(mb.group(1))) {
					dub = true;
					break;
				}
			}
			if (dub == false) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(1));
				t.setValue(mb.group(2));
				newtvs.add(t);
			}
		}
		
		if ( newtvs.size() == 0 ) {
			errmsg = ERRID+"レコーダ設定情報が取得できなかった： "+"folder";
			System.err.println(errmsg);
			return false;
		}
		
		tvs.clear();
		for ( TextValueSet t : newtvs ) {
			tvs.add(t);
		}
		TVSsave(tvs,savefile);
		return true;
	}
	
	private boolean setSettingEncoder(ArrayList<TextValueSet> tvs, String res, String savefile) {
		ArrayList<TextValueSet> newtvs = new ArrayList<TextValueSet>();
		Matcher mb = Pattern.compile("<input type=\"radio\" [^>]*? name=enc_type value=\"(.+?)\".*?><img src=\"/img/parts/toru_(.+?)\\.gif\">").matcher(res);
		while (mb.find()) {
			TextValueSet t = new TextValueSet();
			t.setText(mb.group(2));
			t.setValue(mb.group(1));
			newtvs.add(t);
		}
		
		if (newtvs.size() == 0) {
			errmsg = ERRID+"レコーダ設定情報が取得できなかった： "+"enc_type";
			System.err.println(errmsg);
			return false;
		}

		tvs.clear();
		for ( TextValueSet t : newtvs ) {
			tvs.add(t);
		}
		TVSsave(tvs,savefile);
		return true;
	}

	protected boolean setSettingEtc(ArrayList<TextValueSet> tvs, String key, int typ, String res, String savefile) {
		ArrayList<TextValueSet> newtvs = new ArrayList<TextValueSet>();
		String valExpr = "(\\d+)";
		if (typ == 1) {
			valExpr = "\"(.+?)\"";
		}
		Matcher mb = Pattern.compile("<(SELECT|select)[^>]+?name="+key+"[^.]*?>([\\s\\S]+?)</(SELECT|select)>").matcher(res);
		if (mb.find()) {
			Matcher mc = Pattern.compile("<(OPTION|option)\\s+(VALUE|value)\\s*=\\s*"+valExpr+"\\s*( selected)?>\\s*(.+?)\\s*</(OPTION|option)>").matcher(mb.group(2));
			while (mc.find()) {
				TextValueSet t = new TextValueSet();
				if (key.equals("\"vrate\"")) {
					t.setText(mc.group(5).replaceAll(" ", ""));
				}
				else if (key.equals("channel")) {
					t.setText(mc.group(5).replaceAll("^CH ", "CH"));
				}
				else {
					t.setText(mc.group(5));
				}
				t.setValue(mc.group(3));
				if (mc.group(4) != null) {
					t.setDefval(true);
				}
				newtvs.add(t);
			}
		}
		
		if ( newtvs.size() == 0 ) {
			errmsg = ERRID+"レコーダ設定情報が取得できなかった： "+key;
			System.err.println(errmsg);
			return false;
		}
		
		tvs.clear();
		for ( TextValueSet t : newtvs ) {
			tvs.add(t);
		}
		TVSsave(tvs,savefile);
		return true;
	}
}
