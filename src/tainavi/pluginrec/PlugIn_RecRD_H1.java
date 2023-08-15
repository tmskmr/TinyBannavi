package tainavi.pluginrec;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.ReserveList;
import tainavi.TraceProgram;

/*
 * 
 */

public class PlugIn_RecRD_H1 extends PlugIn_RecRD_H2 implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_H1 clone() {
		return (PlugIn_RecRD_H1) super.clone();
	}
	
	//private static final String thisEncoding = "MS932";
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "RD-H1"; }
	
	/* ここまで */
	
	
	
	/* 個別コード－ここから最後まで */
	
	/*
	 * 公開メソッド 
	 */
	
	/*
	 * 非公開メソッド 
	 */
	
	//
	@Override
	protected ArrayList<ReserveList> GetRdReservedList(String response) {
		
		System.out.println("H1's GetRdReservedList()");
		//
		response = response.replaceAll("\n", "");

		ArrayList<ReserveList> newReserveList = new ArrayList<ReserveList>();

		Matcher ma = Pattern.compile("(<td [\\s\\S]+?)</tr>").matcher(response);
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
}
