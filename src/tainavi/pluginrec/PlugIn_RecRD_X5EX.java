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

public class PlugIn_RecRD_X5EX extends PlugIn_RecRD_X5 implements HDDRecorder,Cloneable {
	
	@Override
	public PlugIn_RecRD_X5EX clone() {
		return (PlugIn_RecRD_X5EX) super.clone();
	}

	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "RD-X5EX"; }

	
	
	/* ここまで */

	
	
	/* 個別コード－ここから最後まで */

	/*
	 * 非公開メソッド 
	 */
	
	//
	@Override
	protected ArrayList<ReserveList> GetRdReservedList(String response) {
		
		System.out.println("X5EX's GetRdReservedList()");
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
			
			String[] d = new String[11];
			mb = Pattern.compile("<(td|TD).*?>(.*?)</(td|TD)>").matcher(buf);
			for (int i=0; i<d.length; i++) {
				if ( mb.find()) {
					d[i] = mb.group(2);
				}
				//System.out.println(i+") "+d[i]);
			}
			
			// 予約実行ON/OFF
			mb = Pattern.compile("check_off\\.gif").matcher(d[1]);
			if (mb.find()) {
				entry.setExec(false);
			}
			
			// 予約名のエスケープを解除する
			String title = d[2];
			mb = Pattern.compile("<A .*?>(.+?)</A>").matcher(title);
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
			entry.setRec_pattern(d[5]);
			entry.setRec_pattern_id(getRec_pattern_Id(entry.getRec_pattern()));
			mb = Pattern.compile("(\\d\\d):(\\d\\d).*?(\\d\\d):(\\d\\d)").matcher(d[6]);
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
			
			mb = Pattern.compile("act_(.+?)\\.gif").matcher(d[3]);
			if (mb.find()) {
				entry.setTuner(mb.group(1));
			}
			else {
				entry.setTuner("R1");
			}
			entry.setRec_mode(d[9]);
			entry.setTitle(title);
			entry.setTitlePop(TraceProgram.replacePop(title));
			entry.setCh_name(getChCode().getCH_REC2WEB(d[4]));
			entry.setChannel(d[4]);

			entry.setRec_audio(d[10]);
			//entry.rec_folder = data.get();	// 予約一覧からはとれない
			//entry.rec_genre = data.get();		//　予約一覧からはとれない
			
			// 予約情報を保存
			newReserveList.add(entry);
		}
		
		return(newReserveList);
	}
}
