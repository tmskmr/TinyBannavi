package tainavi.pluginrec;

import tainavi.*;
import tainavi.TVProgram.ProgGenre;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 *
 */

public class PlugIn_RecRD_T450 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_T450 clone() {
		return (PlugIn_RecRD_T450) super.clone();
	}

	private static final String thisEncoding = "MS932";
	private static final String utf8Encoding = "UTF-8";

	/* 必須コード  - ここから */

	// 種族の特性
	@Override
	public String getRecorderId() { return "REGZA DBR-T450"; }
	@Override
	public RecType getType() { return RecType.RECORDER; }

	@Override
	public String getLabel_MsChapter() { return "持出用録画"; }
	@Override
	public String getLabel_MvChapter() { return "持出用品質"; }

	@Override
	public String getChDatHelp() { return
			"「レコーダの放送局名」はネットdeナビの録画予約一覧で表示される「CH」の欄の値を設定してください。\n"+
			"予約一覧取得が正常に完了していれば設定候補がコンボボックスで選択できるようになります。"
			;
	}

	// 個体の特性
	private GetRDStatus gs = new GetRDStatus();
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String rsvedFile = "";

	protected void setErrmsg(String s) { errmsg = s; }
	private String errmsg = "";

	/*
	 * 定数
	 */

	private final String MSGID = "["+getRecorderId()+"] ";
	private final String ERRID = "[ERROR]"+MSGID;
	private final String DBGID = "[DEBUG]"+MSGID;

	//
	private static final String ITEM_VIDEO_TYPE_DR  = "[DR]";
	private static final String ITEM_VIDEO_TYPE_VR  = "[VR] ";
	private static final String ITEM_VIDEO_TYPE_AVC = "[AVC] ";

	private static final String ITEM_APPS_RSV_TYPE_NOMAL = "通常のみ";
	private static final String ITEM_APPS_RSV_TYPE_BRING = "持出のみ";
	private static final String ITEM_APPS_RSV_TYPE_BOTH  = "通常＋持出";

	// chvalueを使っていいよ
	@Override
	public boolean isChValueAvailable() { return true; }
	// CHコードは入力しなくていい
	@Override
	public boolean isChCodeNeeded() { return false; }

	// 公開メソッド

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public PlugIn_RecRD_T450() {
		super();
		this.setTunerNum(3);
	}

	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/

	/*
	 *
	 */
	@Override
	public ChannelCode getChCode() {
		return cc;
	}

	/*
	 * チャンネルリモコン機能
	 */
	@Override
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

	/*
	 *	レコーダーから予約一覧を取得する
	 */
	public boolean GetRdReserve(boolean force)
	{
		System.out.println("レコーダから予約一覧を取得します("+force+")： "+getRecorderId()+"("+getIPAddr()+":"+getPortNo()+")");

		errmsg = "";

		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		String folderTFile = "env/folders."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String deviceTFile = "env/device."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String mvChapterTFile = "env/mvchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String chTypeTFile = "env/chtype."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		// ハードコーディング（録画設定ほか）
		setSettingVrate(vrate);
		setSettingEncoder(encoder);
		setSettingApps(mschapter);

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（録画設定ほか）
			//arate = TVSload(arateTFile);
			folder = TVSload(folderTFile);
			device = TVSload(deviceTFile);
			mvchapter = TVSload(mvChapterTFile);
			chvalue = TVSload(chValueTFile);
			chtype = TVSload(chTypeTFile);

			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));

			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (folder.size()>0 && device.size()>0 &&
					mvchapter.size()>0 &&
					chvalue.size()>0 && chtype.size()>0) {
				return(true);
			}
		}


		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header="";
		String response="";
		{
			reportProgress(MSGID+"処理IDを取得します(1/3).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
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
			header = d[0];
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

		boolean isfault = false;

		// (1)録画設定の取得
		{
			// ハング防止のおまじない
			reportProgress(MSGID+"録画設定を取得します(3/3).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);

			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
			String hdr = d[0];
			String res = d[1];

			if (res == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}

			//Matcher mb = null;

			// 正常取得チェック
			if ( ! res.matches("[\\s\\S]*var hdd_folder_text[\\s\\S]*")) {
				errmsg = "レコーダーからの情報取得に失敗しました（録画設定）";
				return false;
			}

			ArrayList<TextValueSet> tvsa = null;

			// (1-6)記録先デバイス
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"media",0,res);
			if (tvsa.size() > 0) {
				TVSsave(device=tvsa, deviceTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 記録先デバイスが取得できません");
				isfault = true;
			}

			// (1-Y) 持出用品質
			tvsa = new ArrayList<TextValueSet>();
			setSettingEtc(tvsa,"apps_vrate",0,res);
			if ( tvsa.size() > 0 ) {
				TVSsave(mvchapter=tvsa, mvChapterTFile);
			}
			else {
				System.err.println(errmsg = ERRID+"【致命的エラー】 持出用品質が取得できません");
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
		}

		ArrayList<TextValueSet> tvsa = new ArrayList<TextValueSet>();
		for ( TextValueSet devset : device ) {
			String dev = devset.getText();

			reportProgress(MSGID + "フォルダ一覧を取得します("+dev+").");
			String[] d = reqGET("http://" + getIPAddr() + ":" + getPortNo() + "/reserve/" + idx + "/folderlist.htm?"+dev, null, utf8Encoding);
			String hdr = d[0];
			String res = d[1];

			if (res == null) {
				errmsg = ERRID + "レコーダーが反応しません";
				continue;
			}

			ArrayList<String> text = new ArrayList<String>();
			ArrayList<String> value = new ArrayList<String>();
			Matcher mdev = Pattern.compile("var\\s+folder_list_text\\s+=\\s+new\\s+Array\\((.+?)\\);.+?var\\s+folder_list_value\\s+=\\s+new\\s+Array\\((.+?)\\);", Pattern.DOTALL).matcher(res);
			if ( mdev.find() && mdev.groupCount() == 2 ) {
				String[] textTemp = mdev.group(1).split("[\\n\\r]+");
				for ( String ts : textTemp ) {
					String t = ts.trim();
					if ( t.length() > 0 ) {
						text.add(t);
						System.out.println(t);
					}
				}
				String[] valueTemp = mdev.group(2).split("[\\n\\r]+");
				for ( String vs : valueTemp ) {
					String v = vs.trim();
					if ( v.length() > 0 ) {
						value.add(v);
					}
				}

				if ( text.size() == value.size() ) {
					for ( int i=0; i < text.size(); i ++ ) {
						TextValueSet tvs = new TextValueSet();
						tvs.setText(String.format("[%s] %s", dev, text.get(i)));
						tvs.setValue(value.get(i));
						tvsa.add(tvs);
					}
				}
			}
		}
		TVSsave(folder = tvsa, folderTFile);

		// 予約一覧データの分析
		setReserves(decodeReservedList(response));

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		// 取得した情報の表示
		System.out.println("---Reserved List Start---");
		for ( int i = 0; i<getReserves().size(); i++ ) {
			// 詳細情報の取得
			ReserveList e = getReserves().get(i);
			System.out.println(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s(%s)\t%s\t%s",
					(i+1),
					e.getId(),
					e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(), e.getZmm(), e.getRec_min(),
					e.getRec_mschapter(), ((e.getAppsRsv())?(e.getRec_mvchapter()):(e.getRec_mode())),
					e.getTitle(), e.getTitlePop(),
					e.getChannel(), e.getCh_name()));
		}
		System.out.println("---Reserved List End---");

		return( ! isfault);
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

			// (1-6)記録先デバイス [一覧から取得]

			// (1-3)フォルダ
			mx = Pattern.compile("var folder_current = \"(\\d+?)\";").matcher(d[1]);
			if ( ! mx.find()) {
				reportProgress("レコーダーからの戻り値が不正です(フォルダ)");
				continue;
			}
			{
				ArrayList<TextValueSet> currentFolderList = new ArrayList<TextValueSet>();
				final String deviceId = String.format("[%s] ", entry.getRec_device());
				for ( TextValueSet f : folder ) {
					if ( f.getText().startsWith(deviceId) ) {
						currentFolderList.add(f);
					}
				}
				int index = Integer.parseInt(mx.group(1));
				System.out.println(currentFolderList.size()+", "+index);
				if ( currentFolderList.size() <= index ) {
					reportProgress("レコーダーからの戻り値が不正です(フォルダリスト)");
					continue;
				}
				entry.setRec_folder(currentFolderList.get(index).getText());
			}

			// (1-Y) 持出用録画関連
			{
				mx = Pattern.compile("var apps_rsv_use_current\\s+?=\\s+?(\\d+?);").matcher(d[1]);
				if ( ! mx.find()) {
					reportProgress("レコーダーからの戻り値が不正です(持出用録画有無)");
					continue;
				}
				int apps_rsv_use = Integer.valueOf(mx.group(1));
				if ( apps_rsv_use != 0 ) {
					mx = Pattern.compile("var apps_rsv_type_current\\s+?=\\s+?(\\d+?);").matcher(d[1]);
					if ( ! mx.find()) {
						reportProgress("レコーダーからの戻り値が不正です(持出用録画方法)");
						continue;
					}
					apps_rsv_use = Integer.valueOf(mx.group(1)) + 1;
				}
				entry.setRec_mschapter(mschapter.get(apps_rsv_use).getText());

				mx = Pattern.compile("var apps_vrate_current\\s+?=\\s+?(\\d+?);").matcher(d[1]);
				if ( ! mx.find()) {
					reportProgress("レコーダーからの戻り値が不正です(持出用録画品質)");
					continue;
				}
				int apps_vrate = Integer.valueOf(mx.group(1));

				entry.setRec_mvchapter(mvchapter.get(apps_vrate).getText());
			}

			// (1-8)チャンネル [一覧から取得]
		}

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		return(true);
	}

	/*
	 *	予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r)
	{
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		errmsg = "";

		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}


		/*
		 * 予約情報の整理
		 */

		// 予約パターンID・次回予定日
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));

		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));

		// 開始日時・終了日時
		getStartEndDateTime(r);


		/*
		 * 予約実行
		 */

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("処理IDを取得します(1/7).");
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
			reportProgress("予約実行前の予約一覧を取得します(2/7).");
			idx = ma.group(1);
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];

			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		ArrayList<String[]> oldids = getIds(response);

		// 予約エントリー数を取得する
		int RsvCnt = 0;
		ma = Pattern.compile("RsvCnt\\s*=\\s*(\\d+?);").matcher(response);
		if (ma.find()) {
			RsvCnt = Integer.valueOf(ma.group(1));
		}

		// RDに新規登録要請
		{
			// ハング防止のおまじない
			reportProgress("予約ページを開きます(3/7).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);

			reportProgress("予約ページを開きます(4/7).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}

		// POSTデータを変換する
		HashMap<String, String> pdat = modPostdata(r);

		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{
			reportProgress("予約を実行します(5/7).");
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

		// 予約ID番号を取得（キャッシュに存在しない番号が新番号）
		{
			reportProgress("処理IDを取得します(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("予約実行後の予約一覧を取得します(7/7).");
			//
			String param = "/reserve.htm";
			if ( getDebug() ) {
				param = "/reserve2.htm";
			}
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+param,null);
			//
			header = d[0];
			response = d[1];
		}
		ArrayList<String[]> newids = getIds(response);

		// 予約IDの取得
		ArrayList<String[]> rids = new ArrayList<String[]>();
		for ( String[] nid : newids ) {
			String[] rid = nid;
			for ( String[] oid : oldids ) {
				if ( nid[0].equals(oid[0]) ) {
					rid = null;
					break;
				}
			}
			if ( rid != null ) {
				rids.add(rid);
			}
		}
		System.out.println("+追加された予約IDの数： "+rids.size());
		if ( rids.size() > 0 ) {
			for ( String[] rid : rids ) {
				System.out.println("+-予約ID: "+rid[0]);
				ReserveList nr = r.clone();
				if ( rid[1].equals("0") && (nr.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_NOMAL)||nr.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_BOTH)) ) {
					// 通常
					nr.setAppsRsv(false);
					nr.setRec_mschapter(ITEM_APPS_RSV_TYPE_NOMAL);
				}
				else if ( rid[1].equals("1") && (nr.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_NOMAL)||nr.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_BOTH)) ) {
					// 持出
					nr.setAppsRsv(true);
					nr.setRec_mschapter(ITEM_APPS_RSV_TYPE_BRING);
				}
				else {
					errmsg = "【警告】予約IDと予約情報が一致しません。";
					continue;
				}
				nr.setId(rid[0]);
				getReserves().add(nr);
			}
		}
		else {
			errmsg = "【警告】予約IDが取得できませんでした。";
		}


		// 予約リストをキャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);


		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}


	/*
	 *	予約を更新する
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {

		System.out.println("Run: UpdateRdEntry()");

		errmsg = "";

		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}

		if ( ! o.getRec_mschapter().equals(r.getRec_mschapter()) ) {
			errmsg = String.format("【警告】持出用録画の設定は変更できません。(%s→%s)",o.getRec_mschapter(),r.getRec_mschapter()) ;
			System.out.println(errmsg);
			return(false);
		}

		/*
		 * 予約情報の整理
		 */

		// 予約パターンID・次回予定日
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		r.setRec_nextdate(CommonUtils.getNextDate(r));

		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));

		// 開始日時・終了日時
		getStartEndDateTime(r);


		/*
		 * 予約実行
		 */

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

		// 更新準備
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("処理IDを取得します(1/5).");
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
			reportProgress("更新対象の予約を確認します(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];

			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
		}
		int lineno = getLineNo(response, r.getId());

		// RDに更新要請
		{
			// ハング防止のおまじない
			reportProgress("予約ページを開きます(3/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?"+r.getId()+"&"+lineno,null);

			reportProgress("予約ページを開きます(4/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}

		// POSTデータを変換する
		HashMap<String, String> pdat = modPostdata(r);

		// RDへの情報作成
		String pstr = joinPoststr(pdat);

		// RDへ情報送信
		{
			reportProgress("更新を実行します(5/5).");
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/entry.htm", pstr, null);
			header = d[0];
			response = d[1];

			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(false);
			}
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

		// 情報置き換え
		getReserves().remove(o);
		getReserves().add(r);

		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		return(true);
	}

	/*
	 *	予約を削除する
	 */
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
		String header;
		String response;
		{
			reportProgress("処理IDを取得します(1/5).");
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
			reportProgress("削除対象の予約を確認します(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
		}
		int lineno = getLineNo(response, rx.getId());

		// RDに削除要請
		{
			// ハング防止のおまじない
			reportProgress("予約ページを開きます(3/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?"+rx.getId()+"&"+lineno,null);

			reportProgress("予約ページを開きます(4/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
		}
		{
			reportProgress("削除を実行します(5/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/delete.htm", null);
			header = d[0];
			response = d[1];

			if ( response == null ) {
				errmsg = "レコーダーが反応しません。";
				return(null);
			}
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

	/*
	 *
	 */
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}




	/* ここまで */






	/* 個別コード－ここから最後まで */

	/*
	 * 非公開メソッド
	 */

	private String joinPoststr(HashMap<String, String> pdat)
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
			"date1",
			"start_hour",
			"start_minute",
			"end_hour",
			"end_minute",
			"apps_rsv_use",		// new
			"apps_rsv_type",	// new
			"apps_vrate",		// new
			"disc",
			"vrate",
			"amode",
			"videotype_digital",
			"videomode_digital",
			"folder",
			"auto_delete",
			"dvdr",
			"lVoice",
			"edge_left",
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

	private HashMap<String, String> modPostdata(ReserveList r) {

		HashMap<String, String> newdat = new HashMap<String, String>();
		try {
			// 実行するよ
			newdat.put("bExec", (r.getExec())?("ON"):("OFF"));
			if (r.getUpdateOnlyExec()) {
				return(newdat);
			}

			// 予約名・予約詳細
			if ( r.getAutocomplete() ) {
				newdat.put("title_name", "");
				//newdat.put("detail", "");
			}
			else {
				try {
					newdat.put("title_name", URLEncoder.encode(CommonUtils.substringrb(r.getTitle(),86),thisEncoding));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}

				// 予約詳細
				/*
				try {
					newdat.put("detail", URLEncoder.encode(CommonUtils.substringrb(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")),75*5), thisEncoding));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				*/
			}

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

			try {
				String ech = URLEncoder.encode(channel,thisEncoding);
				newdat.put("channel_list", ech);
				newdat.put("channel_no", ech);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			// 開始・終了日時
			Matcher ma = Pattern.compile("^\\d").matcher(r.getRec_pattern());
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
				int i = 0;
				for ( String s : RPTPTN ) {
					if ( s.equals(r.getRec_pattern()) == true ) {
						break;
					}
					i++;
				}
				if ( i <= RPTPTN_ID_SAT ) {
					newdat.put("date1", String.valueOf(0x0001 << ((i + 6) % 7)));
				}
				else if ( i >= RPTPTN_ID_MON2FRI &&  i <= RPTPTN_ID_EVERYDAY ) {
					newdat.put("date1", String.valueOf((0x0001 << (i - 3))-1));
				}
			}

			newdat.put("start_hour", r.getAhh());
			newdat.put("start_minute", r.getAmm());
			newdat.put("end_hour", r.getZhh());
			newdat.put("end_minute", r.getZmm());

			String val;

			/*
			 *  持出用録画のありなしが影響する項目
			 */

			if ( r.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_NOMAL) || r.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_BOTH) ) {
				// 保存先
				try {
					newdat.put("folder", URLEncoder.encode(text2value(folder, r.getRec_folder()),thisEncoding));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				// 記録先
				newdat.put("disc", text2value(device, r.getRec_device()));

				// 録画レート
				ma = Pattern.compile("^(\\d+?):(.*?)$").matcher(text2value(vrate, r.getRec_mode()));
				if (ma.find()) {
					if (ma.group(1).equals("1")) {
						// VR
						newdat.put("videotype_digital",ma.group(1));
						newdat.put("videomode_digital",ma.group(2));
					}
					else if (ma.group(1).equals("2")) {
						// AVC
						newdat.put("videotype_digital",ma.group(1));
						newdat.put("videomode_digital",ma.group(2));
					}
					else {
						// DR
						newdat.put("videotype_digital",ma.group(1));
					}
				}
			}

			// 持出用録画
			if ( r.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_BRING) || r.getRec_mschapter().equals(ITEM_APPS_RSV_TYPE_BOTH) ) {
				newdat.put("apps_rsv_use", "1");
				newdat.put("apps_rsv_type", text2value(mschapter,r.getRec_mschapter()));
				newdat.put("apps_vrate", text2value(mvchapter,r.getRec_mvchapter()));
			}
			else {
				//newdat.put("apps_rsv_use", "0");
			}


			// 追加
			newdat.put("start_form"			, "");
			//newdat.put("dtv_sid"			, "0");		// ？
			//newdat.put("dtv_nid"			, "0");		// ？
			//newdat.put("net_link"			, "0");		// ？
			newdat.put("add_ch_text"		, "");
			newdat.put("add_ch_value"		, "");
			//newdat.put("sport_ext_submit"	, "undefined");		// 本体から受信した予約状態を上書きしない(>>208.)
			//newdat.put("title_link_submit"	, "undefined");	// 本体から受信した予約状態を上書きしない(>>208.)
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
	protected void setSettingVrate(ArrayList<TextValueSet> vrate)
	{
		vrate.clear();
		TextValueSet t = null;

		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_DR);
		t.setValue("128:");
		vrate.add(t);

		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AF");
		t.setValue("2:8");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AN");
		t.setValue("2:9");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AS");
		t.setValue("2:18");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AL");
		t.setValue("2:19");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AE");
		t.setValue("2:10");
		vrate.add(t);

		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AT 4.7GB");
		t.setValue("2:22");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AT 8.5GB");
		t.setValue("2:23");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AT 25GB");
		t.setValue("2:11");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_AVC+"AT 50GB");
		t.setValue("2:12");
		vrate.add(t);

		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_VR+"XP");
		t.setValue("1:20");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_VR+"SP");
		t.setValue("1:1");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_VR+"LP");
		t.setValue("1:2");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_VR+"EP");
		t.setValue("1:21");
		vrate.add(t);
		t = new TextValueSet();
		t.setText(ITEM_VIDEO_TYPE_VR+"AT 4.7GB");
		t.setValue("1:4");
		vrate.add(t);
	}

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

	private void setSettingChCodeValue(ArrayList<TextValueSet> chvalue, ArrayList<TextValueSet> chtype, String res) {
		chvalue.clear();
		chtype.clear();
		for ( String typ : new String[] { "uvd","bsd","csd","l1","l2","l3" } ) {
			String txtkey = typ+"_ch_text";
			String valkey = typ+"_ch_value";
			Matcher mc = Pattern.compile("var "+txtkey+"\\s*= new Array\\((.+?)\\);",Pattern.DOTALL).matcher(res);
			Matcher md = Pattern.compile("var "+valkey+"\\s*= new Array\\((.+?)\\);",Pattern.DOTALL).matcher(res);
			if ( mc.find() && md.find() ) {
				Matcher me = Pattern.compile("\"(.+?)\",?").matcher(mc.group(1));
				Matcher mf = Pattern.compile("\"([^\"]+?)\",").matcher(md.group(1));
				//System.out.println(txtkey+" "+mc.group(1));
				//System.out.println(valkey+" "+md.group(1));
				while ( me.find() && mf.find() ) {
					TextValueSet t = new TextValueSet();
					t.setText(me.group(1));
					t.setValue(mf.group(1));
					chvalue.add(t);

					TextValueSet x = new TextValueSet();
					x.setText(mf.group(1));
					x.setValue(typ);
					chtype.add(x);
				}
			}
		}
	}

	private void setSettingApps(ArrayList<TextValueSet> tvs) {
		tvs.clear();
		TextValueSet t = null;
		
		t = new TextValueSet();
		t.setText(ITEM_APPS_RSV_TYPE_NOMAL);
		t.setValue("");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText(ITEM_APPS_RSV_TYPE_BRING);
		t.setValue("0");
		tvs.add(t);
		
		t = new TextValueSet();
		t.setText(ITEM_APPS_RSV_TYPE_BOTH);
		t.setValue("1");
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
	
	
	
	/***
	 *  RDデジタル系の予約一覧の解読
	 */
	protected ArrayList<ReserveList> decodeReservedList(String response) {
		
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		Matcher ma = Pattern.compile("(c1\\[\\d+?\\]=[\\s\\S]+?\";)\\n").matcher(response);
		while ( ma.find() ) {
			
			// 個々のデータを取り出す
			ReserveList entry = new ReserveList();
			
			Matcher mb = null;
				
			String[] d = new String[18];
			for ( int n=0; n < d.length; n++ ) {
				d[n] = "";
			}
			mb = Pattern.compile("c(\\d+?)\\[\\d+?\\]=\"(.*?)\";").matcher(ma.group(1));
			while ( mb.find() ) {
				int n = Integer.valueOf(mb.group(1));
				if ( n >= d.length ) {
					continue;
				}
				
				d[n] = mb.group(2);
				//System.out.println(n+") "+d[n]);
			}
			
			// 予約ID
			entry.setId(d[1]);
			
			// 実行ON/OFF
			if (d[2].equals("2")) {
				entry.setExec(false);
			}
			
			// 予約名
			String title = CommonUtils.unEscape(d[3]).replaceAll("<BR>","");
			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));

			// チャンネル
			//entry.setCh_name(getChCode().getCH_NO2NAME(d[4]));	// 機種固有領域に移動
			entry.setChannel(d[5]);
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
			
			// 開始・終了日時
			entry.setRec_pattern(d[6]);
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[7]+"-"+d[8]);
			if (mb.find()) {
				entry.setAhh(mb.group(1));
				entry.setAmm(mb.group(2));
				entry.setZhh(mb.group(3));
				entry.setZmm(mb.group(4));
			}
			entry.setRec_nextdate(CommonUtils.getNextDate(entry));
			//entry.setRec_nextdate(getNextDate(entry.getRec_pattern(), entry.getZhh()+":"+entry.getZmm()));
			entry.setRec_min(CommonUtils.getRecMin(entry.getAhh(), entry.getAmm(), entry.getZhh(), entry.getZmm()));
			getStartEndDateTime(entry);
			
			// 記録先デバイス
			entry.setRec_device(d[9]);
			
			// 画質・音質（取得できない）
			//entry.setRec_mode(d[10]);
			//entry.setRec_audio(d[11]);
			
			// 番組追跡
			if ( d[15].matches("^(1|2|3|4)$") ) {
				entry.setPursues(true);
			}
			
			// 持出用フラグ
			if (d[16].equals("1")) {
				entry.setAppsRsv(true);
				entry.setRec_mschapter(ITEM_APPS_RSV_TYPE_BRING);
			}
			else {
				entry.setAppsRsv(false);
				entry.setRec_mschapter(ITEM_APPS_RSV_TYPE_NOMAL);
			}
			
			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributes(entry, getReserves());
			
			// 予約情報を保存
			newReserveList.add(entry);
		}
		return(newReserveList);
	}
	
	// レコーダーから取得できない情報は直接コピー
	@Override
	protected void copyAttributes(ReserveList entry, ArrayList<ReserveList> reserves) {
		for ( ReserveList e : reserves ) {
			if ( e.getId().equals(entry.getId()) ) {
				// 鯛ナビの内部フラグ
				entry.setAutocomplete(e.getAutocomplete());
				
				// 予約一覧からは取得できない情報
				entry.setDetail(e.getDetail());
				entry.setRec_genre(e.getRec_genre());
				//entry.setRec_device(e.getRec_device());
				entry.setRec_folder(e.getRec_folder());
				//entry.setRec_mschapter(e.getRec_mschapter());
				entry.setRec_mvchapter(e.getRec_mvchapter());
				//
				entry.setRec_aspect(e.getRec_aspect());
				entry.setRec_mode(e.getRec_mode());

				return;
			}
		}
	}
	
	private ArrayList<String[]> getIds(String response) {
		ArrayList<String[]> ids = new ArrayList<String[]>();
		Matcher ma = Pattern.compile("c1\\[\\d+?\\]=\"(\\d+?)\";.+?c16\\[\\d+?\\]=\"(\\d+?)\";").matcher(response);
		while (ma.find()) {
			String data[] = { ma.group(1), ma.group(2) };
			ids.add(data);
		}
		return(ids);
	}
	
	private int getLineNo(String response, String id) {
		Matcher ma = Pattern.compile("c1\\[(\\d+?)\\]=\""+id+"\";").matcher(response);
		if ( ma.find() ) {
			return(Integer.valueOf(ma.group(1))+1);
		}
		return(0);
	}
}
