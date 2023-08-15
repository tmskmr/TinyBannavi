package tainavi.pluginrec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;


public class PlugIn_RecRD_S1004K extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_S1004K clone() {
		return (PlugIn_RecRD_S1004K) super.clone();
	}

	//private static final String thisEncoding = "MS932";
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "VARDIA RD-S1004K"; }
	public RecType getType() { return RecType.RECORDER; }
	
	// 個体の特性
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
	 * 
	 */
	public boolean ChangeChannel(String Channel) {
		
		if (Channel == null) {
			return true;
		}
		
		String ch = null;
		int curBC;
		int newBC;
		String chNo;
		
		errmsg = "";
		
		/*
		 * 変更前の放送（地上波／ＢＳ／ＣＳ）
		 */
		
		for (int i=0; i<3; i++) {
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/getcurchannel.htm", null);
			if ((ch = d[1]) != null) {
				break;
			}
			CommonUtils.milSleep(500);
		}
		if (ch == null || ch.startsWith("null")) {
			errmsg = "レコーダへのアクセスに失敗しました(チャンネルリモコン)。";
			System.err.println(errmsg);
			return false;
		}
		
		byte[] ba = ch.getBytes();
		byte enc = ba[0];
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
		
		/*
		 * 変更後のＣＨ
		 */
		
		// 放送（地上波／ＢＳ／ＣＳ）
		ch = cc.getCH_WEB2REC(Channel);
		if (ch.startsWith("SP")) {
			errmsg = "外部入力にアサインされているチャンネルには変更できません("+Channel+", "+ch+")。";
			System.err.println(errmsg);
			return false;
		}
		else if (ch.matches("^\\d.+")) {
			newBC = 0;
		}
		else if (ch.startsWith("BS")) {
			newBC = 1;
		}
		else if (ch.startsWith("CS")) {
			newBC = 2;
		}
		else {
			errmsg = "放送種別が識別できません。プログラム異常かも？("+Channel+", "+ch+")。";
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
		
		/*
		 * ＣＨ変更実行
		 */
		
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
	public boolean GetRdReserve(boolean force) {
		
		errmsg = "";
		
		System.out.println("Run: GetRdReserve("+force+")");
		
		// 録画設定
		if (force == true || encoder.size() == 0) {
			FileReader fr = null;
			BufferedReader r = null;
			try {
				encoder.clear();
				vrate.clear();
				arate.clear();
				device.clear();
				dvdcompat.clear();
				xchapter.clear();
				mschapter.clear();
				mvchapter.clear();
				
				String defFile = "env/mail.def" ;
				fr = new FileReader(defFile);
				r = new BufferedReader(fr);
				String s ;
				while ( (s = r.readLine()) != null ) {
					String[] b = s.split(",");
					if ( b.length >= 3 ) {
						TextValueSet t = new TextValueSet() ;
						t.setText(b[1]) ;
						t.setValue(b[2]) ;
						
						if ( b[0].equals("7") ) {
							encoder.add(t) ;
						}
						else if ( b[0].equals("10") ) {
							vrate.add(t) ;
						}
						else if ( b[0].equals("11") ) {
							arate.add(t) ;
						}
						else if ( b[0].equals("12") ) {
							device.add(t) ;
						}
						else if ( b[0].equals("14") ) {
							dvdcompat.add(t) ;
						}
						else if ( b[0].equals("17") ) {
							xchapter.add(t) ;
						}
						else if ( b[0].equals("18") ) {
							mschapter.add(t) ;
						}
						else if ( b[0].equals("19") ) {
							mvchapter.add(t) ;
						}
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally {
				if (r != null) try { r.close(); } catch (Exception e) {};
				if (fr != null) try { fr.close(); } catch (Exception e) {};
			}
		}
		
		
		// 予約一覧をキャッシュから
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			return(true);
		}
		
		// 予約一覧をレコーダーから取得する
		reportProgress("get reserved list(1/1).");
		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/reserve.htm",null);
		String header = d[0];
		String response = d[1];
		if (response == null) {
			errmsg = "レコーダーが反応しません";
			return(false);
		}
		
		// 取得したデータを内部形式に変換する
		ArrayList<ReserveList> ra = decodeReservedList(response); 
		for (ReserveList entry : ra) {
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
			
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
		
		return(true);
	}
	
	/*
	 *	予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r) {
		
		errmsg = "";
		
		// 設定もれを撥ねる
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");
		
		// ポストデータを作る
		r.setId(null);
		String pstr = getPoststr(r);
		
		// リクエストを送る
		reportProgress("send request.(1/1)");
		String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/entry.htm", pstr, null);
		String header = d[0];
		String response = d[1];
		if ( response == null ) {
			errmsg = "レコーダーが反応しません。";
			return(false);
		}
		
		/*
		 * 予約情報の調整 
		 */
		
		// 予約ID番号を取得
		Matcher ma = Pattern.compile("^(\\d+)").matcher(response);
		if (ma.find()) {
			r.setId(ma.group(1));
		}
		
		// 音質（TS/TSEでは音質の設定はできない）
		if (r.getRec_mode().indexOf("[TS") == 0) {
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
		
		errmsg = "";
		
		// 設定もれを撥ねる
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: UpdateRdEntry()");
		
		// ポストデータを作る
		String pstr = getPoststr(r);
		
		// リクエストを送る
		reportProgress("send request.(1/1)");
		String[] d = reqPOST("http://"+getIPAddr()+":"+getPortNo()+"/update.htm", pstr, null);
		String header = d[0];
		String response = d[1];
		if ( response == null ) {
			errmsg = "レコーダーが反応しません。";
			return(false);
		}
		
		/*
		 * 予約情報の調整 
		 */
		
		// 音質（TS/TSEでは音質の設定はできない）
		if (r.getRec_mode().indexOf("[TS") == 0) {
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
		
		// 予約リストを更新
		getReserves().remove(o);
		getReserves().add(r);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に登録できました。");
		return(true);
	}
	
	/*
	 *	予約を削除する
	 */
	public ReserveList RemoveRdEntry(String delid) {
		
		errmsg = "";
		
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
		
		// リクエストを送る
		reportProgress("send request.(1/1).");
		String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/delete.htm?id="+rx.getId(), null);
		String header = d[0];
		String response = d[1];
		if ( response == null ) {
			errmsg = "レコーダーが反応しません。";
			return(null);
		}
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");
		return(rx);
	}
	
	//
	public String getErrmsg() {
		return(errmsg);
	}
	
	
	
	//
	private String getPoststr(ReserveList r) {
		
		StringBuilder sb = new StringBuilder();
		
		try {
			String exec		= (r.getExec())?("ON"):("OFF");
			String title	= URLEncoder.encode(r.getTitle(), "UTF-8");
			String ch_code	= cc.getCH_WEB2CODE(r.getCh_name());
			String channel	= cc.getCH_CODE2REC(ch_code);
			String pattern	= URLEncoder.encode(r.getRec_pattern(), "UTF-8");
			String dvdr		= URLEncoder.encode(r.getRec_dvdcompat(), "UTF-8");
			String xchap	= URLEncoder.encode(r.getRec_xchapter(), "UTF-8");
			String mschap	= URLEncoder.encode(r.getRec_mschapter(), "UTF-8");
			String mvchap	= URLEncoder.encode(r.getRec_mvchapter(), "UTF-8");
			String autocomp = (r.getAutocomplete())?("ON"):("OFF");
			
			if (r.getId() != null) sb.append("id="+r.getId()+"&");
			
			sb.append("exec="+exec+"&");
			sb.append("title="+title+"&");
			sb.append("ch_code="+ch_code+"&");
			sb.append("channel="+channel+"&");
			sb.append("pattern="+pattern+"&");
			sb.append("ahh="+r.getAhh()+"&");
			sb.append("amm="+r.getAmm()+"&");
			sb.append("zhh="+r.getZhh()+"&");
			sb.append("zmm="+r.getZmm()+"&");
			sb.append("tuner="+r.getTuner()+"&");
			sb.append("video="+r.getRec_mode()+"&");
			sb.append("audio="+r.getRec_audio()+"&");
			sb.append("disc="+r.getRec_device()+"&");
			sb.append("dvdr="+dvdr+"&");
			sb.append("xchapter="+xchap+"&");
			sb.append("mschapter="+mschap+"&");
			sb.append("mvchapter="+mvchap+"&");
			sb.append("autocomp="+autocomp+"&");
			sb.append("\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return(sb.toString());
	}
}
