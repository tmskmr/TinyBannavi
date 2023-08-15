package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.GetRDStatus;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TVProgram;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;
import tainavi.HDDRecorderUtils.MyAuthenticator;
import tainavi.TVProgram.ProgGenre;

/**
 * REGZA RD-BZ700用のレコーダプラグインです。
 * @author original:番ナビスレ12・102氏
 * @version 3.15.4β ネットdeナビのHTMLが変更されてエンコーダ情報がとりにくくなったのでZ260同様固定でもたせるようにした
 */
public class PlugIn_RecRD_BZ700 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_BZ700 clone() {
		return (PlugIn_RecRD_BZ700) super.clone();
	}
	
	private static final String thisEncoding = "MS932";
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA RD-BZ700"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }

	// chvalueを使っていいよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }
	
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

	protected static final String ITEM_ENCODER_R1 = "R1";
	protected static final String ITEM_ENCODER_R2 = "R2";

	protected static final String ITEM_VIDEO_TYPE_DR  = "[DR]";
	protected static final String ITEM_VIDEO_TYPE_VR  = "[VR]";
	protected static final String ITEM_VIDEO_TYPE_AVC = "[AVC]";

	protected static final String ITEM_MEDIA_HDD = "[HDD] ";
	protected static final String ITEM_MEDIA_USB = "[USB] ";

	protected static final String VALUE_ENCODER_RE_1 = "1";
	//protected static final String VALUE_ENCODER_RE_2 = "2";
	protected static final String VALUE_ENCODER_R1 = "3";
	protected static final String VALUE_ENCODER_R2 = "4";
	
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
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	public boolean ChangeChannel(String Channel) {
		
		if (Channel == null) {
			return true;
		}
		
		int curBC;
		int newBC;
		String chNo;
		//byte enc;
		
		errmsg = "";
		
		/*
		 * 変更前の放送（地上波／ＢＳ／ＣＳ）
		 */
		{
			String ch = null;
			for (int i=0; i<3 && (ch = gs.getCurChannel(getIPAddr())) == null; i++) {
				CommonUtils.milSleep(500);
			}
			if (ch == null) {
				errmsg = "レコーダへのアクセスに失敗しました(チャンネルリモコン)。";
				System.err.println(errmsg);
				return false;
			}
			
			//byte[] ba = ch.getBytes();
			//enc = ba[0];
			ch = ch.substring(1,5);
			
			if (ch.matches("^\\d.+")) {
				curBC = 0;
			}
			else if (ch.startsWith("BS")) {
				curBC = 1;
			}
			else if (ch.startsWith("CS")) {
				curBC = 2;
			}
			else {
				errmsg = "現在のチャンネルが認識できません("+ch+")。";
				System.err.println(errmsg);
				return false;
			}
		}
		
		/*
		 * 変更後のＣＨ
		 */
		{
			// 放送（地上波／ＢＳ／ＣＳ）
			String cd = cc.getCH_WEB2CODE(Channel);
			String ch = cc.getCH_CODE2REC(cd);
			String typ = text2value(chtype, cd);
			if (typ == null) {
				errmsg = "鯛ナビに情報が同期されていないチャンネルです("+Channel+", "+ch+")。";
				System.err.println(errmsg);
				return false;
			}
			else if (typ.matches("^l[123]$")) {
				errmsg = "外部入力にアサインされているチャンネルには変更できません("+Channel+", "+ch+", "+typ+")。";
				System.err.println(errmsg);
				return false;
			}
			else if (typ.equals("uvd")) {
				newBC = 0;
			}
			else if (typ.equals("bsd")) {
				newBC = 1;
			}
			else if (typ.equals("csd")) {
				newBC = 2;
			}
			else {
				errmsg = "放送種別が識別できません。プログラム異常です("+Channel+", "+ch+")。";
				System.err.println(errmsg);
				return false;
			}
			
			// CH番号
			Matcher ma = Pattern.compile("(\\d\\d\\d)").matcher(ch);
			if (ma.find()) {
				chNo = ma.group(1);
			}
			else {
				errmsg = "ＣＨ番号が取得できません("+Channel+", "+ch+")。";
				System.err.println(errmsg);
				return false;
			}
		}
		
		/*
		 * ＣＨ変更実行
		 */
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		int cBC;
		for (int i=0; i<3 && newBC != (cBC = (curBC+i)%3); i++) {
			// 切り替え実行
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=21", null);
			
			// 地上波→ＢＳ・ＢＳ→ＣＳは切り替え完了までに時間がかかる
			CommonUtils.milSleep((cBC == 0 || cBC == 1)?(3000):(1000));
		}
		
		reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=25", null);
		CommonUtils.milSleep(1000);
		for (int i=0; i<3; i++) {
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0"+chNo.substring(i,i+1), null);
			CommonUtils.milSleep(200);
		}
		reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=44", null);
		
		return true;
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
		
		rsvedFile				= "env/reserved."+myFileId+".xml";
		
		String arateTFile		= "env/audiorate."+myFileId+".xml";
		String folderTFile		= "env/folders."+myFileId+".xml";
		String encoderTFile		= "env/encoders."+myFileId+".xml";
		String dvdcompatTFile	= "env/dvdcompat."+myFileId+".xml";
		String deviceTFile		= "env/device."+myFileId+".xml";
		String xChapterTFile	= "env/xchapter."+myFileId+".xml";
		String msChapterTFile	= "env/mschapter."+myFileId+".xml";
		String mvChapterTFile	= "env/mvchapter."+myFileId+".xml";
		String chValueTFile		= "env/chvalue."+myFileId+".xml";
		String chTypeTFile		= "env/chtype."+myFileId+".xml";
		String aspectTFile		= "env/aspect."+myFileId+".xml";
		String bvperfTFile		= "env/bvperf."+myFileId+".xml";
		String lvoiceTFile		= "env/lvoice."+myFileId+".xml";
		String autodelTFile		= "env/autodel."+myFileId+".xml";
		
		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// ハードコーディング（録画設定ほか）
			setSettingVrate(vrate);
			setSettingGenre(genre);
			
			// キャッシュから読み出し（録画設定ほか）
			arate = TVSload(arateTFile);
			folder = TVSload(folderTFile);
			encoder = TVSload(encoderTFile);
			dvdcompat = TVSload(dvdcompatTFile);
			device = TVSload(deviceTFile);
			xchapter = TVSload(xChapterTFile);
			mschapter = TVSload(msChapterTFile);
			mvchapter = TVSload(mvChapterTFile);
			chvalue = TVSload(chValueTFile);
			chtype = TVSload(chTypeTFile);
			
			// その他
			aspect = TVSload(aspectTFile);
			bvperf = TVSload(bvperfTFile);
			lvoice = TVSload(lvoiceTFile);
			autodel = TVSload(autodelTFile);
			
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (arate.size()>0 && folder.size()>0 && encoder.size()>0 && device.size()>0 &&
					xchapter.size()>0 && mschapter.size()>0 && mvchapter.size()>0 &&
					chvalue.size()>0 && chtype.size()>0) {
				return(true);
			}
		}
		
		boolean isfault = false;
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String response="";
		{
			reportProgress(MSGID+"処理IDを取得します(1/3).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			response = d[1];
			
			if (response == null) {
				errmsg = ERRID+"レコーダーが反応しません";
				return(false);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if ( ! ma.find()) {
			errmsg = ERRID+"レコーダーからの情報取得に失敗しました（処理ID）";
			return(false);
		}
		
		idx = ma.group(1);	// 処理ID
		
		{
			reportProgress(MSGID+"予約一覧を取得します(2/3).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			response = d[1];
			
			if (response == null) {
				errmsg = ERRID+"レコーダーが反応しません";
				return(false);
			}
		}
		
		// 予約エントリー数を取得する
		int RsvCnt = 0;
		ma = Pattern.compile("RsvCnt\\s*=\\s*(\\d+?);").matcher(response);
		if ( ! ma.find()) {
			errmsg = ERRID+"レコーダーからの情報取得に失敗しました（予約一覧）";
			return false;
		}
		RsvCnt = Integer.valueOf(ma.group(1));
		
		// (1)録画設定の取得
		{
			// ハング防止のおまじない
			reportProgress(MSGID+"録画設定を取得します(3/3).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);
			
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
			String res = d[1];
			
			if (res == null) {
				errmsg = ERRID+"レコーダーが反応しません";
				return(false);
			}
			
			//Matcher mb = null;
			
			// 正常取得チェック
			if ( ! res.matches("[\\s\\S]*var hdd_folder_text[\\s\\S]*")) {
				errmsg = ERRID+"レコーダーからの情報取得に失敗しました（録画設定）";
				return false;
			}
			
			ArrayList<TextValueSet> tvsa = null;
			
			// (1-1)画質設定 [保存しない]
			setSettingVrate(vrate);
			
			// (1-2)音質設定
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"amode",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(arate = tvsa, arateTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 音質設定が取得できません");
				isfault = true;
			}
			
			// (1-3)フォルダ一覧
			tvsa = new ArrayList<TextValueSet>();
			setSettingFolder(tvsa,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(folder = tvsa, folderTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 フォルダ一覧が取得できません");
				isfault = true;
			}
			
			// (1-4)エンコーダ
			tvsa = new ArrayList<TextValueSet>();
			setSettingEncoder(tvsa,res);
			if ( tvsa.size() == 0 ) {
				// HTMLからとれなければ固定で返す
				setSettingEncoder(tvsa);
			}
			if ( tvsa.size() > 0 ) {
				TVSsave(encoder = tvsa, encoderTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 エンコーダが取得できません");
				isfault = true;
			}
			
			// (1-5)DVD互換モード
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"dvdr",1,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(dvdcompat = tvsa, dvdcompatTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 DVD互換モードが取得できません");
				isfault = true;
			}
			
			// (1-6)記録先デバイス
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"media",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(device = tvsa, deviceTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 記録先デバイスが取得できません");
				isfault = true;
			}
			
			// (1-7)自動チャプター関連
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"mutechapter",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(xchapter = tvsa, xChapterTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 自動ﾁｬﾌﾟﾀ（１）が取得できません");
				isfault = true;
			}
			
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"magicchapter",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(mschapter = tvsa, msChapterTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 自動ﾁｬﾌﾟﾀ（２）が取得できません");
				isfault = true;
			}
			
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"cmchapter",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(mvchapter = tvsa, mvChapterTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 自動ﾁｬﾌﾟﾀ（３）が取得できません");
				isfault = true;
			}
			
			// (1-8)チャンネルコードバリュー  - uva、bsaは廃止 -
			ArrayList<TextValueSet> tvsb = new ArrayList<TextValueSet>(); 
			ArrayList<TextValueSet> tvsc = new ArrayList<TextValueSet>(); 
			setSettingChCodeValue(tvsb,tvsc,res);
			if ( tvsb.size() > 0 && tvsc.size() > 0 ) {
				TVSsave(chvalue = tvsb, chValueTFile);
				TVSsave(chtype = tvsc, chTypeTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 チャンネルコードバリューが取得できません");
				isfault = true;
			}
			
			// (1-9)ジャンル [保存しない]
			setSettingGenre(genre);
			
			// (1-10)番組詳細 [関係ない]
			
			// (1-11)録画のりしろ
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"edge",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(aspect = tvsa, aspectTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 録画のりしろが取得できません");
				isfault = true;
			}
			
			// (1-12)録画優先度
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"rec_priority",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(bvperf = tvsa, bvperfTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 録画優先度が取得できません");
				isfault = true;
			}
			
			// (1-13)ライン音声選択
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"lvoice",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(lvoice = tvsa, lvoiceTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 ライン音声選択が取得できません");
				isfault = true;
			}
			
			// (1-14)自動削除
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"auto_del",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(autodel = tvsa, autodelTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 自動削除が取得できません");
				isfault = true;
			}
		}
		
		// 予約一覧データの分析
		ArrayList<ReserveList> ra = decodeReservedList(response); 
		for (ReserveList entry : ra) {
			
			// 放送局名変換
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
			
			// TS/TSE->DR/ACV
			if (entry.getRec_mode().startsWith("[TS]")) {
				entry.setRec_mode(entry.getRec_mode().replaceFirst("\\[TS\\]", ITEM_VIDEO_TYPE_DR));
			}
			else if (entry.getRec_mode().startsWith("[TSE]")) {
				entry.setRec_mode(entry.getRec_mode().replaceFirst("\\[TSE\\]", ITEM_VIDEO_TYPE_AVC));
			}
			else if (entry.getRec_mode().matches("^\\s*$")) {
				entry.setRec_mode("");
			}
			
			// TS->DR
			translateAttributeTuner(entry);
			
			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributes(entry, getReserves());
		}
		setReserves(ra);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		// 取得した情報の表示
		System.out.println("---Reserved List Start---");
		for ( int i = 0; i<getReserves().size(); i++ ) {
			// 詳細情報の取得
			ReserveList e = getReserves().get(i);
			System.out.println(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s(%s)\t%s\t%s",
					(i+1), e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(),	e.getZmm(),	e.getRec_min(), e.getRec_mode(), e.getTitle(), e.getTitlePop(), e.getChannel(), e.getCh_name()));
		}
		System.out.println("---Reserved List End---");
		
		return(!isfault);
	}
	
	@Override
	public boolean isThereAdditionalDetails() {
		return true;
	}
	@Override
	public boolean GetRdReserveDetails()
	{
		/*
		 *  前処理
		 */
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		Matcher mx = null;
		String idx = "";
		String header="";
		String response="";
		{
			reportProgress("処理IDを取得します(1/1).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
		}
		mx = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if ( ! mx.find()) {
			errmsg = "レコーダーからの情報取得に失敗しました（処理ID）";
			return(false);
		}
		idx = mx.group(1);
		
		/*
		 *  詳細確認
		 */
		int lineno = 0;
		ArrayList<ReserveList> ra = getReserves();
		for (ReserveList entry : ra) {
			
			lineno++;
			
			// 詳細情報を引いてみる
			reportProgress("+番組詳細を取得します("+lineno+"/"+ra.size()+").");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?"+entry.getId()+"&"+lineno,null);
			
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
			if (d[1] == null) {
				reportProgress("レコーダーからの戻り値が不正です");
				continue;
			}
			
			// (1-1)画質設定
			mx = Pattern.compile("videotype_digital_value = (\\d+);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(画質1)");
				continue;
			}
			String vtypestr = mx.group(1);
			mx = Pattern.compile("videomode_digital_value = (\\d+);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(画質2)");
				continue;
			}
			String vmodestr = mx.group(1);
			entry.setRec_mode(value2text(vrate, vtypestr+":"+vmodestr));

			// (1-2)音質設定 [一覧から取得]
			
			// (1-6)記録先デバイス [一覧から取得]
			
			// (1-3)フォルダ
			mx = Pattern.compile("var folder_current = \"(.*?)\";").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(フォルダ)");
				continue;
			}
			String fTyp = (entry.getRec_device().equals("HDD")) ? (ITEM_MEDIA_HDD) : (ITEM_MEDIA_USB);
			for ( TextValueSet t : folder ) {
				if (t.getText().startsWith(fTyp)) {
					if (t.getValue().equals(mx.group(1))) {
						entry.setRec_folder(t.getText());
						break;
					}
				}
			}
			
			// (1-4)エンコーダ [一覧から取得]
			
			// (1-5)DVD互換モード
			mx = Pattern.compile("var dvdr_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(DVD互換モード)");
				continue;
			}
			entry.setRec_dvdcompat(dvdcompat.get(Integer.valueOf(mx.group(1))).getText());
			
			// (1-7)自動チャプター関連
			mx = Pattern.compile("var mutechapter_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(自動チャプタ1)");
				continue;
			}
			entry.setRec_xchapter(xchapter.get(Integer.valueOf(mx.group(1))).getText());
			
			mx = Pattern.compile("var magicchapter_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(自動チャプタ2)");
				continue;
			}
			entry.setRec_mschapter(mschapter.get(Integer.valueOf(mx.group(1))).getText());
			
			mx = Pattern.compile("var cmchapter_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(自動チャプタ3)");
				continue;
			}
			entry.setRec_mvchapter(mvchapter.get(Integer.valueOf(mx.group(1))).getText());
			
			// (1-8)チャンネル [一覧から取得]
			
			// (1-9)ジャンル
			mx = Pattern.compile("var genre_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(ジャンル)");
				continue;
			}
			int gVal = Integer.valueOf(mx.group(1));
			if (gVal >= genre.size()) {
				gVal = genre.size()-1;
			}
			entry.setRec_genre(genre.get(gVal).getText());
			
			// (1-10)番組詳細
			mx = Pattern.compile("var title_detail = \"(.*?)\";").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(番組詳細)");
				continue;
			}
			entry.setDetail(CommonUtils.unEscape(mx.group(1)).replaceAll("\\\\r\\\\n","\n"));
			
			// (1-11)録画のりしろ
			mx = Pattern.compile("var edge_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(録画のりしろ)");
				continue;
			}
			entry.setRec_aspect(aspect.get(Integer.valueOf(mx.group(1))).getText());
			
			// (1-12)録画優先度
			mx = Pattern.compile("var rec_priority_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(録画優先度)");
				continue;
			}
			entry.setRec_bvperf(bvperf.get(Integer.valueOf(mx.group(1))).getText());
			
			// (1-13)ライン音声選択
			mx = Pattern.compile("var lvoice_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(ライン音声選択)");
				continue;
			}
			entry.setRec_lvoice(lvoice.get(Integer.valueOf(mx.group(1))).getText());
			
			// (1-14)自動削除
			mx = Pattern.compile("var auto_del_current = (\\d+?);").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(自動削除)");
				continue;
			}
			entry.setRec_autodel(autodel.get(Integer.valueOf(mx.group(1))).getText());
		}
		
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
		String chcode = cc.getCH_WEB2CODE(r.getCh_name());
		if (chcode == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
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
		String header;
		String response;
		{
			reportProgress("get reserved list(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			reportProgress("get reserved list(2/7).");
			idx = ma.group(1);
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
			
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		
		// 予約エントリー数を取得する
		int RsvCnt = 0;
		ma = Pattern.compile("RsvCnt\\s*=\\s*(\\d+?);").matcher(response);
		if (ma.find()) {
			RsvCnt = Integer.valueOf(ma.group(1));
		}
		
		// RDに新規登録要請
		{
			// ハング防止のおまじない
			reportProgress("get program(3/7).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);
			
			reportProgress("get program(4/7).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}

		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);
		
		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{		
			reportProgress("send request(5/7).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/entry.htm", pstr, null);
			header = d[0];
			response = d[1];
		}
		
		// 登録結果の確認
		ma = Pattern.compile("alert\\(msg\\)").matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("\\bmsg=\"([\\s\\S]+?)\";").matcher(response);
			if (mb.find()) {
				errmsg = mb.group(1);
				System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
				Matcher mc = Pattern.compile("(予約時間が重複しています。|Ｗ録の振り替えをおこないました)").matcher(errmsg);
				if ( ! mc.find() ) {
					return(false);
				}
			}			
		}


		
		/*
		 * 予約情報の調整 
		 */
		
		// 予約ID番号を取得（キャッシュに存在しない番号が新番号）
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
		}
		r.setId(getNewId(response));
		
		if ( r.getId() == null ) {
			errmsg = ERRID+"【致命的エラー】 予約は成功したと思われますが、該当する予約IDを見つけることができませんでした。";
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			return false;
		}
		
		reportProgress("予約IDは"+r.getId()+"です。");

		
		// 音質（TS/TSEでは音質の設定はできない）
		//ma = Pattern.compile("^\\[TS").matcher(r.getRec_mode());
		if (r.getRec_mode().startsWith(ITEM_VIDEO_TYPE_DR) || r.getRec_mode().startsWith(ITEM_VIDEO_TYPE_AVC)) {
			r.setRec_audio("");
		}

		// 予約パターンID
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));

		// 次回予定日
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		//r.setRec_nextdate(getNextDate(r.getRec_pattern(), r.getZhh()+":"+r.getZmm()));
		
		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		
		// 開始日時・終了日時
		getStartEndDateTime(r);
		
		// 予約リストを更新
		getReserves().add(r);
		
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
		String chcode = cc.getCH_WEB2CODE(r.getCh_name());
		if (chcode == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}

		System.out.println("Run: UpdateRdEntry()");
		
		errmsg = "";

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// 更新準備
		Matcher ma = null;
		String idx = "";
		int lineno = 0;
		String header;
		String response;
		{
			reportProgress("get reserved list(1/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
			
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		ma = Pattern.compile("c1\\[(\\d+?)\\]=\""+r.getId()+"\";").matcher(response);
		if ( ma.find() ) {
			lineno = Integer.valueOf(ma.group(1))+1;
		}
		
		// RDに更新要請
		{
			// ハング防止のおまじない
			reportProgress("get program(3/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?"+r.getId()+"&"+lineno,null);
			
			reportProgress("get program(4/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}

		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);
		
		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{		
			reportProgress("send request.(5/5)");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/entry.htm", pstr, null);
			header = d[0];
			response = d[1];
		}

		// 更新結果の確認
		ma = Pattern.compile("alert\\(msg\\)").matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("\\bmsg=\"([\\s\\S]+?)\";").matcher(response);
			if (mb.find()) {
				errmsg = mb.group(1);
				System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
				Matcher mc = Pattern.compile("(予約時間が重複しています。|Ｗ録の振り替えをおこないました)").matcher(errmsg);
				if ( ! mc.find() ) {
					return(false);
				}
			}			
		}

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");

		/*
		 * 予約情報の調整 
		 */
		
		// 音質（TS/TSEでは音質の設定はできない）
		//ma = Pattern.compile("^\\[TS").matcher(r.getRec_mode());
		if (r.getRec_mode().startsWith(ITEM_VIDEO_TYPE_DR) || r.getRec_mode().startsWith(ITEM_VIDEO_TYPE_AVC)) {
			r.setRec_audio("");
		}

		// 予約パターンID
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		
		// 次回予定日
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		
		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		
		// 開始日時・終了日時
		getStartEndDateTime(r);
		
		// 情報置き換え
		getReserves().remove(o);
		getReserves().add(r);

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
			return(null);
		}

		// 予約行番号を取得
		Matcher ma = null;
		String idx = "";
		int lineno = 0;
		String header;
		String response;
		{
			reportProgress("get identifier(1/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(null);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("c1\\[(\\d+?)\\]=\""+rx.getId()+"\";").matcher(response);
		if ( ma.find() ) {
			lineno = Integer.valueOf(ma.group(1))+1;
		}

		// RDに削除要請
		{
			// ハング防止のおまじない
			reportProgress("get program(3/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?"+rx.getId()+"&"+lineno,null);
			
			reportProgress("get program(4/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}
		{		
			reportProgress("send request.(5/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/delete.htm", null);
			header = d[0];
			response = d[1];
		}

		// 削除結果の確認
		ma = Pattern.compile("alert\\(msg\\)").matcher(response);
		if ( ma.find() ) {
			Matcher mb = Pattern.compile("\\bmsg=\"([\\s\\S]+?)\";").matcher(response);
			if (mb.find()) {
				errmsg = mb.group(1);
				System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
				Matcher mc = Pattern.compile("Ｗ録の振り替えをおこないました").matcher(errmsg);
				if ( ! mc.find() ) {
					return(null);
				}
			}			
		}
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		return(rx);
	}
	
	
	/* ここまで */
	
	
	

	
	
	/* 個別コード－ここから最後まで */

	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/

	private String joinPoststr(Hashtable<String, String> pdat)
	{
		String[] pkeys = {
			"bExec",
			"start_form",
			"title_name",
			"detail",
			"genre",
			"enc_type",
			"broadcast",
			"channel_list",
			"rec_priority",
			"maiyoubi_type",
			"date",
			"start_hour",
			"start_minute",
			"end_hour",
			"end_minute",
			"disc",
			"folder",
			"vrate",
			"amode",
			"videotype_digital",	// new
			"videomode_digital",	//new
			"audiomode_digital",
			"auto_delete",
			"dvdr",
			"lVoice",
			"edge_left",
			"bAutoChapter",
			"MagicChapter",
			"CM_Chapter",
			"channel_no",
			"dtv_sid",
			"dtv_nid",
			"net_link",
			"add_ch_text",
			"add_ch_value",
			"sport_ext_submit",
			"title_link_submit",
			"end_form"
		};
		
		String pstr = "";
		for ( String key : pkeys ) {
			if (pdat.containsKey(key)) {
				pstr += key+"="+pdat.get(key)+"&";
			}
		}
		pstr = pstr.substring(0, pstr.length()-1);
		
		System.err.println("poststr: "+pstr);
		
		return(pstr);
	}
	
	private Hashtable<String, String> modPostdata(ReserveList r) {

		Hashtable<String, String> newdat = new Hashtable<String, String>();
		try {
			// 実行するよ
			newdat.put("bExec", (r.getExec())?("ON"):("OFF"));
			if (r.getUpdateOnlyExec()) {
				return(newdat);
			}
	
			// 予約名・予約詳細
			if ( r.getAutocomplete() ) {
				newdat.put("title_name", "");
				newdat.put("detail", "");
			}
			else {
				try {
					newdat.put("title_name", URLEncoder.encode(CommonUtils.substringrb(r.getTitle(),86),thisEncoding));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
		
				// 予約詳細
				try {
					newdat.put("detail", URLEncoder.encode(CommonUtils.substringrb(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")),75*5), thisEncoding));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			
			// 保存先
			try {
				newdat.put("folder", URLEncoder.encode(text2value(folder, r.getRec_folder()),thisEncoding));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			// ジャンル
			String gstr = text2value(genre,r.getRec_genre());
			if (gstr == null || gstr.length() == 0) {
				gstr = text2value(genre,TVProgram.ProgGenre.NOGENRE.toString());
			}
			newdat.put("genre", gstr);
			
			// DVD互換モード
			newdat.put("dvdr", text2value(dvdcompat, r.getRec_dvdcompat()));
			
			// 録画チャンネル - uva、bsaは廃止 -
			String channel = cc.getCH_WEB2CODE(r.getCh_name());
			String ch_no = cc.getCH_CODE2REC(channel);
			String typ = text2value(chtype, channel);
			if (typ.equals("l1")) {
				newdat.put("broadcast","2");		// 外部入力(L1)
			}
			else if (typ.equals("l2")) {
				newdat.put("broadcast","3");		// 外部入力(L2)
			}
			else if (typ.equals("l3")) {
				newdat.put("broadcast","4");		// 外部入力(L3)
			}
			else if (typ.equals("bsd")) {
				newdat.put("broadcast","10");		// BSデジタル
			}
			else if (typ.equals("csd")) {
				newdat.put("broadcast","11");		// 110度CSデジタル
			}
			else if (typ.equals("uvd")) {
				newdat.put("broadcast","12");		// 地上デジタル
			}
			else {
				// 普通ここには落ちない
				if (ch_no.startsWith("C")) {
					newdat.put("broadcast","2");		// "C***"は外部入力(L1)
				}
				else if (ch_no.startsWith("SP")) {
					newdat.put("broadcast","4");		// "SP***"は外部入力(L3)
				}
				else {
					newdat.put("broadcast","3");		// 未定義は全部外部入力(L2)
				}
			}
			
			newdat.put("channel_list", channel);
			newdat.put("channel_no", channel);
			
			// 録画レート
			newdat.put("enc_type", text2valueEncoderSP(encoder, r.getTuner(), r.getRec_mode()));
			
			Matcher ma = Pattern.compile("^(\\d+?):(.*?)$").matcher(text2value(vrate, r.getRec_mode()));
			if (ma.find()) {
				if (ma.group(1).equals("1")) {
					// VR
					newdat.put("vrate","1");	
					newdat.put("amode","1");
					newdat.put("videotype_digital",ma.group(1));	
					newdat.put("videomode_digital",ma.group(2));
					newdat.put("audiomode_digital",text2value(arate, r.getRec_audio()));
				}
				else if (ma.group(1).equals("2")) {
					// TSE
					newdat.put("vrate","1");	
					newdat.put("amode","1");	
					newdat.put("videotype_digital",ma.group(1));	
					newdat.put("videomode_digital",ma.group(2));
				}
				else {
					// TS
					newdat.put("vrate","1");	
					newdat.put("amode","1");	
					newdat.put("videotype_digital",ma.group(1));	
					//newdat.put("videomode_digital",ma.group(2));
				}
				
			}
	
			// 繰り返し
			ma = Pattern.compile("^\\d").matcher(r.getRec_pattern());
			if (ma.find()) { 
				newdat.put("maiyoubi_type","0");
				try {
					newdat.put("date", URLEncoder.encode(r.getRec_pattern(),thisEncoding));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}
			else {
				newdat.put("maiyoubi_type", "1");
				int i = 1;
				for ( String s : RPTPTN ) {
					if ( s.equals(r.getRec_pattern()) == true ) {
						newdat.put("date", String.valueOf(i));
					}
					i++;
				}
			}

			// 開始・終了時刻
			newdat.put("start_hour", r.getAhh());
			newdat.put("start_minute", r.getAmm());
			newdat.put("end_hour", r.getZhh());
			newdat.put("end_minute", r.getZmm());
	
			// 自動チャプター関連
			newdat.put("bAutoChapter"		, text2value(xchapter, r.getRec_xchapter()));
			newdat.put("MagicChapter"		, text2value(mschapter, r.getRec_mschapter()));
			newdat.put("CM_Chapter"			, text2value(mvchapter, r.getRec_mvchapter()));
			
			// 記録先
			newdat.put("disc", text2value(device, r.getRec_device()));
			
			String val;
			
			val = text2value(aspect, r.getRec_aspect());
			newdat.put("edge_left", (val!=null)?(val):("0"));
			
			val = text2value(lvoice, r.getRec_lvoice());
			newdat.put("lVoice", (val!=null)?(val):("1"));
			
			val = text2value(bvperf, r.getRec_bvperf());
			newdat.put("rec_priority", (val!=null)?(val):("150"));
			
			val = text2value(autodel, r.getRec_autodel());
			newdat.put("auto_delete", (val!=null)?(val):("0"));
			
			// 追加
			newdat.put("start_form"			, "");
			//newdat.put("rec_priority"		, "150");
			newdat.put("title_link"			, "0");
			//newdat.put("auto_delete"		, "0");
			newdat.put("video_es"			, "00");
			newdat.put("audio_es"			, "10");
			//newdat.put("lVoice"		, "1");
			//newdat.put("edge_left"			, "0");		// 録画のりしろ
			//newdat.put("MagicChapter"		, "0");
			//newdat.put("MagicChapter"		, "0");
			//newdat.put("CM_Chapter"			, "0");
			newdat.put("add_ch_text"		, "");
			newdat.put("add_ch_value"		, "");
			//newdat.put("sport_ext_submit"	, "undefined");		// 本体からの予約の状態を保持する(>>208.)
			//newdat.put("title_link_submit"	, "undefined");	// 本体からの予約の状態を保持する(>>208.)
			newdat.put("end_form"			, "0");
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return(newdat);
	}
	
	
	
	/*
	 * 録画設定の解読
	 */
	private void setSettingVrate(ArrayList<TextValueSet> tvs)
	{
		tvs.clear();
		TextValueSet t = null;
		
		t = new TextValueSet();
		t.setText("[DR]");
		t.setValue("128:6");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText("[AVC] AF 12.0");
		t.setValue("2:8");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AN 8.0");
		t.setValue("2:9");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AE 2.0");
		t.setValue("2:10");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText("[AVC] AT 4.7GB");
		t.setValue("2:4");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 8.5GB");
		t.setValue("2:7");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 9.4GB");
		t.setValue("2:5");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 25GB");
		t.setValue("2:11");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[AVC] AT 50GB");
		t.setValue("2:12");
		tvs.add(t);
		
		for (int br=1400; br<=17000; ) {
			t = new TextValueSet();
			t.setText(String.format("[AVC] %d.%d", (br-br%1000)/1000, (br%1000)/100));
			t.setValue("2:"+String.valueOf(br));
			tvs.add(t);
			if (br < 10000) {
				br += 200;
			}
			else {
				br += 500;
			}
		}
		t = new TextValueSet();
		t.setText("[AVC] 高レート節約");
		t.setValue("2:16");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText("[VR] SP4.4/4.6");
		t.setValue("1:1");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[VR] LP2.0/2.2");
		t.setValue("1:2");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText("[VR] AT 4.7GB");
		t.setValue("1:4");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[VR] AT 8.5GB");
		t.setValue("1:7");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[VR] AT 9.4GB");
		t.setValue("1:5");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText("[VR] 1.0");
		t.setValue("1:1000");
		tvs.add(t);
		t = new TextValueSet();
		t.setText("[VR] 1.4");
		t.setValue("1:1400");
		tvs.add(t);
		for (int br=2000; br<=9200; br+=200) {
			t = new TextValueSet();
			t.setText(String.format("[VR] %d.%d", (br-br%1000)/1000, (br%1000)/100));
			t.setValue("1:"+String.valueOf(br));
			tvs.add(t);
		}
		t = new TextValueSet();
		t.setText("[VR] 高レート節約");
		t.setValue("1:13");
		tvs.add(t);
	}
	
	private void setSettingFolder(ArrayList<TextValueSet> folder, String res) {
		folder.clear();
		LinkedHashMap<String,String> folderKey = new LinkedHashMap<String, String>();
		folderKey.put("hdd",ITEM_MEDIA_HDD);
		folderKey.put("dvd",ITEM_MEDIA_USB);
		for (String typ : folderKey.keySet()) {
			String txtkey = typ+"_folder_text";
			String valkey = typ+"_folder_value";
			Matcher mc = Pattern.compile("var "+txtkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			Matcher md = Pattern.compile("var "+valkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if ( mc.find() && md.find() ) {
				Matcher me = Pattern.compile("\"(.+?)\",?").matcher(mc.group(1));
				Matcher mf = Pattern.compile("\"(.+?)\",?").matcher(md.group(1));
				while (me.find() && mf.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(folderKey.get(typ)+me.group(1));
					t.setValue(mf.group(1));
					folder.add(t);
				}
			}
		}
	}
	
	protected void setSettingEncoder(ArrayList<TextValueSet> tvs, String res)
	{
		tvs.clear();
		
		Matcher mb = Pattern.compile("var double_encode_flg = (\\d+?);").matcher(res);
		while (mb.find()) {
			Matcher mc = Pattern.compile("\\n\\s*?switch \\( double_encode_flg \\) \\{([\\s\\S]+?default:)").matcher(res);
			if (mc.find()) {
				Matcher md = Pattern.compile("(case "+mb.group(1)+":[\\s\\S]+?break;)").matcher(mc.group(1));
				if (md.find()) {
					{
						Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?toru_(.+?)\\.gif").matcher(md.group(1));
						while (me.find()) {
							TextValueSet t = new TextValueSet();
							t.setText(me.group(2).replaceFirst("^TS","DR"));
							t.setValue(me.group(1));
							tvs.add(t);
						}
						if ( tvs.size() > 0 ) {
							System.out.println(DBGID+" エンコーダ情報から2012/10/18以前のファームと判断しました");
							return;
						}
					}
					
					// 2012/10/18ファームのBZ700対応
					{
						Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?act_(.+?)\\.gif").matcher(md.group(1));
						while (me.find()) {
							TextValueSet t = new TextValueSet();
							t.setText(me.group(2));
							t.setValue(me.group(1));
							tvs.add(t);
						}
						if ( tvs.size() > 0 ) {
							System.out.println(DBGID+" エンコーダ情報から2012/10/18以降のファームと判断しました");
							return;
						}
					}
					
					System.out.println(ERRID+" エンコーダ情報を解析できませんでした。未対応のファームと思われます。ダンプとログを添付して開発元にお問い合わせください。");
				}
			}
		}
	}
	
	/**
	 * エンコーダ情報がHTMLからとれなけば固定値を使う
	 * @see #setSettingEncoder(ArrayList, String)
	 */
	protected void setSettingEncoder(ArrayList<TextValueSet> tvs)
	{
		tvs.clear();
		TextValueSet t = null;
		
		t = new TextValueSet();
		t.setText(ITEM_ENCODER_R1);
		t.setValue(VALUE_ENCODER_R1);
		tvs.add(t);
		t = new TextValueSet();
		t.setText(ITEM_ENCODER_R2);
		t.setValue(VALUE_ENCODER_R2);
		tvs.add(t);
	}
	
	private void setSettingChCodeValue(ArrayList<TextValueSet> tvsvalue, ArrayList<TextValueSet> tvstype, String res) {
		tvsvalue.clear();
		tvstype.clear();
		for ( String typ : new String[] { "uvd","bsd","csd","l1","l2","l3" } ) {
			String txtkey = typ+"_ch_text";
			String valkey = typ+"_ch_value";
			Matcher mc = Pattern.compile("var "+txtkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			Matcher md = Pattern.compile("var "+valkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if ( mc.find() && md.find() ) {
				Matcher me = Pattern.compile("\"(.+?)\",?").matcher(mc.group(1));
				Matcher mf = Pattern.compile("\"([^\"]+?)\",").matcher(md.group(1));
				while ( me.find() && mf.find() ) {
					TextValueSet t = new TextValueSet();
					t.setText(me.group(1));
					t.setValue(mf.group(1));
					tvsvalue.add(t);
					
					TextValueSet x = new TextValueSet();
					x.setText(mf.group(1));
					x.setValue(typ);
					tvstype.add(x);
				}
			}
		}
	}
	
	private void setSettingGenre(ArrayList<TextValueSet> tvs)
	{
		tvs.clear();
		TextValueSet t = null;
		
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.MOVIE.toString());
		t.setValue("0");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.MUSIC.toString());
		t.setValue("1");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.DORAMA.toString());
		t.setValue("2");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.ANIME.toString());
		t.setValue("3");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.SPORTS.toString());
		t.setValue("4");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.DOCUMENTARY.toString());
		t.setValue("5");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.THEATER.toString());
		t.setValue("6");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.HOBBY.toString());
		t.setValue("7");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.VARIETY.toString());
		t.setValue("8");
		tvs.add(t);
		t = new TextValueSet();
		t.setText(TVProgram.ProgGenre.NOGENRE.toString());
		t.setValue("10");
		tvs.add(t);
	}
	
	private void setSettingEtc(ArrayList<TextValueSet> tvs, String key, int typ, String res) {
		tvs.clear();
		String valExpr = "(\\d+),?";
		if (typ == 1) {
			valExpr = "\"(.+?)\",?";
		}
		Matcher mc = Pattern.compile("var "+key+"_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
		Matcher md = Pattern.compile("var "+key+"_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
		if (mc.find() && md.find()) {
			Matcher me = Pattern.compile("\"(.+?)\",?").matcher(mc.group(1));
			Matcher mf = Pattern.compile(valExpr).matcher(md.group(1));
			while (me.find() && mf.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(me.group(1));
				t.setValue(mf.group(1));
				tvs.add(t);
			}
		}
	}
	
	//
	protected void translateAttributeTuner(ReserveList entry) {
		if (entry.getTuner().startsWith("TS")) {
			entry.setTuner(entry.getTuner().replaceFirst("^TS", "DR"));
		}
	}
	
	//
	protected String text2valueEncoderSP(ArrayList<TextValueSet> tvs, String text, String vrate) {
		return text2value(tvs, text);
	}
}
