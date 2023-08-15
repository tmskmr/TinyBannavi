package tainavi.pluginrec;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.TraceProgram;
import tainavi.b64;
import tainavi.HDDRecorder.RecType;

/*
 * 
 */

public class PlugIn_RecDIGA_DMR_BW200 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecDIGA_DMR_BW200 clone() {
		return (PlugIn_RecDIGA_DMR_BW200) super.clone();
	}
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "DIGA DMR-BW200"; }
	public RecType getType() { return RecType.RECORDER; }

	// 個体の特性
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String rsvedFile = "";
	private String vrateTFile = "";
	private String chValueTFile = "";
	
	private String errmsg = "";

	

	// 公開メソッド
	
	/*
	 * 
	 */
	public ChannelCode getChCode() {
		return cc;
	}
	
	/*
	 * 
	 */
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
				while ( doLogin() == false ) {
					System.out.println("Wait for retry ...");
					try {
						Thread.sleep(5000);
					}
					catch (Exception e) {
						// 例外
					}
				}
			}
			{
				System.out.println("change channel.(2/2)");
				String url = "http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_chtune.cgi";
				String pstr = "cCHSRC="+ma.group(2)+"&cCHNUM="+ma.group(1)+"&cCMD_SET.x=49&cCMD_SET.y=8";
				reqPOST(url, pstr, null);
			}
		}
		
		return true;
	}

	/*
	 * 
	 */
	@Override
	public void wakeup() {
		poweronoff(true);
	}
	//
	@Override
	public void shutdown() {
		poweronoff(false);
	}
	
	/*
	 *	レコーダーから予約一覧を取得する 
	 */
	public boolean GetRdReserve(boolean force)
	{
		System.out.println("Run: GetRdReserve("+force+")");
		
		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		vrateTFile = "env/videorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		// チューナー数は自動生成
		{
			encoder.clear();
			if ( getTunerNum() >= 2 ) {
				for ( int i=1; i<=getTunerNum(); i++ ) {
					TextValueSet t = new TextValueSet();
					t.setText("D"+i);
					t.setValue("D"+i);
					encoder.add(t);
				}
			}
		}

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			
			// キャッシュから読み出し（録画設定）
			vrate = TVSload(vrateTFile);
			if ( vrate.size() > 0 ) {
				System.out.println("+video rate="+vrateTFile);
			}
			
			// チャンネルコードバリュー
			chvalue = TVSload(chValueTFile);
			if ( chvalue.size() > 0 ) {
				System.out.println("+chvalue="+chValueTFile);
			}
			
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && chvalue.size()>0 && getReserves().size()>0) {
				return(true);
			}
			
			getReserves().removeAll(getReserves());
		}
		
		// とりあえず再ログインしてみる
		while ( doLogin() == false ) {
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}

		// DIGAから情報取得
		
		// (1)録画設定の取得
		while ( getDigaRecordSetting() < 0 ) {
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// (2)予約一覧の取得・キャッシュへの保存
		getDigaReserveList();
		
		return(true);
	}
	
	/*
	 *	予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r)
	{
		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		//
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		// とりあえず再ログインしてみる
		while ( doLogin() == false ) {
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String header;
		String response;
		while (true) {
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVADD.x=32&cCMD_RSVADD.y=16", null);
			header = d[0];
			response = d[1];

			Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
			if (! ma.find()) {
				break;
			}
			
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// 予約情報送信
		String[] keys = {"cRPG", "cRHEX", "cTSTR", "cRHEXEX"};
		for ( String c : keys ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				pdat.put(c,ma.group(1));
			}
		}
		pdat.put("RSV_FIX.x","39");
		pdat.put("RSV_FIX.y","14");
		String pstr = joinPoststr(1, pdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_add.cgi", pstr, null);
			header = d[0];
			response = d[1];
		}
		
		// 予約実行
		Hashtable<String, String> dgdat = new Hashtable<String, String>();
		
		String[] keys2 = { "cRPG", "cRHEX", "cTSTR", "cRHEXEX" };
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				dgdat.put(c,ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","39");
		dgdat.put("RSV_EXEC.y","12");
		pstr = joinPoststr(2, dgdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_addq.cgi", pstr, null);
			header = d[0];
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
			pstr = joinPoststr(6, pdat);
			{
				String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/apl_err.cgi", pstr, null);
				header = d[0];
				response = d[1];
			}
		}
		
		// 登録結果の返信
		ma = Pattern.compile("本体操作中、または現在実行できない操作です。").matcher(response);
		if ( ma.find() ) {
			errmsg = "本体操作中、または現在実行できない操作です。" ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		ma = Pattern.compile("予約が設定できませんでした。").matcher(response);
		if ( ma.find() ) {
			errmsg = "予約が設定できませんでした。" ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		
		
		
		/*
		 * 予約情報の調整 
		 */
		
		// 予約リスト番号を取得（キャッシュに存在しない番号が新番号）
		r.setId(getNewId(response));

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
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}
	
	/*
	 *	予約を更新する
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: UpdateRdEntry()");

		// とりあえず再ログインしてみる
		while ( doLogin() == false ) {
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// POSTデータを変換する
		Hashtable<String, String> pdat = modPostdata(r);
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String header;
		String response;
		while (true) {
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_list.cgi?ANC_RSVLSTNO="+r.getId()+"&cDMYDT="+DMY_TIME, null);
			header = d[0];
			response = d[1];

			Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
			if (! ma.find()) {
				break;
			}
			
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// 更新情報送信
		String[] keys = { "cRVID","cRVORG","cRVORGEX","cRPG","cRHEX","cTSTR","cRHEXEX" };
		for ( String c : keys ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				pdat.put(c,ma.group(1));
			}
		}
		pdat.put("RSV_EDIT.x","48");
		pdat.put("RSV_EDIT.y","14");
		String pstr = joinPoststr(5, pdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_edit.cgi", pstr, null);
			header = d[0];
			response = d[1];
		}

		// 予約実行
		Hashtable<String, String> dgdat = new Hashtable<String, String>();
		
		String[] keys2 = { "cRVID", "cRVORG", "cRVORGEX", "cRPG", "cRHEX", "cTSTR", "cRHEXEX" };
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
			if ( ma.find() ) {
				dgdat.put(c,ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","29");
		dgdat.put("RSV_EXEC.y","12");
		pstr = joinPoststr(4, dgdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_editq.cgi", pstr, null);
			header = d[0];
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
			pstr = joinPoststr(6, pdat);
			{
				String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/apl_err.cgi", pstr, null);
				header = d[0];
				response = d[1];
			}
		}
		
		// 登録結果の返信
		ma = Pattern.compile("本体操作中、または現在実行できない操作です。").matcher(response);
		if ( ma.find() ) {
			errmsg = "本体操作中、または現在実行できない操作です。" ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}
		ma = Pattern.compile("予約が設定できませんでした。").matcher(response);
		if ( ma.find() ) {
			errmsg = "予約が設定できませんでした。" ;
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", errmsg);
			return(false);
		}

		
		
		/*
		 * 予約情報の調整 
		 */

		// 予約パターンID
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		
		// 次回予定日
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		//r.setRec_nextdate(getNextDate(r.getRec_pattern(), r.getZhh()+":"+r.getZmm()));
		
		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		
		// 開始日時・終了日時
		getStartEndDateTime(r);
		
		// 情報置き換え
		getReserves().remove(o);
		getReserves().add(r);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に更新できました。");
		return(true);
	}

	/*
	 *	予約を削除する
	 */
	public ReserveList RemoveRdEntry(String delid) {
		System.out.println("Run: RemoveRdEntry()");

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
		
		// とりあえず再ログインしてみる
		while ( doLogin() == false ) {
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// POSTデータの入れ物を作る
		Hashtable<String, String> pdat = new Hashtable<String, String>();
		Hashtable<String, String> dgdat = new Hashtable<String, String>();
		
		// 事前データを取得する（ログイン後、処理を待たされることがある）
		String header;
		String response;
		while (true) {
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_list.cgi?ANC_RSVLSTNO="+delid+"&cDMYDT="+DMY_TIME, null);
			header = d[0];
			response = d[1];

			Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
			if (! ma.find()) {
				break;
			}
			
			reportProgress("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		
		// 削除情報送信
		Matcher mb = Pattern.compile("option value=\"(.*?)\" selected").matcher(response);
		String[] keys = {"cRVMD","cWEKV","cRVHM1","cRVHM2","cCHSRC","cCHNUM","cRSPD1","cTHEX","cTIMER","cRVID","cRVORG","cRVORGEX","cRPG","cRHEX","cTSTR","cRHEXEX"}; 
		for ( String c : keys ) {
			if ( c.equals("cWEKV") ) {
				if (mb.find()) {
					pdat.put(c,mb.group(1));
					//mb.replaceFirst("");
				}
			}
			else if ( c.equals("cCHSRC") ) {
				if (mb.find()) {
					pdat.put(c,mb.group(1));
					//mb.replaceFirst("");
				}
			}
			else if ( c.equals("cRSPD1")) {
				if (mb.find()) {
					pdat.put(c,mb.group(1));
					//mb.replaceFirst("");
				}
			}
			else if ( c.equals("cTIMER")) {
				if (mb.find()) {
					pdat.put(c,mb.group(1));
					//mb.replaceFirst("");
				}
			}
			else {
				Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response);
				if ( ma.find() ) {
					pdat.put(c,ma.group(1));
				}
			}
		}
		pdat.put("RSV_DEL.x","64");
		pdat.put("RSV_DEL.y","5");
		String pstr = joinPoststr(3, pdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_edit.cgi", pstr, null);
			header = d[0];
			response = d[1];
		}
		
		// 削除実行
		String keys2[] = {"cRVID", "cRVORG", "cRVORGEX", "cRPG", "cRHEX", "cTSTR", "cRHEXEX"};
		for ( String c : keys2 ) {
			Matcher ma = Pattern.compile("name=\""+c+"\" value=\"(.*?)\"").matcher(response) ;
			if ( ma.find() ) {
				dgdat.put(c, ma.group(1));
			}
		}
		dgdat.put("RSV_EXEC.x","33");
		dgdat.put("RSV_EXEC.y","9");
		pstr = joinPoststr(4, dgdat);
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/reserve_delq.cgi", pstr, null);
			header = d[0];
			response = d[1];
		}

		// 削除結果の返信
		Matcher ma = Pattern.compile("本体操作中、または現在実行できない操作です。").matcher(response);
		if ( ma.find() ) {
			System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "本体操作中、または現在実行できない操作です。");
			errmsg = "本体操作中、または現在実行できない操作です。";
			return(null);
		}
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from DIGA >>> \"%s\"\n\n", "正常に削除できました。");
		return(rx);
	}
	
	/*
	 * 
	 */
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}
	
	public PlugIn_RecDIGA_DMR_BW200() {
		super();
		this.setTunerNum(2);
	}
	
	/* ここまで */

	
	
	
	
	
	
	
	
	/* 個別コード－ここから最後まで */
	
	private String DMY_TIME = "";
	private String NONCE = "";
	private String DIGEST = "";


	
	/*
	 * 非公開メソッド 
	 */
	public void poweronoff(boolean isWakeup) {
		// とりあえず再ログインしてみる
		while ( doLogin() == false ) {
			System.out.println("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		// 電源断かどうか確認する
		String header;
		String response;
		while (true) {
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dispframe.cgi?DISP_PAGE=1001&Radio_Drive=1", null);
			header = d[0];
			response = d[1];

			Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
			if (! ma.find()) {
				break;
			}
			
			System.out.println("Wait for retry ...");
			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				// 例外
			}
		}
		Matcher ma = Pattern.compile("電源切").matcher(response);
		if (isWakeup) {
			if ( ! ma.find()) {
				System.out.println("Already wakeup");
				return;
			}
		}
		else {
			if (ma.find()) {
				System.out.println("Already shutdown");
				return;
			}
		}
		// 電源入or切
		{
			String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_POWER.x=61&cCMD_POWER.y=9", null);
			header = d[0];
			response = d[1];
			//System.out.println(response);
		}
	}
	
	@Override
	protected String getNewId(String response) {
		Matcher ma = null;
		String newid = null;
		ma = Pattern.compile("ANC_RSVLSTNO=(\\d+)&").matcher(response);
		while (ma.find()) {
			
			String idtmp = ma.group(1);
			
			boolean flag = true;
			for (ReserveList rx : getReserves()) {
				if (rx.getId().equals(idtmp)) {
					flag = false;
					break;
				}
			}
			if (flag == true) {
				newid = idtmp;
				break;
			}
		}
		return(newid);
	}

	/*
	 *	DIGA MANAGERにログインする
	 */
	private boolean doLogin()
	{
		reportProgress("Run: doLogin("+getIPAddr()+":"+getPortNo()+")");

		// ホームページ
		String header;
 		String response;
 		{
 			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/", null);
			header = d[0];
			response = d[1];
		}
		Matcher ma = Pattern.compile("DMY_TIME=(\\d+?)'").matcher(response);
		if (ma.find()) {
			DMY_TIME = ma.group(1);
			//System.out.println("DMY_TIME="+DMY_TIME);
		}

		// ログインフレーム
		{
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/prevLogin.cgi?DMY_TIME="+DMY_TIME, null);
			header = d[0];
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
			String[] d = reqPOST("http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/loginPsWd.cgi", "passwd=&nonce="+NONCE+"&digest="+DIGEST+"&cmd.x=19&cmd.y=10", null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("onLoadLoginNext").matcher(response);
		if (ma.find()) {
			return(true);
		}

		return(false);
	}

	
	
	/*
	 * 内部処理用に情報を整理する
	 */
	
	public static final String[] DIGA_WDPTNCODE = {"2", "4", "8", "10", "20", "40", "80", "7c", "7c", "fc", "fe" };
	
	private void modPostdata_date(Hashtable<String, String> newdat, String rec_pattern) {
		int i = 0;
		for ( ; i < RPTPTN.length && RPTPTN[i].equals(rec_pattern) == false; i++ ) {
			// 処理はないよ
		}
		System.out.println(rec_pattern+"("+i+")");
		if ( i < RPTPTN.length ) {
			newdat.put("cWEKV", DIGA_WDPTNCODE[i]);
			newdat.put("cRVMD", "");
			newdat.put("date", rec_pattern);
		}
		else {
			Matcher ma = Pattern.compile("^(\\d\\d\\d\\d)/(\\d\\d)/(\\d\\d)").matcher(rec_pattern);
			if ( ma.find() ) {
				newdat.put("cWEKV", "0");
				newdat.put("cRVMD", ma.group(2)+ma.group(3));
				newdat.put("date", rec_pattern);
			}
		}
	}
	private Hashtable<String, String> modPostdata(ReserveList r) {
		
		Hashtable<String, String> newdat = new Hashtable<String, String>();
		
		// 録画チャンネル（ここは良く落ちる）
		r.setChannel(cc.getCH_WEB2CODE(r.getCh_name()));
		Matcher ma = Pattern.compile("^20([123])$").matcher(r.getChannel());
		if ( ma.find() ) {
			newdat.put("cCHSRC","3");		// 外部入力
			newdat.put("cCHNUM","");
		}
		else {
			String[] s = r.getChannel().split(":");
			newdat.put("cCHSRC",s[1]);
			newdat.put("cCHNUM",s[0]);
		}
		
		// 録画レート
		newdat.put("cRSPD1", _recstr2mode(r.getRec_mode()));
		
		// 日付
		modPostdata_date( newdat, r.getRec_pattern() );
		
		// 時刻
		newdat.put("cRVHM1", String.format("%02d%02d",Integer.valueOf(r.getAhh()), Integer.valueOf(r.getAmm())));
		newdat.put("cRVHM2", String.format("%02d%02d",Integer.valueOf(r.getZhh()), Integer.valueOf(r.getZmm())));
		
		// タイトル
		newdat.put("cTHEX", r.getTitle());
		
		// タイマー？？？なんだろうこれは…思い出せない
		newdat.put("cTIMER", "1");
		
		// 作業用
		newdat.put("channel", r.getChannel());
		newdat.put("date", r.getRec_pattern());
		
		return newdat;
	}
	
	private String[] joinArrays(String[] a1,String[] a2,String[] a3,String[] a4) {
		String[] dim = new String[a1.length+a2.length+a3.length+a4.length];
		int i = 0;
		for ( String s : a1 ) {
			dim[i++] = s;
		}
		for ( String s : a2 ) {
			dim[i++] = s;
		}
		for ( String s : a3 ) {
			dim[i++] = s;
		}
		for ( String s : a4 ) {
			dim[i++] = s;
		}
		return(dim.clone());
	}
	private String joinPoststr(int mode, Hashtable<String, String> pdat) {

		String[] pkeys1 = { "cRVMD","cWEKV","cRVHM1","cRVHM2","cCHSRC","cCHNUM","cRSPD1","cTHEX","cTIMER"};
		String[] pkeys2 = { "cRVID","cRVORG","cRVORGEX" };
		String[] pkeys3 = {	"cRPG","cRHEX","cTSTR","cRHEXEX" };
		String[] pkeys4 = {	"RSV_FIX.x","RSV_FIX.y" };
		String[] pkeys5 = {	"RSV_EXEC.x","RSV_EXEC.y" };
		String[] pkeys6 = { "RSV_DEL.x","RSV_DEL.y" };
		String[] pkeys7 = { "RSV_EDIT.x","RSV_EDIT.y" };
		String[] pkeys8 = {	"cRPG","cERR","TTL_DRIVE","cRVID","cRHEX","cTSTR","cRHEXEX","Image_BtnRyoukai.x","Image_BtnRyoukai.y" };

		ArrayList<String> pkeys = new ArrayList<String>();
		switch (mode) {
		case 1:
			String[] keys1 = joinArrays( pkeys1, pkeys3, pkeys4, new String[] {} );
			for ( String s : keys1 ) {
				pkeys.add(s);
			}
			break;
		case 3:
			String[] keys3 = joinArrays( pkeys1, pkeys2, pkeys3, pkeys6 );
			for ( String s : keys3 ) {
				pkeys.add(s);
			}
			break;
		case 4:
			String[] keys4 = joinArrays( pkeys2, pkeys3, pkeys5, new String[] {} );
			for ( String s : keys4 ) {
				pkeys.add(s);
			}
			break;
		case 5:
			String[] keys5 = joinArrays( pkeys1, pkeys2, pkeys3, pkeys7 );
			for ( String s : keys5 ) {
				pkeys.add(s);
			}
			break;
		case 6:
			String[] keys6 = joinArrays( pkeys8, new String[] {}, new String[] {}, new String[] {} );
			for ( String s : keys6 ) {
				pkeys.add(s);
			}
			break;
		default:
			String[] keys99 = joinArrays( pkeys3, pkeys5, new String[] {}, new String[] {} );
			for ( String s : keys99 ) {
				pkeys.add(s);
			}
			break;
		}
		
		String pstr = "";
		for ( String key : pkeys ) {
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
		System.out.println("POST data: "+pstr);
		return(pstr);
	}
	
	/*
	 *	録画を取得する 
	 */
	private int getDigaRecordSetting() {
		System.out.println("run: getDigaRecordSetting");
		
		// 旧リストは全部削除
		vrate.clear();
		chvalue.clear();
		
		// リクエスト発行 
		String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVADD.x=32&cCMD_RSVADD.y=16", null);
		String header = d[0];
		String response = d[1];
			
		if (response == null) {
			// エラーになった場合
			System.out.println("予約できませんでした");
			return(-99);
		}
		
		Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
		if (ma.find()) {
			// リトライしてね
			return(-1);
		}
		ma = Pattern.compile("ログインしてからアクセスしてください。").matcher(response);
		if (ma.find()) {
			// 再ログインしてね
			return(-2);
		}
		
		ma = Pattern.compile("<select name=\"cRSPD1\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\".*?>(.+?)\\(HDD\\)</option>").matcher(ma.group(1));
			while (mb.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(2));
				t.setValue(mb.group(1));
				vrate.add(t);
			}
		}
		
		// キャッシュファイルに保存
		TVSsave(vrate, vrateTFile);
		
		ma = Pattern.compile("<select name=\"cCHSRC\">([\\s\\S]+?)</select>").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("<option value=\"(.+?)\".*?>(.+?)</option>").matcher(ma.group(1));
			while (mb.find()) {
				TextValueSet t = new TextValueSet();
				t.setText(mb.group(2));
				t.setValue(mb.group(1));
				chvalue.add(t);
			}
		}
		
		// キャッシュファイルに保存
		TVSsave(chvalue, chValueTFile);
		
		return(0);
	}
	
	/*
	 *	予約一覧を取得する 
	 */
	private int getDigaReserveList() {
		System.out.println("run: getDigaReserveList");

		// 旧リストは全部削除
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		// リクエスト発行
		String[] d = reqPOST("http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/dvdr/dvdr_ctrl.cgi", "cCMD_RSVLST.x=34&cCMD_RSVLST.y=9", null);
		String header = d[0];
		String response = d[1];

		if (response == null) {
			// エラーになった場合
			System.out.println("予約できませんでした");
			return(-99);
		}
		
		Matcher ma = Pattern.compile("DIGAと通信中です。しばらくお待ちください。").matcher(response);
		if (ma.find()) {
			// リトライしてね
			return(-1);
		}
		ma = Pattern.compile("ログインしてからアクセスしてください。").matcher(response);
		if (ma.find()) {
			// 再ログインしてね
			return(-2);
		}
		
		// 予約詳細を作る
		ReserveList entry = new ReserveList();
		ma = Pattern.compile("<tr class=\\\"s_Bffffff\\\">((\\s|\\S)+?)</tr>").matcher(response);
		while (ma.find()) {
			// 予約内容の部分を切り出す
			String reserve = ma.group(1);

			// 番号
			Matcher mb = Pattern.compile("<a href=\\\"(.+?)\\\">").matcher(reserve);
			if (mb.find()) {
				String[] ar = mb.group(1).split("\\?",2);
				Matcher mc = Pattern.compile("ANC_RSVLSTNO=(\\d+)").matcher(ar[1]);
				if (mc.find()) {
					entry.setId(mc.group(1));
				}
			}
			
			// 日付
			String yy = "";
			String mm = "";
			String dd = "";
			mb = Pattern.compile("\\n(\\d+?)/(\\d+?)\\((.+?)\\)</div>").matcher(reserve);
			if (mb.find()) {
				mm = mb.group(1);
				dd = mb.group(2);

				String[] ar = _mmdd2yyyymmdd(mm, dd);
				yy = ar[0];
				mm = ar[1];
				dd = ar[2];
			}

			// チャンネル
			mb = Pattern.compile("\\n<td width=\"100\" height=\"20\" class=\"s_F16\">&nbsp;(.+?)&nbsp;(.+?)</td>").matcher(reserve);
			if (mb.find()) {
				entry.setChannel(mb.group(1)+" "+mb.group(2));
				entry.setCh_name(cc.getCH_REC2WEB(entry.getChannel()));
			}
			
			// 開始終了時刻と録画時間
			mb = Pattern.compile("\\n(\\d+):(\\d+)～(\\d+):(\\d+)</div>").matcher(reserve);
			if (mb.find()) {
				String ar[] = _hhmm2hhmm_min( mb.group(1)+":"+mb.group(2), mb.group(3)+":"+mb.group(4));
				entry.setAhh(ar[0]);
				entry.setAmm(ar[1]);
				entry.setZhh(ar[2]);
				entry.setZmm(ar[3]);
				entry.setRec_min(ar[4]);
			}
			
			// 画質
			mb = Pattern.compile("\\n(.+?)\\(HDD\\)</div>").matcher(reserve);
			if (mb.find()) {
				entry.setRec_mode(mb.group(1));
			}
			
			// パターン
			mb = Pattern.compile("\\n<td width=\"250\" height=\"20\" class=\"s_F16\">&nbsp;(.+?)、").matcher(reserve);
			if (mb.find()) {
				entry.setRec_pattern(_recpatternDGstr2RDstr(mb.group(1),yy,mm,dd));
				entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
				entry.setRec_nextdate(CommonUtils.getNextDate(entry));
				//entry.setRec_nextdate(getNextDate(entry.getRec_pattern(), entry.getZhh()+":"+entry.getZmm()));
			}
			
			// 開始日時
			mb = Pattern.compile("^(\\d+)/(\\d+)/(\\d+)").matcher(entry.getRec_nextdate());
			if (mb.find()) {
				// ★★★　MM/DDをYYYY/MM/DDに戻す？　★★★
				GregorianCalendar c = new GregorianCalendar(Locale.JAPAN);
				c.set(Calendar.MINUTE, Integer.valueOf(entry.getAmm()));
				c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(entry.getAhh()));
				c.set(Calendar.DATE, Integer.valueOf(mb.group(3)));
				c.set(Calendar.MONTH, Integer.valueOf(mb.group(2))-1);
				c.set(Calendar.YEAR, Integer.valueOf(mb.group(1)));
				entry.setStartDateTime(String.format("%04d/%02d/%02d %02d:%02d", c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)));

				c.add(Calendar.MINUTE, Integer.valueOf(entry.getRec_min()));
				entry.setEndDateTime(String.format("%04d/%02d/%02d %02d:%02d", c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)));
			}
			
			
			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			copyAttributes(entry, getReserves());
			
			// 予約情報を保存
			newReserveList.add(entry.clone());
		}
		setReserves(newReserveList);
		
		// 詳細情報の取得
		System.out.println("========");
		for (int i=0; i<getReserves().size(); i++) {
			ReserveList e = getReserves().get(i);
			
			getDigaReserveInfo(e, e.getId());

			reportProgress(String.format("[%s] %s\t%s\t%s\t%s:%s\t%s:%s\t%sm\t%s\t%s\t%s\t%s",
					(i+1), e.getId(), e.getRec_pattern(), e.getRec_nextdate(), e.getAhh(), e.getAmm(), e.getZhh(),	e.getZmm(),	e.getRec_min(), e.getRec_mode(), e.getTitle(), e.getChannel(), e.getCh_name()));
		}
		System.out.println("========");

		// キャッシュファイルに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		return(0);
	}
	/*
	 * 予約詳細を取得する
	 */
	private ReserveList getDigaReserveInfo(ReserveList reserve_d, String id)
	{

		String header;
		String response;
		{
			String[] d = reqPOST("http://"+this.getIPAddr()+":"+this.getPortNo()+"/cgi-bin/reserve_list.cgi", "ANC_RSVLSTNO="+id+"&cDMYDT="+DMY_TIME, null);
			header = d[0];
			response = d[1];
		}
		if ( response == null ) {
			System.out.println("★");
			System.out.println("★ 予約情報を取得できませんでした。");
			System.out.println("★");
			return(null);
		}
		
		// 予約日付
		Matcher ma = Pattern.compile("name=\"cRVMD\" value=\"(\\d\\d)(\\d\\d)\"").matcher(response);
		if (! ma.find()) {
			return(null);
		}
		String[] da = _mmdd2yyyymmdd(ma.group(1), ma.group(2));
		String yy = da[0];
		String mm = da[1];
		String dd = da[2];
		
		// 開始終了時刻と録画時間
		ma = Pattern.compile("name=\"cRVHM1\" value=\"(\\d\\d)(\\d\\d)\"").matcher(response);
		if (! ma.find()) {
			return(null);
		}
		String ahh = ma.group(1);
		String amm = ma.group(2);
		ma = Pattern.compile("name=\"cRVHM2\" value=\"(\\d\\d)(\\d\\d)\"").matcher(response);
		if (! ma.find()) {
			return(null);
		}
		String zhh = ma.group(1);
		String zmm = ma.group(2);
		String[] db = _hhmm2hhmm_min(ahh+":"+amm,zhh+":"+zmm);
		ahh = db[0];
		amm = db[1];
		zhh = db[2];
		zmm = db[3];
		String rec_min = db[4];
		
		// 画質
		ma = Pattern.compile("name=\"cRSPD1\"[\\s\\S]+?value=\"(\\d+?)\" selected>").matcher(response);
		if (! ma.find()) {
			return(null);
		}
		String rec_mode = _recmode2str(ma.group(1));
		
		// チャンネル
		String ch_source = "";
		String ch_number = "";
		String channel = "";
		ma = Pattern.compile("<select name=\"cCHSRC\">(\\s|\\S)*?value=\"(.+?)\" selected(\\s|\\S)*?</select>").matcher(response);
		if (ma.find()) {
			ch_source = ma.group(2);
			Matcher mb = Pattern.compile("name=\"cCHNUM\" value=\"(.+?)\"").matcher(response);
			if (mb.find()) {
				ch_number = mb.group(1);
			}
		}
		else {
			Matcher mb = Pattern.compile("<input type=\"hidden\" name=\"cCHSRC\" value=\"(.+?)\">(\\s|\\S)*?<input type=\"hidden\" name=\"cCHNUM\" value=\"(.+?)\" maxlength=\"5\" size=\"5\">").matcher(response);
			if (mb.find()) {
				ch_source = mb.group(1);
				ch_number = mb.group(3);
			}
		}
		for ( TextValueSet t : chvalue ) {
			if ( t.getValue().equals(ch_source) ) {
				channel = t.getText()+" "+ch_number;
				break;
			}
		}
		String ch_name = cc.getCH_REC2WEB(channel);
		
		// パターン
		ma = Pattern.compile("name=\"cWEKV\"[\\s\\S]+?value=\"(\\d+?)\" selected>").matcher(response);
		if ( ! ma.find()) {
			return(null);
		}
		String rec_pattern = _recpatternDG2RDstr(ma.group(1), yy, mm, dd);
		
		// タイトル
		ma = Pattern.compile("name=\"cTHEX\" value=\"(.*?)\"").matcher(response);
		if (! ma.find()) {
			return(null);
		}
		String title = ma.group(1);
		
		
		// 予約情報更新
		reserve_d.setId(id);
		reserve_d.setRec_pattern(rec_pattern);
		reserve_d.setRec_pattern_id(getRec_pattern_Id(rec_pattern));
		reserve_d.setAhh(ahh);
		reserve_d.setAmm(amm);
		reserve_d.setZhh(zhh);
		reserve_d.setZmm(zmm);
		reserve_d.setRec_min(rec_min);
		reserve_d.setRec_mode(rec_mode);
		reserve_d.setTitle(title);
		reserve_d.setTitlePop(TraceProgram.replacePop(title));
		reserve_d.setChannel(channel);
		reserve_d.setCh_name(ch_name);
		
		
		
		return(reserve_d);
	}
	
	//
	private String _recmode2str(String mode)
	{
		for ( TextValueSet t : vrate ) {
			if (t.getValue().equals(mode)) {
				return(t.getText());
			}
		}
		return(null);
	}
	private String _recstr2mode(String str)
	{
		for ( TextValueSet t : vrate ) {
			if (t.getText().equals(str)) {
				return(t.getValue());
			}
		}
		return(null);
	}
	
	//
	private String _recpatternDG2RDstr(String source, String yy, String mm, String dd)
	{
		if ( source.equals("fe") ) {
			return "毎日";
		}
		else if ( source.equals("fc") ) {
			return "毎月～土";
		}
		else if ( source.equals("7c") ) {
			return "毎月～金";
		}
		else if ( source.equals("2") ) {
			return "毎日曜日";
		}
		else if ( source.equals("4") ) {
			return "毎月曜日";
		}
		else if ( source.equals("8") ) {
			return "毎火曜日";
		}
		else if ( source.equals("10") ) {
			return "毎水曜日";
		}
		else if ( source.equals("20") ) {
			return "毎木曜日";
		}
		else if ( source.equals("40") ) {
			return "毎金曜日";
		}
		else if ( source.equals("80") ) {
			return "毎土曜日";
		}
		else {
			GregorianCalendar cal = new GregorianCalendar(Locale.JAPAN);
			cal.setTime(new Date());
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.DATE, Integer.valueOf(dd));
			cal.set(Calendar.MONTH, Integer.valueOf(mm)-1);
			cal.set(Calendar.YEAR, Integer.valueOf(yy));
			return String.format("%04d/%02d/%02d(%s)", cal.get(Calendar.YEAR),cal.get(Calendar.MONTH)+1,cal.get(Calendar.DATE),CommonUtils.WDTPTN[cal.get(Calendar.DAY_OF_WEEK)-1]);
		}
	}
	private String _recpatternDGstr2RDstr(String source, String syy, String smm, String sdd)
	{

		int yy = Integer.valueOf(syy);
		int mm = Integer.valueOf(smm);
		int dd = Integer.valueOf(sdd);
		
		GregorianCalendar cal = new GregorianCalendar(Locale.JAPAN);
		cal.setTime(new Date());
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.DATE, dd);
		cal.set(Calendar.MONTH, mm-1);
		cal.set(Calendar.YEAR, yy);
		
		//Matcher m = Pattern.compile("^曜日指定").matcher(source);
		Matcher m = Pattern.compile("^毎週").matcher(source);
		if (m.find()) {
			return("毎"+CommonUtils.WDTPTN[cal.get(Calendar.DAY_OF_WEEK)-1]+"曜日");
		}
		m = Pattern.compile("^曜日指定").matcher(source);
		if (m.find()) {
			return("毎月～木");
		}
		m = Pattern.compile("^月金").matcher(source);
		if (m.find()) {
			return("毎月～金");
		}
		m = Pattern.compile("^月土").matcher(source);
		if (m.find()) {
			return("毎月～土");
		}
		m = Pattern.compile("^毎日").matcher(source);
		if (m.find()) {
			return("毎日");
		}
		
		return(String.format("%04d/%02d/%02d(%s)", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE), CommonUtils.WDTPTN[cal.get(Calendar.DAY_OF_WEEK)-1]));
	}
}
