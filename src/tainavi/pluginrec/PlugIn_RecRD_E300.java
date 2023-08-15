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
import tainavi.GetRDStatus;
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

public class PlugIn_RecRD_E300 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_E300 clone() {
		return (PlugIn_RecRD_E300) super.clone();
	}

	private static final String thisEncoding = "MS932";

	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "VARDIA RD-E300"; }
	public RecType getType() { return RecType.RECORDER; }
	
	// 個体の特性
	private GetRDStatus gs = new GetRDStatus();
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String rsvedFile = "";
	
	private String errmsg = "";

	// 公開メソッド
	
	/*
	 * 
	 */
	public ChannelCode getChCode() {
		return cc;
	}
	
	/*
	 * チャンネルリモコン機能
	 */
	public boolean ChangeChannel(String Channel) {
		
		if (Channel == null) {
			return true;
		}
		
		int curBC;
		int newBC;
		String chNo;
		byte enc;
		
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
			
			byte[] ba = ch.getBytes();
			enc = ba[0];
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
		for (int i=0; i<4 && newBC != (cBC = (curBC+i)%4); i++) {
			// 地上アナログ(3)が選択できるのはRE(0x06)だけ
			if (enc != 0x06 && cBC == 3) {
				continue;
			}
			
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
		String chTypeTFile = "env/chtype."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";

		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（録画設定ほか）
			vrate = TVSload(vrateTFile);
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
			
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && arate.size()>0 &&
					folder.size()>0 && encoder.size()>0 && device.size()>0 &&
					xchapter.size()>0 && mschapter.size()>0 && mvchapter.size()>0 &&
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
			reportProgress("get reserved list(1/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get reserved list(2/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				errmsg = "レコーダーが反応しません";
				return(false);
			}
		}
		
		// 予約エントリー数を取得する
		int RsvCnt = 0;
		ma = Pattern.compile("RsvCnt\\s*=\\s*(\\d+?);").matcher(response);
		if ( ! ma.find()) {
			errmsg = "レコーダーからの情報取得に失敗しました（予約一覧）";
			return false;
		}
		RsvCnt = Integer.valueOf(ma.group(1));
		
		// (1)録画設定の取得
		{
			// ハング防止のおまじない
			reportProgress("get reserved list(3/4).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);
		}
		{
			reportProgress("get reserved list(4/4).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
			String hdr = d[0];
			String res = d[1];
			Matcher mb = null;
			
			// 正常取得チェック
			if ( ! res.matches("[\\s\\S]*var hdd_folder_text[\\s\\S]*")) {
				errmsg = "レコーダーからの情報取得に失敗しました（録画設定）";
				return false;
			}
			
			// (1-1)画質設定
			setSettingEtc(vrate,"vrate",1,res);
			TVSsave(vrate, vrateTFile);
			
			// (1-2)音質設定
			setSettingEtc(arate,"amode",0,res);
			TVSsave(arate, arateTFile);
			
			// (1-3)フォルダ一覧
			setSettingEtc(folder,"hdd_folder",1,res);
			TVSsave(folder, folderTFile);
			
			// (1-4)エンコーダ
			encoder.clear();
			
			mb = Pattern.compile("var double_encode_flg = (\\d+?);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\\n\\s*?switch \\( double_encode_flg \\) \\{([\\s\\S]+?default:)").matcher(res);
				if (mc.find()) {
					Matcher md = Pattern.compile("(case "+mb.group(1)+":[\\s\\S]+?break;)").matcher(mc.group(1));
					if (md.find()) {
						Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?act_(.+?)\\.gif").matcher(md.group(1));
						while (me.find()) {
							TextValueSet t = new TextValueSet();
							t.setText(me.group(2));
							t.setValue(me.group(1));
							encoder.add(t);
						}
					}
				}
			}
			
			TVSsave(encoder, encoderTFile);
			
			// (1-5)DVD互換モード
			setSettingEtc(dvdcompat,"dvdr",1,res);
			TVSsave(dvdcompat, dvdcompatTFile);
			
			// (1-6)記録先デバイス
			setSettingEtc(device,"media",0,res);
			TVSsave(device, deviceTFile);
			
			// (1-7)自動チャプター関連
			setSettingEtc(xchapter,"mutechapter",0,res);
			TVSsave(xchapter, xChapterTFile);
			
			setSettingEtc(mschapter,"magicchapter",0,res);
			TVSsave(mschapter, msChapterTFile);

			setSettingEtc(mvchapter,"cmchapter",0,res);
			TVSsave(mvchapter, mvChapterTFile);
			
			// (1-8)チャンネルコードバリュー
			chvalue.clear();
			chtype.clear();
			for ( String typ : new String[] { "uvd","bsd","csd","uva","bsa","l1","l2","l3" } ) {
				String txtkey = typ+"_ch_text";
				String valkey = typ+"_ch_value";
				Matcher mc = Pattern.compile("var "+txtkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
				Matcher md = Pattern.compile("var "+valkey+"\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
				if ( mc.find() && md.find() ) {
					Matcher me = Pattern.compile("\"(.+?)\",?").matcher(mc.group(1));
					Matcher mf = null;
					if ( typ.equals("uva") ) {
						mf = Pattern.compile("(\\d+),").matcher(md.group(1));
					}
					else {
						mf = Pattern.compile("\"([^\"]+?)\",").matcher(md.group(1));
					}
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
			TVSsave(chvalue, chValueTFile);
			TVSsave(chtype, chTypeTFile);
		}
		
		// 予約一覧データの分析
		// 情報ブロックをとりだし…
		/*
			var	c1  = new Array(RsvCnt);		// 予約ID
			var	c2  = new Array(RsvCnt);		// 実行ﾌﾗｸﾞ
			var	c3  = new Array(RsvCnt);		// 予約名
			var	c4  = new Array(RsvCnt);		// REC1/REC2
			var	c5  = new Array(RsvCnt);		// 表示CH
			var	c6  = new Array(RsvCnt);		// 録画日付
			var	c7  = new Array(RsvCnt);		// 録画開始時刻
			var	c8  = new Array(RsvCnt);		// 録画終了時刻
			var	c9  = new Array(RsvCnt);		// 記録先
			var	c10 = new Array(RsvCnt);		// 画質
			var	c11 = new Array(RsvCnt);		// 音質
			var	c12 = new Array(RsvCnt);		// 録画開始時刻の文字色
			var	c13 = new Array(RsvCnt);		// 録画終了時刻の文字色
			var	c14 = new Array(RsvCnt);		// おすすめ
			var	c15 = new Array(RsvCnt);		// 番組追っかけ
			var	c16 = new Array(RsvCnt);		// スポーツ延長
			var	c17 = new Array(RsvCnt);		// 優先度
		 */
		{
			//int cnt = 0;
			ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();
			
			ma = Pattern.compile("(c1\\[\\d+?\\]=[\\s\\S]+?\";)\\n").matcher(response);
			while ( ma.find() ) {
				// 個々のデータを取り出す
				ReserveList entry = new ReserveList();
				
				Matcher mb = null;
					
				String[] d = new String[17];
				mb = Pattern.compile("c\\d+?\\[\\d+?\\]=\"(.*?)\";").matcher(ma.group(1));
				for (int i=0; i<d.length; i++) {
					if ( mb.find()) {
						d[i] = mb.group(1);
					}
				}
				
				// 予約ID
				entry.setId(d[0]);
				
				// 実行フラグ
				if ( ! d[1].equals("1")) {
					entry.setExec(false);
				}
				
				// 予約名
				entry.setTitle(CommonUtils.unEscape(d[2]).replaceAll("<BR>", ""));
				entry.setTitlePop(TraceProgram.replacePop(entry.getTitle()));
				
				// REC1/REC2
				entry.setTuner(value2text(encoder, d[3]));
				
				// 表示CH
				entry.setChannel(d[4]);
				entry.setCh_name(cc.getCH_REC2WEB(d[4]));
				
				// 録画日付・開始時刻・終了時刻
				entry.setRec_pattern(d[5]);
				entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
				mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[6]+"-"+d[7]);
				if (mb.find()) {
					entry.setAhh(mb.group(1));
					entry.setAmm(mb.group(2));
					entry.setZhh(mb.group(3));
					entry.setZmm(mb.group(4));
				}
				entry.setRec_nextdate(CommonUtils.getNextDate(entry));
				entry.setRec_min(CommonUtils.getRecMin(entry.getAhh(), entry.getAmm(), entry.getZhh(), entry.getZmm()));
				getStartEndDateTime(entry);
				
				// 記録先
				entry.setRec_device(d[8]);
				
				// 画質・音質
				entry.setRec_mode(d[9]);
				entry.setRec_audio(d[10]);
				
				// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
				copyAttributes(entry, getReserves());
				
				// 予約情報を保存
				newReserveList.add(entry.clone());
			}
			setReserves(newReserveList);
		}
		
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
			reportProgress("send request.(5/7)");
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
				Matcher mc = Pattern.compile("(予約時間が重複しています。|修正しました。)").matcher(errmsg);
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

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		// 更新準備
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
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			reportProgress("get identifier(2/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
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
				Matcher mc = Pattern.compile("(予約時間が重複しています。|修正しました。)").matcher(errmsg);
				if ( ! mc.find() ) {
					return(false);
				}
			}			
		}

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に更新できました。");

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
		int lineno = 0;
		String header;
		String response;
		{
			reportProgress("get identifier(1/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
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
			reportProgress("send request.(5/5)");
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
			}
			return(null);
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
//			"title_link",
			"auto_delete",
//			"video_es",
//			"audio_es",
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
		
		return(pstr);
	}
	
	private Hashtable<String, String> modPostdata(ReserveList r) {

		Hashtable<String, String> newdat = new Hashtable<String, String>();
		
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
				newdat.put("title_name", URLEncoder.encode(r.getTitle(),thisEncoding));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
	
			// 予約詳細
			try {
				newdat.put("detail", URLEncoder.encode(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")), thisEncoding));
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
		newdat.put("genre", _recstr2genre(r.getRec_genre()));
		
		// DVD互換モード
		newdat.put("dvdr", text2value(dvdcompat, r.getRec_dvdcompat()));
		
		// 録画チャンネル
		String channel = cc.getCH_WEB2CODE(r.getCh_name());
		String ch_no = cc.getCH_CODE2REC(channel);
		if (chtype.size() > 0) {
			String typ = text2value(chtype, channel);
			if (typ.equals("uva")) {
				newdat.put("broadcast","0");		// 地上アナログ
			}
			else if (typ.equals("bsa")) {
				newdat.put("broadcast","1");		// BSアナログ
			}
			else if (typ.equals("l1")) {
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
		}
		else {
			// 後方互換
			Matcher ma = Pattern.compile("^(.)(.)").matcher(ch_no);
			if (ma.find()) {
				if ((ma.group(1)+ma.group(2)).equals("CH")) {
					newdat.put("broadcast","0");		// 地上アナログ
				}
				else if ((ma.group(1)+ma.group(2)).equals("BS")) {
					newdat.put("broadcast","10");		// BSデジタル
				}
				else if ((ma.group(1)+ma.group(2)).equals("CS")) {
					newdat.put("broadcast","11");		// 110度CSデジタル
				}
				else if (ma.group(1).equals("L")) {
					newdat.put("broadcast", String.valueOf(Integer.valueOf(ma.group(2))+1));	// ライン入力ＡＢＣ
				}
				else {
					newdat.put("broadcast","12");		// 地上デジタル
				}
				
				newdat.put("channel_list", channel);
				newdat.put("channel_no", channel);
			}
		}
		
		// 録画レート
		newdat.put("enc_type", text2value(encoder, r.getTuner()));
		newdat.put("vrate", text2value(vrate, r.getRec_mode()));
		
		// 録音レート
		newdat.put("amode", text2value(arate, r.getRec_audio()));

		// TS時の修正
		if (r.getTuner().equals("TS")) {
			r.setRec_mode("TS");
			r.setRec_audio("");
		}

		// 繰り返し
		Matcher ma = Pattern.compile("^\\d").matcher(r.getRec_pattern());
		if (ma.find()) { 
			newdat.put("maiyoubi_type","0");
			newdat.put("date", r.getRec_pattern());
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


		newdat.put("start_hour", r.getAhh());
		newdat.put("start_minute", r.getAmm());
		newdat.put("end_hour", r.getZhh());
		newdat.put("end_minute", r.getZmm());

		// 記録先
		newdat.put("disc", text2value(device, r.getRec_device()));

		// 自動チャプター関連
		newdat.put("bAutoChapter"		, text2value(xchapter, r.getRec_xchapter()));
		newdat.put("MagicChapter"		, text2value(mschapter, r.getRec_mschapter()));
		newdat.put("CM_Chapter"			, text2value(mvchapter, r.getRec_mvchapter()));
		
		// 追加
		newdat.put("start_form"			, "");
		newdat.put("rec_priority"		, "150");
		newdat.put("title_link"			, "0");
		newdat.put("auto_delete"		, "0");
		newdat.put("video_es"			, "00");
		newdat.put("audio_es"			, "10");
		newdat.put("edge_left"			, "0");		// 録画のりしろ
		//newdat.put("bAutoChapter"		, "0");
		//newdat.put("MagicChapter"		, "0");
		//newdat.put("CM_Chapter"			, "0");
		newdat.put("add_ch_text"		, "");
		newdat.put("add_ch_value"		, "");
		//newdat.put("sport_ext_submit"	, "undefined");
		//newdat.put("title_link_submit"	, "undefined");
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
}
