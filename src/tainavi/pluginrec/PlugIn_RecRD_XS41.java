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

/*
 * 
 */

public class PlugIn_RecRD_XS41 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_XS41 clone() {
		return (PlugIn_RecRD_XS41) super.clone();
	}
	
	private static final String thisEncoding = "MS932";

	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "RD-XS41"; }
	public RecType getType() { return RecType.RECORDER; }
	
	// 予約ダイアログのラベル
	public String getLabel_XChapter() { return "無音部分ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_MsChapter() { return "DVD時ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_MvChapter() { return "音多連動ﾁｬﾌﾟﾀ分割"; }
	public String getLabel_DVDCompat() { return "DVD互換モード"; }
	public String getLabel_Aspect() { return "DVD記録時画面比"; }
	public String getLabel_BVperf() { return "高ﾚｰﾄ節約"; }
	public String getLabel_LVoice() { return "ﾗｲﾝ音声選択"; }
	
	// 個体の特性
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String rsvedFile = "";
	//private String vrateTFile = "";
	//private String arateTFile = "";
	
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
		return false;
	}

	/*
	 *	レコーダーから予約一覧を取得する 
	 */
	public boolean GetRdReserve(boolean force)
	{
		errmsg = "";
		
		System.out.println("Run: GetRdReserve("+force+")");
		
		//
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String vrateTFile = "env/videorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String arateTFile = "env/audiorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String dvdcompatTFile = "env/dvdcompat."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String xChapterTFile = "env/xchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（録画設定ほか）
			vrate = TVSload(vrateTFile);
			if ( vrate.size() > 0 ) {
				System.out.println("+video rate="+vrateTFile);
			}
			arate = TVSload(arateTFile);
			if ( arate.size() > 0 ) {
				System.out.println("+audio rate="+arateTFile);
			}
			dvdcompat = TVSload(dvdcompatTFile);
			if ( dvdcompat.size() > 0 ) {
				System.out.println("+dvdcompat="+dvdcompatTFile);
			}
			// 自動チャプタ関連
			xchapter = TVSload(xChapterTFile);
			if ( xchapter.size() > 0 ) {
				System.out.println("+xchapter="+xChapterTFile);
			}
			
			// キャッシュから読み出し（予約一覧）
			setReservesV1(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && arate.size()>0 &&
					xchapter.size()>0) {
				return(true);
			}
			
			getReserves().removeAll(getReserves());
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("get reserved list(1/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get reserved list(2/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
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
			String hdr = d[0];
			String res = d[1];
			Matcher mb = null; 
			
			// 画質設定
			vrate.clear();
			
			mb = Pattern.compile("<select name=\"vrate\"[\\s\\S]*?>([\\s\\S]+?)</select>").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("<option value=\"(.*?)\".*?>(.+?)</option>").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					Matcher md = Pattern.compile(" ").matcher(mc.group(2));
					if (md.find()) {
						t.setText(md.replaceAll(""));
					}
					else {
						t.setText(mc.group(2));
					}
					t.setValue(mc.group(1));
					vrate.add(t);
				}
			}
			
			TVSsave(vrate, vrateTFile);
			
			// 音質設定
			arate.clear();
			
			mb = Pattern.compile("<select name=\"amode\".+?>([\\s\\S]+?)</select>").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("<option value=\"(.*?)\".*?>(.+?)</option>").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(2));
					t.setValue(mc.group(1));
					arate.add(t);
				}
			}
			
			TVSsave(arate, arateTFile);
			
			// (1-5)DVD互換モード
			dvdcompat.clear();
			
			mb = Pattern.compile("<select name=\"dvdr\" [^>]*?>([\\s\\S]+?)</select>").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("<option value\\s*=\"(.*?)\"( selected)?>\\s*(.+?)\\s*</option>").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(3));
					t.setValue(mc.group(1));
					dvdcompat.add(t);
				}
			}
			
			TVSsave(dvdcompat, dvdcompatTFile);
			
			// (1-7)自動チャプタ関連
			xchapter.clear();
			mb = Pattern.compile("<select[^>]+?name=\"bAutoChapter\"[^>]+?>([\\s\\S]+?)</select>").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("<option value\\s*=\"(.*?)\"\\s*(selected)?>\\s*(.+?)\\s*</option>").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(3));
					t.setValue(mc.group(1));
					xchapter.add(t);
				}
			}
			TVSsave(xchapter, xChapterTFile);
		}
		
		// (2)予約一覧の取得
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
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("get reserved list(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			reportProgress("get reserved list(2/7).");
			idx = ma.group(1);
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
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
			reportProgress("get program.(3/7)");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?"+lineno,null);

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
			header = d[0];
			response = d[1];
		}

		//System.out.println("#"+response);

		// 登録結果の確認
		ma = Pattern.compile("msg=\"(.+?)\"").matcher(response);
		if ( ma.find() ) {
			errmsg = ma.group(1);
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			
			ma = Pattern.compile("(正常に更新できました。|予約時間が一部重複しています。|現在のHDD残量では入りません。)").matcher(errmsg);
			if ( ! ma.find()) {
				return false;
			}
		}
		
		/*
		 * 予約情報の調整 
		 */
		// キャッシュ上の予約情報を更新する（ライン入力放送局名引継対応）
		getReserves().add(r);
		
		// 新しい予約一覧を取得する
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
			response = d[1];
		}

		setReservesV1(GetRdReservedList(response));
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");

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
		
		errmsg = "";

		// 更新対象を探す（内部）
		ReserveList rx = null;
		for ( ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(r.getId())) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			errmsg = "キャッシュ上に操作対象が見つかりません。";
			return(false);
		}
		
		// 更新準備
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("get identifier(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
			response = d[1];
		}
		
		// 更新対象を探す（外部）
		String id = null;
		for ( ReserveList cr : GetRdReservedList(response) ) {
			if (cr.getTitle().equals(rx.getTitle()) &&
					cr.getChannel().equals(rx.getChannel()) &&
					cr.getRec_pattern().equals(rx.getRec_pattern())
			) {
				id = cr.getId();
				break;
			}
		}
		if (id == null) {
			errmsg = "レコーダ上に操作対象が見つかりません。";
			return(false);
		}

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
			header = d[0];
			response = d[1];
		}

		// 更新結果の確認
		ma = Pattern.compile("msg=\"(.+?)\"").matcher(response);
		if ( ma.find() ) {
			errmsg = ma.group(1);
			System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", errmsg);
			
			ma = Pattern.compile("(正常に更新できました。|予約時間が一部重複しています。|現在のHDD残量では入りません。)").matcher(errmsg);
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
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
			response = d[1];
		}

		setReservesV1(GetRdReservedList(response));

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
		
		// 削除対象を探す
		ReserveList rx = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			errmsg = "キャッシュ上に操作対象が見つかりません。";
			return(null);
		}

		// 削除準備
		Matcher ma = null;
		String idx = "";
		String header;
		String response;
		{
			reportProgress("get identifier(1/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
			response = d[1];
		}
		
		// 削除対象を探す（外部）
		String id = null;
		for ( ReserveList cr : GetRdReservedList(response) ) {
			if (cr.getTitle().equals(rx.getTitle()) &&
					cr.getChannel().equals(rx.getChannel()) &&
					cr.getRec_pattern().equals(rx.getRec_pattern())
			) {
				id = cr.getId();
				break;
			}
		}
		if (id == null) {
			errmsg = "レコーダ上に操作対象が見つかりません。";
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
			header = d[0];
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

		// 新しい予約一覧を取得する
		{
			reportProgress("get identifier(6/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			header = d[0];
			response = d[1];
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(7/7).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			header = d[0];
			response = d[1];
		}

		setReservesV1(GetRdReservedList(response));
		
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

	private String joinPoststr(Hashtable<String, String> pdat)
	{
		String[] pkeys = {
			"bExec",
			"title_name",
			"channel",
			"date",
			"start_hour",
			"start_minute",
			"end_hour",
			"end_minute",
			"disc",
			"vrate",
			"amode",
			"genre",
			"dvdr",
			"aspect",
			"bVPerform",
			"bAutoChapter",
			"detail",
			"dtv_sid",
			"dtv_nid",
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
		System.out.println("POST data: "+pstr);
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
		newdat.put("dvdr", _recstr2dvdcompat(r.getRec_dvdcompat()));
		
		// 録画チャンネル
		String channel = cc.getCH_WEB2CODE(r.getCh_name());
		newdat.put("channel", channel);
		String ch_no = getChCode().getCH_CODE2REC(channel);
		r.setChannel(ch_no);
		
		// 録画レート
		newdat.put("vrate", _recstr2mode(r.getRec_mode()));

		// 録画レート
		newdat.put("amode", _recstr2audio(r.getRec_audio()));

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


		newdat.put("start_hour", r.getAhh());
		newdat.put("start_minute", r.getAmm());
		newdat.put("end_hour", r.getZhh());
		newdat.put("end_minute", r.getZmm());

		// 自動チャプター関連
		newdat.put("bAutoChapter"		, text2value(xchapter, r.getRec_xchapter()));

		// 追加
		newdat.put("disc"				, "0");
		newdat.put("aspect"				, "1");
		newdat.put("bVPerform"			, "0");
		//newdat.put("bAutoChapter"		, "0");
		newdat.put("dtv_sid"			, "0");
		newdat.put("dtv_nid"			, "0");
		newdat.put("end_form"			, "0");
		
		return(newdat);
	}

	private String _recstr2audio(String str)
	{
		for ( TextValueSet t : arate ) {
			if (t.getText().equals(str)) {
				return(t.getValue());
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
		//else if (genre.equals(TVProgram.ProgGenre.THEATER.toString())) {
		//	return "6";
		//}
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
	private String _recstr2dvdcompat(String str)
	{
		for ( TextValueSet t : dvdcompat ) {
			if (t.getText().equals(str)) {
				return(t.getValue());
			}
		}
		return(null);
	}
	
	
	//
	private ArrayList<ReserveList> GetRdReservedList(String response) {
		
		//
		response = response.replaceAll("\n","");
		
		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
		
		String buf = "";
		Matcher ma = Pattern.compile("<tr [^>]*?>([\\s\\S]*?)</tr>").matcher(response);
		while (ma.find()) {

			// 個々のデータを取り出す
			buf = ma.group(1);
			
			Matcher mb = null; 
			mb = Pattern.compile(">新規予約<").matcher(buf);
			if (mb.find()) {
				break;
			}
			
			ReserveList entry = new ReserveList();
			
			String[] d = new String[9];
			mb = Pattern.compile("<td .*?>(.*?)</td>").matcher(buf);
			for (int i=0; i<9; i++) {
				if ( mb.find()) {
					d[i] = mb.group(1);
				}
			}
			
			// 予約実行ON/OFF
			mb = Pattern.compile("check_off\\.gif").matcher(d[1]);
			if (mb.find()) {
				entry.setExec(false);
			}
			
			// 予約名のエスケープを解除する
			String title = d[2];
			mb = Pattern.compile("<a .*?>(.+?)</a>").matcher(title);
			if (mb.find()) title = mb.group(1);
			mb = Pattern.compile("<BR>").matcher(title);	// 余計な改行の削除
			if (mb.find()) title = mb.replaceAll("");
			mb = Pattern.compile("&quot;").matcher(title);
			if (mb.find()) title = mb.replaceAll("\"");
			mb = Pattern.compile("&lt;").matcher(title);
			if (mb.find()) title = mb.replaceAll("<");
			mb = Pattern.compile("&gt;").matcher(title);
			if (mb.find()) title = mb.replaceAll(">");
			mb = Pattern.compile("&nbsp;").matcher(title);
			if (mb.find()) title = mb.replaceAll(" ");
			
			entry.setId(String.valueOf(Integer.valueOf(d[0])-1));
			entry.setRec_pattern(d[4]);
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[5]);
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
			
			entry.setTuner("");
			entry.setRec_mode(d[7]);
			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));
			entry.setCh_name(cc.getCH_REC2WEB(d[3]));
			entry.setChannel(d[3]);

			entry.setRec_audio(d[8]);
			//entry.rec_folder = data.get();	// 予約一覧からはとれない
			//entry.rec_genre = data.get();		//　予約一覧からはとれない
			
			// 予約情報を保存
			newReserveList.add(entry.clone());
		}
		
		return(newReserveList);
	}
}
