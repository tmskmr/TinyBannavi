package tainavi.pluginrec;

import java.io.File;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TraceProgram;
import tainavi.HDDRecorderUtils.MyAuthenticator;

/*
 * 
 */

public class PlugIn_RecRD_H2 extends PlugIn_RecRD_X5 implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_H2 clone() {
		return (PlugIn_RecRD_H2) super.clone();
	}
	
	//private static final String thisEncoding = "MS932";
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "RD-H2"; }
	
	/* ここまで */
	
	
	
	/* 個別コード－ここから最後まで */
	
	/*
	 * 公開メソッド 
	 */
	
	/*
	 *	レコーダーから予約一覧を取得する 
	 */
	public boolean GetRdReserve(boolean force)
	{
		System.out.println("Run: GetRdReserve("+force+")");
		
		setErrmsg("");
		
		//
		setRsvedFile("env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml");
		
		String vrateTFile = "env/videorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String arateTFile = "env/audiorate."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String folderTFile = "env/folders."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String dvdcompatTFile = "env/dvdcompat."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String xChapterTFile = "env/xchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String mvChapterTFile = "env/mvchapter."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String chValueTFile = "env/chvalue."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String bvperfTFile = "env/bvperf."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		String lvoiceTFile = "env/lvoice."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		File f = new File(getRsvedFile());
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（録画設定ほか）
			vrate = TVSload(vrateTFile);
			arate = TVSload(arateTFile);
			folder = TVSload(folderTFile);
			dvdcompat = TVSload(dvdcompatTFile);
			
			// 自動チャプタ関連
			xchapter = TVSload(xChapterTFile);
			mvchapter = TVSload(mvChapterTFile);
			
			// その他
			bvperf = TVSload(bvperfTFile);
			lvoice = TVSload(lvoiceTFile);
			
			// チャンネルコードバリュー
			chvalue = TVSload(chValueTFile);
			
			// キャッシュから読み出し（予約一覧）
			setReservesV1(ReservesFromFile(getRsvedFile()));
			
			// なぜか設定ファイルが空になっている場合があるので、その際は再取得する
			if (vrate.size()>0 && arate.size()>0 &&
					folder.size()>0 &&
					xchapter.size()>0 && mvchapter.size()>0) {
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
			reportProgress("処理IDを取得します(1/3).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/b_prgrm.htm",null);
			response = d[1];
			
			if (response == null) {
				setErrmsg("レコーダが応答しません(処理ID).");
				return(false);
			}
		}
		ma = Pattern.compile("/program/(\\d+?)/program.htm").matcher(response);
		if ( ! ma.find()) {
			setErrmsg("レコーダからの戻り値が不正です(処理ID).");
			return(false);
		}
		
		idx = ma.group(1);	// 処理ID
		
		{
			reportProgress("予約一覧を取得します(2/3).");
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/program.htm",null);
			response = d[1];
			
			if (response == null) {
				setErrmsg("レコーダが応答しません(予約一覧).");
				return(false);
			}
		}
		
		// (1)録画設定の取得
		{
			// ハングさせないためのおまじない
			reportProgress("録画設定を取得します(3/3).");
			reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/b_proginfo.htm?0",null);
			
			String[] d = reqGET("http://"+getIPAddr()+":"+getPortNo()+"/program/"+idx+"/proginfo.htm",null);
			String res = d[1];
			
			if (res == null) {
				setErrmsg("レコーダが応答しません(録画設定).");
				return(false);
			}
			
			//Matcher mb = null;
			
			// (1-1)画質設定
			setSettingEtc(vrate, "\"vrate\"", 1, res, vrateTFile);
			
			// (1-2)音質設定
			setSettingEtc(arate, "amode", 0, res, arateTFile);
			
			// (1-3)フォルダ一覧
			setSettingFolder(folder,res,folderTFile);
			
			// (1-4)エンコーダ [RD-H2にはないよ]
			
			// (1-5)DVD互換モード
			setSettingEtc(dvdcompat, "dvdr", 1, res, dvdcompatTFile);
			
			// (1-6)記録先デバイス [RD-H2にはないよ]
			
			// (1-7)自動チャプタ関連
			setSettingEtc(xchapter, "bAutoChapter", 0, res, xChapterTFile);
			
			// bDvdAutoChapter [RD-H2にはないよ]
			
			setSettingEtc(mvchapter, "bAudioAutoChapter", 0, res, mvChapterTFile);
			
			// (1-8)チャンネルコードバリュー
			setSettingEtc(chvalue, "channel", 1, res, chValueTFile);
			
			// (1-10)記録時画面比 [RD-H2にはないよ]
			
			// (1-11)高レート節約
			setSettingEtc(bvperf, "bVPerform", 0, res, bvperfTFile);
			
			// (1-12)ライン音声選択
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
		ReservesToFile(getReserves(), getRsvedFile());
		
		return(true);
	}
	
	/*
	 * 非公開メソッド 
	 */
	
	//
	@Override
	protected ArrayList<ReserveList> GetRdReservedList(String response) {
		
		System.out.println("H2's GetRdReservedList()");
		//
		response = response.replaceAll("\n", "");

		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();

		Matcher ma = Pattern.compile("<tr [^>]*?>([\\s\\S]*?)</tr>").matcher(response);
		while (ma.find()) {

			// 個々のデータを取り出す
			String buf = ma.group(1);

			Matcher mb = null;
			mb = Pattern.compile(">新規予約<").matcher(buf);
			if (mb.find()) {
				break;
			}
			
			ReserveList entry = new ReserveList();
			
			/*
			 * 0 : ID
			 * 1 : 実行ON/OFF
			 * 2 : タイトル
			 * 3 : 放送局
			 * 4 : 日付
			 * 5 : 時刻
			 * 6 : デバイス
			 * 7 : 画質
			 * 8 : 音質
			 */
			String[] d = new String[9];
			mb = Pattern.compile("<(td|TD).*?>(.*?)</(td|TD)>").matcher(buf);
			for (int i=0; i<d.length; i++) {
				if ( mb.find()) {
					d[i] = mb.group(2);
				}
				//System.out.println(i+") "+d[i]);
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
			entry.setRec_min(CommonUtils.getRecMin(entry.getAhh(), entry.getAmm(), entry.getZhh(), entry.getZmm()));
			getStartEndDateTime(entry);
			
			entry.setChannel(d[3]);
			entry.setCh_name(getChCode().getCH_REC2WEB(entry.getChannel()));
			
			entry.setRec_audio(d[8]);
			entry.setRec_mode(d[7]);
			
			// 予約一覧からはとれない [本体で予約名や時刻を変えてしまうとアウト]
			/* setReservesV1()内に移動 */
			
			// 予約情報を保存
			newReserveList.add(entry);
		}
		
		return(newReserveList);
	}
	
	//
	@Override
	protected String joinPoststr(Hashtable<String, String> pdat)
	{
		System.out.println("H2's joinPoststr()");
		
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
			"disc",
			"vrate",
			"amode",
			"folder",				// for X5
			"dvdr",
			"lVoice",				// for X5
			"bVPerform",
			"bAutoChapter",
			"bAudioAutoChapter",	// for X5
			"detail",
			"dtv_sid",
			"dtv_nid",
			"net_link",				// for X5
			"add_ch_text",			// for H2
			"add_ch_value",			// for H2
			"end_form"
		};
		
		String pstr = "";
		for ( String key : pkeys ) {
			if ( ! pdat.containsKey(key)) {
				pstr += key+"=&";
			}
			else {
				pstr += key+"="+pdat.get(key)+"&";
			}
		}
		pstr = pstr.substring(0, pstr.length()-1);
		
		return(pstr);
	}
}
