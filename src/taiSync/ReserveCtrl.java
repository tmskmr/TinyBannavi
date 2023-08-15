package taiSync;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.CommonUtils;
import tainavi.RecorderList;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;

public class ReserveCtrl extends HDDRecorderUtils {

	private ArrayList<ReqCtrl> reqCtrl = null;
	
	// コンストラクタ
	public ReserveCtrl() {
		this.reqCtrl = new ArrayList<ReqCtrl>();
		File f = new File("env");
		if (f.exists() && f.isDirectory()) {
			for (String fn : f.list()) {
				if (fn.matches("^mail.*\\.def$")) {
					this.reqCtrl.add(new ReqCtrl(String.format("env%s%s",File.separator,fn)));
				}
			}
		}
	}
	
	
	
	// 不要になりました予約エントリを削除する
	public void removeEmptyReserveInfo(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		for (int i=rsvInfo.size()-1; i>=0; i--) {
			ReserveInfo rInfo = rsvInfo.get(i);
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if (rInfo.getId().equals("")) {
				rsvInfo.remove(i);
			}
		}
	}
	
	// レコーダーからの予約済み一覧情報を反映するに当たって既存の日時情報をクリアする
	private void clearReserveStartEnd(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			rInfo.getStarts().clear();
			rInfo.getEnds().clear();
			rInfo.getRecIds().clear();
			
			CommonUtils.getStartEndList(rInfo.getStarts(), rInfo.getEnds(), rInfo);
			for (int i=0; i<rInfo.getStarts().size(); i++) {
				rInfo.getRecIds().add(null);
			}
		}
	}
	
	// 予約実行日時が近づいているものを警告する
	public boolean isThereUnprocessing(RecorderList rec, ArrayList<ReserveInfo> rsvInfo, int keeping) {
		String currentDateTime = CommonUtils.getDateTime(0);
		String borderDateTime = CommonUtils.getDateTime(16*60);
		String keepingDateTime = CommonUtils.getDateTime(keeping*86400);
		boolean flag = false;
		for (ReserveInfo rInfo : rsvInfo) {
			if ( rec != null && ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if (rInfo.getId().equals("")) {
				// 削除系
				for (int i=0; i<rInfo.getRecIds().size(); i++) {
					if (rInfo.getRecIds().get(i) != null) {
						if (rInfo.getStarts().get(i).compareTo(borderDateTime) < 0) {
							if (rInfo.getStarts().get(i).compareTo(currentDateTime) < 0) {
								System.out.println("削除の期限が過ぎてしまったため削除できませんでした："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
								rInfo.getRecIds().set(i, null);
							}
							else {
								System.out.println("録画開始時刻まで１５分を切っているため削除できません.："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
							}
						}
						else if (rInfo.getStarts().get(i).compareTo(keepingDateTime) < 0) {
							System.out.println("削除の期限が近づいています："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
							flag = true;
						}
						//
					}
				}
			}
			else {
				// 登録系
				if (rInfo.getExec() && rInfo.getByTainavi()) {
					for (int i=0; i<rInfo.getRecIds().size(); i++) {
						if (rInfo.getRecIds().get(i) == null) {
							if (rInfo.getStarts().get(i).compareTo(borderDateTime) < 0) {
								if (rInfo.getStarts().get(i).compareTo(currentDateTime) < 0) {
									System.out.println("予約の期限が過ぎてしまったため予約できませんでした："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
									rInfo.getRecIds().set(i, "-1");
								}
								else {
									System.out.println("録画開始時刻まで１５分を切っているため予約できません.："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
								}
							}
							else if (rInfo.getStarts().get(i).compareTo(keepingDateTime) < 0) {
								System.out.println("予約の期限が近づいています："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
								flag = true;
							}
						}
					}
				}
			}
		}
		return flag;
	}
	
	// clearReserveStartEnd()と同様だがレコーダーと同期済み(Idあり)のものは引き継ぐ
	public void refreshReserveStartEnd(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		for (int i=rsvInfo.size()-1; i>=0; i--) {
			ReserveInfo rInfo = rsvInfo.get(i);
			if ( rec != null && ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
    		String cur = CommonUtils.getDateTime(0);
			if (rInfo.getRec_pattern_id() == 11) {
				if (rInfo.getStartDateTime().compareTo(cur) < 0) {
					// 単日終了エントリの削除
					rsvInfo.remove(i);
				}
			}
			else if (rInfo.getId() != null && rInfo.getId().equals("")) {
				// レコーダに存在しないエントリの削除
				int cnt = 0;
				for (int j=0; j<rInfo.getRecIds().size(); j++) {
					if (rInfo.getEnds().get(j).compareTo(cur) < 0) {
						// 終了日時が過去のものは無効エントリにする
						rInfo.getRecIds().set(j,"-1");
					}
					String s = rInfo.getRecIds().get(j);
					if (s != null && ! s.equals("-1")) {
						cnt++;
					}
				}
				if (cnt == 0) {
					rsvInfo.remove(i);
				}
			}
			else {
				// 同期済み(Idあり)を引き継ぎつつ情報リセット
				ArrayList<String> starts = new ArrayList<String>();
				ArrayList<String> ends = new ArrayList<String>();
				ArrayList<String> ids = new ArrayList<String>();
				CommonUtils.getStartEndList(starts, ends, rInfo);
				
				if ( starts.size() == 0 ) {
					// 2012/11/10 Out of boundsへの臨時対処 -> なぜここに来るのかはちょっと原因がわからない -> getStartEndListのバグだった
					System.out.println("【致命的エラー】 開始／終了日時が判定できませんでした！このエントリは削除されます！ 現在日時="+cur+" パターン="+rInfo.getRec_pattern()+"("+rInfo.getRec_pattern_id()+") "+rInfo.getAhh()+":"+rInfo.getAmm()+"-"+rInfo.getZhh()+":"+rInfo.getZmm()+" タイトル="+rInfo.getTitle());
					rsvInfo.remove(i);
					continue;
				}
				
				if (rInfo.getByTainavi()) {
					// 鯛ナビからなされた予約エントリの場合
					for (int j=0; j<starts.size(); j++) {
						ids.add(null);
						for (int k=0; k<rInfo.getStarts().size(); k++) {
							if (starts.get(j).equals(rInfo.getStarts().get(k)) && ends.get(j).equals(rInfo.getEnds().get(k))) {
								ids.set(j,rInfo.getRecIds().get(k));
								break;
							}
						}
					}
				}
				else {
					// 本体からなされた予約エントリの場合
					for (int j=0; j<starts.size(); j++) {
						String id = (j==0)?(rInfo.getRecIds().get(0)):(null); 
						ids.add(id);
					}
				}
				rInfo.setStarts(starts);
				rInfo.setEnds(ends);
				rInfo.setRecIds(ids);
				// まあ特に使わない値だけど一応更新
				rInfo.setStartDateTime(rInfo.getStarts().get(0));
				rInfo.setEndDateTime(rInfo.getEnds().get(0));
			}
		}
		
		// ほぞんするお
		ReserveInfo.save(rsvInfo);
		ReserveInfo.showReserveInfo(rsvInfo,(rec!=null)?(rec.getRecorderIPAddr()):(null));
	}
	
	// ローカル予約IDを更新する
	private void setNewRsvId(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		long rsvId = 0;	// オーバーフローは考慮しない
		
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			if (rInfo.getId() != null && ! rInfo.getId().equals("") ) {
				if (Long.valueOf(rInfo.getId()) > rsvId) {
					rsvId = Long.valueOf(rInfo.getId());
				}
			}
		}
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			if (rInfo.getId() == null) {
				rInfo.setId(String.valueOf(++rsvId));
			}
		}
	}
	
	// レコーダーから予約済み一覧を取得する
	private ArrayList<ReserveList> getReservedList(RecorderList rec) {
		
		System.out.println("RD("+rec.getRecorderIPAddr()+")に接続します");
		
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(rec.getRecorderUser(), rec.getRecorderPasswd()));
		
		//　RDから予約一覧を取り出す
		Matcher ma = null;
		String idx = "";
		String header="";
		String response="";
		{
			String[] d = reqGET("http://"+rec.getRecorderIPAddr()+":"+rec.getRecorderPortNo()+"/reserve/b_rsv.htm",null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				System.out.println("レコーダーが反応しません.1("+rec.getRecorderIPAddr()+")");
				return(null);
			}
		}
		ma = Pattern.compile("/reserve/(\\d+?)/reserve.htm").matcher(response);
		if (ma.find()) {
			idx = ma.group(1);
			String[] d = reqGET("http://"+rec.getRecorderIPAddr()+":"+rec.getRecorderPortNo()+"/reserve/"+idx+"/reserve.htm",null);
			header = d[0];
			response = d[1];
			
			if (response == null) {
				System.out.println("レコーダーが反応しません.2("+rec.getRecorderIPAddr()+")");
				return(null);
			}
		}
		
		// 予約一覧データの分析
		return(decodeReservedList(response));
	}
	
	// レコーダーからの予約済み一覧情報と自前の一覧をマージする
	public void margeReservedInfo(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		
		// レコーダーから情報取得
		ArrayList<ReserveList> rsvList = getReservedList(rec);
		if (rsvList == null) {
			return;
		}
		
		// 予約一覧の日時情報をクリアする
		clearReserveStartEnd(rec, rsvInfo);
		
		// マージするよ？
		ArrayList<ReserveList> tmpList = new ArrayList<ReserveList>();
		
		for (ReserveList rList : rsvList) {
			if (rList.getRec_pattern_id() == 11) {
				// レコーダ情報が単日予約の場合
				boolean reserved = false;
				for (ReserveInfo rInfo : rsvInfo) {
					if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
						continue;
					}
					
					if (rInfo.getChannel().equals(rList.getChannel())) {
						if (rInfo.getRec_pattern_id() == 11) {
							// 単日予約
						 	if (rInfo.getRec_pattern().equals(rList.getRec_pattern())) {
								if (rInfo.getStarts().get(0).equals(rList.getStartDateTime()) && rInfo.getEnds().get(0).equals(rList.getEndDateTime())) {
									if (rInfo.getRecIds().get(0) == null) {
										rInfo.getRecIds().set(0, rList.getId());
										reserved = true;
										break;
									}
								}
							}
						}
						else {
							// 繰り返し予約
							for (int i=0; i<rInfo.getStarts().size(); i++) {
								if (rInfo.getStarts().get(i).equals(rList.getStartDateTime()) && rInfo.getEnds().get(i).equals(rList.getEndDateTime())) {
									if (rInfo.getRecIds().get(i) == null) {
										rInfo.getRecIds().set(i, rList.getId());
										reserved = true;
										break;
									}
								}
							}
						}
					}
					if (reserved) {
						break;
					}
				}
				if ( ! reserved) {
					tmpList.add(rList);
				}
			}
			else {
				// レコーダ情報が繰り返し予約の場合
				boolean reserved = false;
				for (ReserveInfo rInfo : rsvInfo) {
					if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
						continue;
					}
					
					if (rInfo.getChannel().equals(rList.getChannel())) {
						if (rInfo.getRec_pattern_id() == rList.getRec_pattern_id()) {
							if (rInfo.getAhh().equals(rList.getAhh()) && rInfo.getAmm().equals(rList.getAmm()) && rInfo.getZhh().equals(rList.getZhh()) && rInfo.getZmm().equals(rList.getZmm())) {
								if (rInfo.getRecIds().get(0) == null) {
									rInfo.getRecIds().set(0, rList.getId());
									reserved = true;
									break;
								}
							}
						}
					}
				}
				if ( ! reserved) {
					tmpList.add(rList);
				}
			}
		}
		for (ReserveList rList : tmpList) {
			ReserveInfo rInfo = new ReserveInfo(rList);
			rInfo.setByTainavi(false);
			rInfo.setIpaddr(rec.getRecorderIPAddr());
			rInfo.setPortno(rec.getRecorderPortNo());
			
			rsvInfo.add(rInfo);
			
			System.err.println("new rsvInfo: "+rInfo.getTitle());	// デバッグ
		}
		
		// 本体から予約したもので移動しちゃったものは削除しますよ
		for (int i=rsvInfo.size()-1; i>=0; i--) {
			ReserveInfo rInfo = rsvInfo.get(i);
			if (rInfo.getByTainavi()) {
				continue;
			}
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			int cnt = 0;
			for (String s : rInfo.getRecIds()) {
				if (s != null) {
					cnt++;
				}
			}
			if (cnt == 0) {
				rsvInfo.remove(i);
			}
		}
		
		// 新規エントリにtaiSyncローカルの予約IDを割り当てる
		if (tmpList.size() > 0) {
			setNewRsvId(rec, rsvInfo);
		}
	}
	
	// 予約一覧データを作成する
	public String getRsvStr(RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		
		margeReservedInfo(rec, rsvInfo);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<HTML>\n");
		sb.append("<HEAD><TITLE>タイニーシンク - "+rec.getRecorderIPAddr()+"</TITLE></HEAD>\n");
		sb.append("<BODY>\n");
		
		{
			sb.append("<!--\n");
			
			int i = 0;
			for (ReserveInfo rInfo : rsvInfo) {
				
				// 自分の分だけ
				if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
					continue;
				}
				
				// 削除済みとかね
				if (rInfo.getId() == null || rInfo.getId().equals("")) {
					continue;
				}
				
				Matcher ma = null;
				String str = null;
				sb.append("c1["+i+"]=\""+rInfo.getId()+"\";");
				sb.append("c2["+i+"]=\""+((rInfo.getExec())?("3"):("2"))+"\";");
				sb.append("c3["+i+"]=\""+rInfo.getTitle().replaceAll("\"", "&quot;")+"\";");
				if (rInfo.getTuner().equals("RE") || rInfo.getTuner().equals("RE1")) {
					str = "7";
				}
				else if (rInfo.getTuner().equals("TS2") || rInfo.getTuner().equals("RE2") || rInfo.getTuner().equals("R2")) {
					str = "9";
				}
				else {
					str = "11";	// TS1
				}
				sb.append("c4["+i+"]=\""+str+"\";");
				sb.append("c5["+i+"]=\""+rInfo.getChannel()+"\";");
				sb.append("c6["+i+"]=\""+rInfo.getRec_pattern()+"\";");
				sb.append("c7["+i+"]=\""+rInfo.getAhh()+":"+rInfo.getAmm()+"\";");
				sb.append("c8["+i+"]=\""+rInfo.getZhh()+":"+rInfo.getZmm()+"\";");
				sb.append("c9["+i+"]=\""+rInfo.getRec_device()+"\";");
				str = null;
				if (str == null) {
					ma = Pattern.compile("^\\[TSE\\] ").matcher(rInfo.getRec_mode());
					if (ma.find()) str = ma.replaceFirst("MN");
				}
				if (str == null) {
					ma = Pattern.compile("^\\[AVC\\] ").matcher(rInfo.getRec_mode());
					if (ma.find()) str = ma.replaceFirst("MN");
				}
				if (str == null) {
					ma = Pattern.compile("^\\[VR\\] 2\\.0/2\\.2$").matcher(rInfo.getRec_mode());
					if (ma.find()) str = ma.replaceFirst("LP2.0/2.2");
				}
				if (str == null) {
					ma = Pattern.compile("^\\[VR\\] 4\\.4/4\\.6$").matcher(rInfo.getRec_mode());
					if (ma.find()) str = ma.replaceFirst("SP4.4/4.6");
				}
				if (str == null) {
					ma = Pattern.compile("^\\[VR\\] ").matcher(rInfo.getRec_mode());
					if (ma.find()) str = ma.replaceFirst("MN");
				}
				if (str == null) {
					//System.err.println(rInfo.getRec_mode());
					str = "TS";
				}
				sb.append("c10["+i+"]=\""+str+"\";");
				sb.append("c11["+i+"]=\""+rInfo.getRec_audio()+"\";");
				sb.append("c14["+i+"]=\"0\";");
				sb.append("c15["+i+"]=\""+((rInfo.getPursues())?("4"):("1"))+"\";");
				sb.append("c16["+i+"]=\"1\";");
				sb.append("c17["+i+"]=\"2\";");
				sb.append("\n");
				i++;
			}
			sb.append("-->\n");
		}
		
		sb.append("<H2>タイニーシンク - "+rec.getRecorderIPAddr()+"</H2>");
		sb.append("<TABLE border=\"3\">\n");
		sb.append("<TR bgcolor=\"#DD3300\">\n");
		sb.append("<TH STYLE=\"width: 25px\">元</TH>\n");
		sb.append("<TH STYLE=\"width: 150px\">録画パターン</TH>\n");
		sb.append("<TH STYLE=\"width: 225px\">開始～終了（赤字は未反映、斜体は削除待ち）</TH>\n");
		sb.append("<TH STYLE=\"width: 50px\">長さ</TH>\n");
		sb.append("<TH STYLE=\"width: 50px\">ＣＨ</TH>\n");
		sb.append("<TH>タイトル</TH>\n");
		sb.append("</TR>\n");
		
		{
			// ソートする
			ArrayList<ReserveInfo> b = new ArrayList<ReserveInfo>();
			for (int i=0; i<rsvInfo.size(); i++) {
				// 自分の分だけ
				if ( ! rsvInfo.get(i).getIpaddr().equals(rec.getRecorderIPAddr())) {
					continue;
				}
				boolean f = true;
				for (int j=0; j<b.size(); j++) {
					if (b.get(j).getStartDateTime().compareTo(rsvInfo.get(i).getStartDateTime()) > 0) {
						b.add(j, rsvInfo.get(i));
						f = false;
						break;
					}
				}
				if (f) {
					b.add(rsvInfo.get(i));
				}
			}
			
			// 出力する
			for ( int i = 0; i<b.size(); i++ ) {
				// 詳細情報の取得
				ReserveInfo e = b.get(i);
				
				int wlen = 0;
				String ptn = e.getRec_pattern();
				Matcher ma = Pattern.compile("[^ -~｡-ﾟ]").matcher(ptn);
				while (ma.find()) {
					wlen++;
				}
				int ptnlen = 14-wlen;
				ptn = String.format(String.format("%%-%ds",ptnlen),ptn);
				
				String bgc = "bgcolor=\"#"+((i % 2 == 0)?("CCCCCC"):("FFFFFF"))+"\"";
				
				int row = e.getStarts().size();
				sb.append("<TR "+bgc+">");
				sb.append("<TD rowspan=\""+row+"\">"+((e.getByTainavi())?("鯛"):("本"))+"</TD>");
				sb.append("<TD rowspan=\""+row+"\">"+ptn+"</TD>");
				sb.append("<TD>"+getHTMLStartEnd(e.getExec(),e.getId(),e.getRecIds().get(0),e.getStarts().get(0),e.getEnds().get(0))+"</TD>");
				sb.append("<TD rowspan=\""+row+"\">"+e.getRec_min()+"m</TD>");
				sb.append("<TD rowspan=\""+row+"\">"+e.getChannel()+"</TD>");
				sb.append("<TD rowspan=\""+row+"\">"+e.getTitle()+"</TD>");
				sb.append("</TR>\n");

				for (int j=1; j<e.getStarts().size(); j++) {
					sb.append("<TR "+bgc+">");
					sb.append("<TD>"+getHTMLStartEnd(e.getExec(),e.getId(),e.getRecIds().get(j),e.getStarts().get(j),e.getEnds().get(j))+"</TD>");
					sb.append("</TR>\n");
				}
			}
		}
		
		sb.append("</BODY>\n");
		sb.append("</HTML>\n");
		
		return(sb.toString());
	}
	private String getHTMLStartEnd(boolean exec, String id, String rsvId, String start, String end) {
		if ( ! id.equals("")) {
			if (rsvId == null) {
				if ( ! exec) {
					return ("<font color=\"red\"><s>"+start+"～"+end.substring(11)+"</s></font>");
				}
				else {
					return ("<font color=\"red\">"+start+"～"+end.substring(11)+"</font>");
				}
			}
			else {
				return (start+"～"+end.substring(11));
			}
		}
		else {
			if (rsvId == null) {
				return ("<font color=\"blue\"><s><i>"+start+"～"+end.substring(11)+"</i></s></font>");
			}
			else {
				return ("<font color=\"blue\"><i>"+start+"～"+end.substring(11)+"</i></font>");
			}
		}
	}
	
	// 鯛ナビからの予約リクエストを処理する
	public String addReservedInfo(RecorderList rec, String pstr, ArrayList<ReserveInfo> rsvInfo) {
		
		// POST文字列から予約情報を生成する
		ReserveInfo newInfo = new ReserveInfo(pstr);
		newInfo.setByTainavi(true);
		newInfo.setIpaddr(rec.getRecorderIPAddr());
		newInfo.setPortno(rec.getRecorderPortNo());
		newInfo.setRec_pattern_id(getRec_pattern_Id(newInfo.getRec_pattern()));
		newInfo.setRec_nextdate(CommonUtils.getNextDate(newInfo));
		newInfo.setRec_min(CommonUtils.getRecMin(newInfo.getAhh(),newInfo.getAmm(),newInfo.getZhh(),newInfo.getZmm()));
		getStartEndDateTime(newInfo);
		
		String cur = CommonUtils.getDateTime(0);
		CommonUtils.getStartEndList(newInfo.getStarts(), newInfo.getEnds(), newInfo);
		for (int i=0; i<newInfo.getStarts().size(); i++) {
			if (newInfo.getStarts().get(i).compareTo(cur) < 0) {
				newInfo.getRecIds().add("-1");
			}
			else {
				newInfo.getRecIds().add(null);
			}
		}
		
		newInfo.setId(null);
		rsvInfo.add(newInfo);
		setNewRsvId(rec, rsvInfo);
		
		System.out.println("予約を受け付けました："+newInfo.getRec_pattern()+" "+newInfo.getTitle());	// デバッグ
		return(newInfo.getId());
	}
	
	// 鯛ナビからの更新リクエストを処理する
	public String updReservedInfo(RecorderList rec, String pstr, ArrayList<ReserveInfo> rsvInfo) {
		
		// POST文字列から予約情報を生成する
		ReserveInfo newInfo = new ReserveInfo(pstr);
		newInfo.setByTainavi(true);
		newInfo.setIpaddr(rec.getRecorderIPAddr());
		newInfo.setPortno(rec.getRecorderPortNo());
		newInfo.setRec_pattern_id(getRec_pattern_Id(newInfo.getRec_pattern()));
		newInfo.setRec_nextdate(CommonUtils.getNextDate(newInfo));
		newInfo.setRec_min(CommonUtils.getRecMin(newInfo.getAhh(),newInfo.getAmm(),newInfo.getZhh(),newInfo.getZmm()));
		getStartEndDateTime(newInfo);
		
		String cur = CommonUtils.getDateTime(0);
		CommonUtils.getStartEndList(newInfo.getStarts(), newInfo.getEnds(), newInfo);
		for (int i=0; i<newInfo.getStarts().size(); i++) {
			if (newInfo.getStarts().get(i).compareTo(cur) < 0) {
				newInfo.getRecIds().add("-1");
			}
			else {
				newInfo.getRecIds().add(null);
			}
		}
		
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if (rInfo.getId().equals(newInfo.getId())) {
				rInfo.setId("");	// 実際の削除はPOP実行時
				break;
			}
		}
		rsvInfo.add(newInfo);
		
		System.out.println("更新を受け付けました："+newInfo.getRec_pattern()+" "+newInfo.getTitle());	// デバッグ
		return(newInfo.getId());
	}
	
	// 鯛ナビからの削除リクエストを処理する
	public void delReservedInfo(RecorderList rec, String targetRsvId, ArrayList<ReserveInfo> rsvInfo) {
		
		// 
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if (rInfo.getId().equals(targetRsvId)) {
				rInfo.setId("");	// 実際の削除はPOP実行時
				
				// 無効な予約の場合は実際の削除リクエストは不要
				for ( int i=0; i<rInfo.getRecIds().size(); i++ ) {
					if (rInfo.getRecIds().get(i) != null && rInfo.getRecIds().get(i).equals("-1")) {
						rInfo.getRecIds().set(i, null);
					}
				}
				
				System.out.println("更新を受け付けました："+rInfo.getRec_pattern()+" "+rInfo.getTitle());	// デバッグ
				break;
			}
		}
	}
	
	//
	public void remoteCtrl(RecorderList rec, String location) {
		// おまじない
		Authenticator.setDefault(new MyAuthenticator(rec.getRecorderUser(), rec.getRecorderPasswd()));
		
		String uri = "http://"+rec.getRecorderIPAddr()+":"+rec.getRecorderPortNo()+location;
		System.err.println("GET "+uri);
		reqGET(uri, null);
	}
	
	
	
	// リクエストキューの作成
	public boolean getReqQueue(RecorderInfo rec, ArrayList<ReserveInfo> rsvInfo, ArrayList<String> reqQueue, boolean appendDT, int period) {
		
		// 無効エントリの削除と新規エントリの追加
		refreshReserveStartEnd(rec, rsvInfo);

		// 更新期限
		String cur = CommonUtils.getDateTime(16*60);
		
		boolean drop = false;
		
		// 削除リクエスト
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if (rInfo.getId() != null && rInfo.getId().equals("")) {
				for (int i=0; i<rInfo.getRecIds().size(); i++) {
					if (rInfo.getRecIds().get(i) != null && ! rInfo.getRecIds().get(i).equals("-1")) {
						if (rInfo.getStarts().get(i).compareTo(cur) > 0) {
							String req = "open "+rec.getRecorderBroadcast()+" prog del "+rInfo.getRecIds().get(i)+"\r\n";
							reqQueue.add(req);
						}
						else {
							System.out.println("【警告】録画開始時刻まで１５分を切っているため削除できません. id="+rInfo.getRecIds().get(i));
							drop = true;
						}
					}
				}
			}
		}
		
		// 登録リクエスト
		String periodDate = CommonUtils.getDateTime(period*86400);
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(rec.getRecorderIPAddr())) {
				continue;
			}
			
			if ( ! rInfo.getExec()) {
				// 予約実行OFFのエントリはＲＤに登録しない
				continue;
			}

			if (rInfo.getId() != null && ! rInfo.getId().equals("")) {
				if (rInfo.getByTainavi()) {
					for (int i=0; i<rInfo.getRecIds().size(); i++) {
						boolean periodFlag = (rInfo.getStarts().get(i).compareTo(periodDate) < 0);
						if (rInfo.getRec_pattern_id() == 11) {
							periodFlag = true;
						}
						if (rInfo.getRecIds().get(i) == null && periodFlag) {
							for (ReqCtrl rc : reqCtrl) {
								if (rc.getVersion().equals(rInfo.getVersion())) {
									if (rInfo.getStarts().get(i).compareTo(cur) > 0) {
										String req = rc.getReqStr(rec, rInfo, i, appendDT);
										reqQueue.add(req);
									}
									else {
										System.out.println("【警告】録画開始時刻まで１５分を切っているため予約できません. start="+rInfo.getStarts().get(i));
										drop = true;
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		
		return drop;
	}
	
	// 予約一覧にIdを埋め込む
	public void addRsvId(String ipaddr, ArrayList<ReserveInfo> rsvInfo, String start, String end, String channel, String title, String id) {
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(ipaddr)) {
				continue;
			}
			
			if ( ! rInfo.getByTainavi()) {
				continue;
			}
			
			// RDが間違ったCHを返してくることがあるのでタイトル一致でもＯＫとする
			boolean matchCh = (rInfo.getChannel().equals(channel));
			boolean matchTi = (rInfo.getTitle().replaceAll(" \\d\\d_\\d\\d$", "").equals(title.replaceAll(" \\d\\d_\\d\\d$", "")));
			if (matchCh || matchTi) {
				System.err.println("DEBUG: matchCh="+matchCh+", matchTi="+matchTi);
				for (int i=0; i<rInfo.getStarts().size(); i++) {
					if (rInfo.getRecIds().get(i) == null) {
						if (rInfo.getStarts().get(i).equals(start) && rInfo.getEnds().get(i).equals(end)) {
							rInfo.getRecIds().set(i, id);
							
							System.out.println("予約が確定しました："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
							
							ReserveInfo.save(rsvInfo);	// 保存しよう
							ReserveInfo.showReserveInfo(rsvInfo,ipaddr);
							return;
						}
					}
				}
			}
		}
		System.err.println("不正な完了通知です.");
	}
	
	// 予約一覧からIdを削除
	public void delRsvId(String ipaddr, ArrayList<ReserveInfo> rsvInfo, String id) {
		for (ReserveInfo rInfo : rsvInfo) {
			if ( ! rInfo.getIpaddr().equals(ipaddr)) {
				continue;
			}
			
			/* == 削除の場合は、エントリが鯛ナビからのか本体からのかを気にしなくてよい ==
			if ( ! rInfo.getByTainavi()) {
				continue;
			}
			*/
			
			// 削除対象でなければスルー
			if ( ! rInfo.getId().equals("")) {
				continue;
			}
			
			int cnt = 0;
			for (int i=0; i<rInfo.getRecIds().size(); i++) {
				if (rInfo.getRecIds().get(i) == null) {
					cnt++;
				}
				else {
					if (rInfo.getRecIds().get(i).equals(id)) {
						rInfo.getRecIds().set(i, null);
						cnt++;
						
						System.out.println("削除が確定しました："+rInfo.getStarts().get(i)+" "+rInfo.getTitle());
					}
				}
			}
			// 全Idがnullになったらエントリから削除
			if (cnt == rInfo.getRecIds().size()) {
				rsvInfo.remove(rInfo);
				
				ReserveInfo.save(rsvInfo);	// 保存しよう
				ReserveInfo.showReserveInfo(rsvInfo,ipaddr);
				return;
			}
		}
	}

}
