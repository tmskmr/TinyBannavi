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
import tainavi.HDDRecorder.RecType;
import tainavi.HDDRecorderUtils.MyAuthenticator;
import tainavi.TVProgram.ProgGenre;

/*
 * 
 */

public class PlugIn_RecRD_A600 extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_A600 clone() {
		return (PlugIn_RecRD_A600) super.clone();
	}

	private static final String thisEncoding = "MS932";

	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "VARDIA RD-A600"; }
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
		
		errmsg = "";
		
		int newBC = 0;			// 新しい放送の種類
		int newEnc = 0;			// 新しいエンコーダ
		String newChNo = "";	// 新しいチャンネル番号
		String ch = cc.getCH_WEB2REC(Channel);

		// 新しい放送の種類とチャンネル番号とエンコーダ
		Matcher ma = null;
		ma = Pattern.compile("^CH(\\d+)").matcher(ch); // CHxxで良いかどうか自信なし
		if (ma.find()) {
			newBC = 0; // 地上アナログ
			newChNo = ma.group(1);
			newEnc = 0; // RE
		}
		else {
			ma = Pattern.compile("^SP(\\d+)").matcher(ch);
			if (ma.find()) {
				newBC = 1; // 外部入力(スカパー！無印)
				newChNo = /*ma.group(1);*/"3"; // 外部入力L3まで
				newEnc = 0; // RE
			}
			else {
				ma = Pattern.compile("^BS(\\d+)").matcher(ch);
				if (ma.find()) {
					newBC = 2; // BS
					newChNo = ma.group(1);
					newEnc = 1; // TS1
				}
				else {
					ma = Pattern.compile("^CS(\\d+)").matcher(ch);
					if (ma.find()) {
						newBC = 3; // 110CS
						newChNo = ma.group(1);
						newEnc = 1; // TS1
					}
					else {
						ma = Pattern.compile("^(\\d+)").matcher(ch);
						if (ma.find()) {
							newBC = 4; // 地上デジタル
							newChNo = ma.group(1);
							newEnc = 1; // TS1
						}
					}
				}
			}
		}

		System.out.println("Change to【" + Channel + "】: (new)enc = " + newEnc + ", (new)BC = " + newBC + ", (new)ChNo = " + newChNo);

		int tryCntEnc  = 0; // エンコーダ変更試行回数
		int tryCntBC   = 0; // 放送の種類変更試行回数
		int tryCntChNo = 0; // チャンネル番号変更試行回数
		//String s;

		while (gs.getCurChannel(getIPAddr()) != null) {
			// 試行回数チェック
			if (10 < tryCntEnc || 10 < tryCntBC || 10 < tryCntChNo) {
				System.out.println(errmsg = "チャンネル切り替え失敗！");
				return false;
			}
			
			int curBC = 0;			// 選択中の放送の種類
			int curEnc = 0;			// 選択中のエンコーダ
			String curChNo = "";	// 選択中のチャンネル番号

			// 選択中の放送の種類とチャンネル番号
			ma = Pattern.compile("^CH (\\d+)").matcher(gs.ch);
			if (ma.find()) {
				curBC = 0; // 地上アナログ
				curChNo = ma.group(1);
			}
			else {
				ma = Pattern.compile("^L (\\d)").matcher(gs.ch);
				if (ma.find()) {
					curBC = 1; // 外部入力
					curChNo = ma.group(1);
				}
				else {
					ma = Pattern.compile("^BS(\\d+)").matcher(gs.ch);
					if (ma.find()) {
						curBC = 2; // BS
						curChNo = ma.group(1);
					}
					else {
						ma = Pattern.compile("^CS(\\d+)").matcher(gs.ch);
						if (ma.find()) {
							curBC = 3; // 110CS
							curChNo = ma.group(1);
						}
						else {
							ma = Pattern.compile("^(\\d+)").matcher(gs.ch);
							if (ma.find()) {
								curBC = 4; // 地上デジタル
								curChNo = ma.group(1);
							}
						}
					}
				}
			}
			
			// 選択中のエンコーダ
			if (gs.enc.equals("RE")) {
				curEnc = 0; // RE
			}
			else if (gs.enc.equals("TS1")) {
				curEnc = 1; // TS1
			}
			else if (gs.enc.equals("TS2")) {
				curEnc = 2; // TS2
			}

			System.out.println("Change to【" + Channel + "】: enc = " + curEnc + ", BC = " + curBC + ", ChNo = " + curChNo);

			// エンコーダの変更
			if (curEnc != newEnc) {
				// おまじない
				Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));

				reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=63", null); // W録
				CommonUtils.milSleep(2000);

				tryCntEnc++;
				
				continue;
			}

			// 放送の種類の変更
			if (curBC != newBC) {
				// おまじない
				Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
				
				// TS1またはTS2
				if (newEnc == 1 || newEnc == 2) {
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=21", null); // 放送切換
					CommonUtils.milSleep(2000);
				}
				// RE
				else {
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0F", null); // 入力切換
					CommonUtils.milSleep(2000);
				}

				tryCntBC++;
				
				continue;
			}
			
			// チャンネル番号の変更
			if (!curChNo.equals(newChNo)) {
				// おまじない
				Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
				
				// 地上アナログ BS 110CS 地上デジタル
				if (newBC == 0 || newBC == 2 || newBC == 3 || newBC == 4) {
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=25", null); // CH番号入力
					CommonUtils.milSleep(1000);
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0"+newChNo.substring(0,1), null); // 数字
					CommonUtils.milSleep(200);
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0"+newChNo.substring(1,2), null); // 数字
					CommonUtils.milSleep(200);
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0"+newChNo.substring(2,3), null); // 数字
					CommonUtils.milSleep(200);
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=44", null); // 決定
					CommonUtils.milSleep(5000);
				}
				else {
					reqGET("http://"+getIPAddr()+":"+getPortNo()+"/remote/remote.htm?key=0F", null); // 入力切換
					CommonUtils.milSleep(2000);
				}

				tryCntChNo++;
				
				continue;
			}
			
			System.out.println("Change to【" + Channel + "】: done");
			break;
		}
		
		return true;
	}

	/*
	 *	レコーダーから予約一覧を取得する 
	 */
	public boolean GetRdReserve(boolean force)
	{
		System.out.println("レコーダから予約一覧を取得します("+force+")");
		
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
			if ( vrate.size() > 0 ) {
				System.out.println("+video rate="+vrateTFile);
			}
			arate = TVSload(arateTFile);
			if ( arate.size() > 0 ) {
				System.out.println("+audio rate="+arateTFile);
			}
			folder = TVSload(folderTFile);
			if ( folder.size() > 0 ) {
				System.out.println("+folder="+folderTFile);
			}
			encoder = TVSload(encoderTFile);
			if ( encoder.size() > 0 ) {
				System.out.println("+encoder="+encoderTFile);
			}
			dvdcompat = TVSload(dvdcompatTFile);
			if ( dvdcompat.size() > 0 ) {
				System.out.println("+dvdcompat="+dvdcompatTFile);
			}
			device = TVSload(deviceTFile);
			if ( device.size() > 0 ) {
				System.out.println("+device="+deviceTFile);
			}
			// 自動チャプタ関連
			xchapter = TVSload(xChapterTFile);
			if ( xchapter.size() > 0 ) {
				System.out.println("+xchapter="+xChapterTFile);
			}
			mschapter = TVSload(msChapterTFile);
			if ( mschapter.size() > 0 ) {
				System.out.println("+mschapter="+msChapterTFile);
			}
			mvchapter = TVSload(mvChapterTFile);
			if ( mvchapter.size() > 0 ) {
				System.out.println("+mvchapter="+mvChapterTFile);
			}
			// チャンネルコードバリュー
			chvalue = TVSload(chValueTFile);
			if ( chvalue.size() > 0 ) {
				System.out.println("+chvalue="+chValueTFile);
			}
			chtype = TVSload(chTypeTFile);
			if ( chtype.size() > 0 ) {
				System.out.println("+chtype="+chTypeTFile);
			}
			
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && arate.size()>0 &&
					folder.size()>0 && encoder.size()>0 && device.size()>0 && // add >>> device
					xchapter.size()>0 && mschapter.size()>0 && mvchapter.size()>0) {
				return(true);
			}
			
			//getReserves().removeAll(getReserves());
		}

		// おまじない
		Authenticator.setDefault(new MyAuthenticator(getUser(), getPasswd()));
		
		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header="";
		String response="";
		{
			reportProgress("get reserved list(1/5).");
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
			reportProgress("get reserved list(2/5).");
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
			reportProgress("get reserved list(3/5).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/b_rsvinfo.htm?0&"+(RsvCnt+1),null);
		}
		{
			reportProgress("get reserved list(4/5).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve/"+idx+"/rsvinfo.htm",null);
			String hdr = d[0];
			String res = d[1];
			Matcher mb = null;
			
			// 正常取得チェック
			if ( ! res.matches("[\\s\\S]*var hdd_folder_text[\\s\\S]*")) {
				errmsg = "レコーダーからの情報取得に失敗しました（録画設定）";
				return false;
			}
			
			// (1-3)フォルダ一覧
			folder.clear();
			
			mb = Pattern.compile("var hdd_folder_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					folder.add(t);
				}
			}
			mb = Pattern.compile("var hdd_folder_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				for (TextValueSet t : folder) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
			
			TVSsave(folder, folderTFile);
			
			// (1-4)エンコーダ
			encoder.clear();
			
			mb = Pattern.compile("var double_encode_flg = (\\d+?);").matcher(res);
			while (mb.find()) {
				Matcher mc = Pattern.compile("\\n\\s*?switch \\( double_encode_flg \\) \\{([\\s\\S]+?default:)").matcher(res);
				if (mc.find()) {
					Matcher md = Pattern.compile("(case "+mb.group(1)+":[\\s\\S]+?break;)").matcher(mc.group(1));
					if (md.find()) {
						//System.out.println(md.group(1));
						Matcher me = Pattern.compile("name=enc_type value=.\"(\\d+?).\"[\\s\\S]+?toru_(.+?)\\.gif").matcher(md.group(1));
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
			dvdcompat.clear();
			
			mb = Pattern.compile("var dvdr_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					dvdcompat.add(t);
				}
			}
			mb = Pattern.compile("var dvdr_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				for (TextValueSet t : dvdcompat) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
			
			TVSsave(dvdcompat, dvdcompatTFile);

			// (1-6)記録先デバイス
			device.clear();
			
			mb = Pattern.compile("var media_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					device.add(t);
				}
			}
			mb = Pattern.compile("var media_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("(\\d+),?").matcher(mb.group(1));
				for (TextValueSet t : device) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
			
			TVSsave(device, deviceTFile);

			// (1-7)自動チャプター関連
			xchapter.clear();
			mb = Pattern.compile("var mutechapter_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					xchapter.add(t);
				}
			}
			mb = Pattern.compile("var mutechapter_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("(\\d+),?").matcher(mb.group(1));
				for (TextValueSet t : xchapter) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
			TVSsave(xchapter, xChapterTFile);
			
			mschapter.clear();
			mb = Pattern.compile("var magicchapter_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					mschapter.add(t);
				}
			}
			mb = Pattern.compile("var magicchapter_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("(\\d+),?").matcher(mb.group(1));
				for (TextValueSet t : mschapter) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
			TVSsave(mschapter, msChapterTFile);

			mvchapter.clear();
			mb = Pattern.compile("var cmchapter_text\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("\"(.+?)\",?").matcher(mb.group(1));
				while (mc.find()) {
					TextValueSet t = new TextValueSet();
					t.setText(mc.group(1));
					mvchapter.add(t);
				}
			}
			mb = Pattern.compile("var cmchapter_value\\s*= new Array\\(([\\s\\S]+?)\\);").matcher(res);
			if (mb.find()) {
				Matcher mc = Pattern.compile("(\\d+),?").matcher(mb.group(1));
				for (TextValueSet t : mvchapter) {
					if (mc.find()) {
						t.setValue(mc.group(1));
					}
				}
			}
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
		{
			reportProgress("get reserved list(5/5).");
			/*
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/cmn/script_function.htm",null);
			String hdr = d[0];
			String res = d[1];
			Matcher mb = null;
			*/
			
			// (1-1)画質設定
			{
				vrate.clear();
				TextValueSet t = null;
				
				t = new TextValueSet();
				t.setText("[TS]");
				t.setValue("128:");
				vrate.add(t);
				
				t = new TextValueSet();
				t.setText("[TSE] 1.0");
				t.setValue("2:1000");
				vrate.add(t);
				t = new TextValueSet();
				t.setText("[TSE] 1.4");
				t.setValue("2:1400");
				vrate.add(t);
				for (int br=2000; br<=17000; ) {
					t = new TextValueSet();
					t.setText(String.format("[TSE] %d.%d", (br-br%1000)/1000, (br%1000)/100));
					t.setValue("2:"+String.valueOf(br));
					vrate.add(t);
					if (br < 10000) {
						br += 200;
					}
					else {
						br += 500;
					}
				}
				
				t = new TextValueSet();
				t.setText("[VR] SP4.4/4.6");
				t.setValue("1:1");
				vrate.add(t);
				t = new TextValueSet();
				t.setText("[VR] LP2.0/2.2");
				t.setValue("1:2");
				vrate.add(t);
				t = new TextValueSet();
				t.setText("[VR] 1.0");
				t.setValue("1:1000");
				vrate.add(t);
				t = new TextValueSet();
				t.setText("[VR] 1.4");
				t.setValue("1:1400");
				vrate.add(t);
				for (int br=2000; br<=9200; br+=200) {
					t = new TextValueSet();
					t.setText(String.format("[VR] %d.%d", (br-br%1000)/1000, (br%1000)/100));
					t.setValue("1:"+String.valueOf(br));
					vrate.add(t);
				}
				t = new TextValueSet();
				t.setText("[VR] 高レート節約");
				t.setValue("1:10");
				vrate.add(t);
				
				TVSsave(vrate, vrateTFile);
			}
			
			// (1-2)音質設定
			{
				arate.clear();
				TextValueSet t = null;
				
				t = new TextValueSet();
				t.setText("M1");
				t.setValue("1");
				arate.add(t);
				t = new TextValueSet();
				t.setText("M2");
				t.setValue("2");
				arate.add(t);
				t = new TextValueSet();
				t.setText("L-PCM");
				t.setValue("3");
				arate.add(t);
				
				TVSsave(arate, arateTFile);
			}
		}
		
		// 予約一覧データの分析
		ArrayList<ReserveList> ra = decodeReservedList(response); 
		for (ReserveList entry : ra) {
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
			
			// タイトル自動補完フラグなど本体からは取得できない情報を引き継ぐ
			for ( ReserveList e : getReserves() ) {
				if ( e.getId().equals(entry.getId()) ) {
					entry.setDetail(e.getDetail());
					entry.setAutocomplete(e.getAutocomplete());
					break;
				}
			}
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
		
		// 音質（TS/TSEでは音質の設定はできない）
		ma = Pattern.compile("^\\[TS").matcher(r.getRec_mode());
		if (ma.find()) {
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
		ma = Pattern.compile("^\\[TS").matcher(r.getRec_mode());
		if (ma.find()) {
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
			"videotype_digital",	// new
			"videomode_digital",	//new
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
			//"sport_ext",
			//"title_link",
			"add_ch_text",
			"add_ch_value",
			"sport_ext_submit",
			"title_link_submit",
			"end_form",
		};
		
		String pstr = "";
		for ( String key : pkeys ) {
			//$pdat{"$key"} =~ s/(\W)/'%'.unpack("H2", $1)/ego;
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

		// 予約名・予約詳細
		if ( r.getAutocomplete() ) {
			// RDが勝手につけるので空白
			// リピート予約の場合は空白が必須
			// 空白にしない場合はリピート予約の巡回時に予約名が固定になるので
			// たとえば見ため的には「侵略！イカ娘 #1」の#1が#2に更新されない動作になる(左記動作はA600で確認済み)
			// 録画時間1分前倒しと1分短縮のときも正しい予約名になるのを確認済み
			newdat.put("title_name", "");
			newdat.put("detail", "");
		}
		else {
			try {
				newdat.put("title_name", URLEncoder.encode(r.getTitle(),thisEncoding));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			
			try {
				//newdat.put("detail", URLEncoder.encode(r.getDetail(), thisEncoding));
				// 改行を\r\nに変換する必要あり
				// 変換しない場合はA600のネットdeナビの詳細表示がバグる
				newdat.put("detail", URLEncoder.encode(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")), thisEncoding));
				//System.out.println(r.getDetail().replaceAll("\n", Matcher.quoteReplacement("\r\n")));
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
				else if ((ma.group(1)+ma.group(2)).equals("SP")) {
					newdat.put("broadcast","4"/*L3と同等*/);		// CSアナログ
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
		
		Matcher ma = Pattern.compile("^(\\d+?):(.*?)$").matcher(text2value(vrate, r.getRec_mode()));
		if (ma.find()) {
			if (ma.group(1).equals("1")) {
				// VR
				newdat.put("vrate",ma.group(2));	
				newdat.put("amode",text2value(arate, r.getRec_audio()));	
				newdat.put("videotype_digital",ma.group(1));	
				newdat.put("videomode_digital",ma.group(2));
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
		//newdat.put("sport_ext"			, "1");
		//newdat.put("title_link"			, "1"); // 追っかけ有効 (有効にならない？->ブラウザからもデフォルトで無効)
		newdat.put("auto_delete"		, "0");
		newdat.put("video_es"			, "00");
		newdat.put("audio_es"			, "10");
		newdat.put("edge_left"			, "0");		// 録画のりしろ
		//newdat.put("MagicChapter"		, "0");
		//newdat.put("MagicChapter"		, "0");
		//newdat.put("CM_Chapter"			, "0");
		newdat.put("add_ch_text"		, "");
		newdat.put("add_ch_value"		, "");
		newdat.put("sport_ext_submit"	, "undefined");
		newdat.put("title_link_submit"	, "undefined");
		//newdat.put("sport_ext_submit"	, "1");
		//newdat.put("title_link_submit"	, "1"); // 追っかけ有効 (有効にならない？->ブラウザからもデフォルトで無効)
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
}
