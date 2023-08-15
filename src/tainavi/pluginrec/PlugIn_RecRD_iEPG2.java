package tainavi.pluginrec;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tainavi.CommonUtils;
import tainavi.ContentIdDIMORA;
import tainavi.ContentIdEDCB;
import tainavi.ContentIdREGZA;
import tainavi.HDDRecorder;
import tainavi.ReserveList;
import tainavi.TVProgram;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgSubgenre;

/**
 * EPGデジタルのプラグイン
 */
public class PlugIn_RecRD_iEPG2 extends PlugIn_RecRD_iEPG implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_iEPG2 clone() {
		return (PlugIn_RecRD_iEPG2) super.clone();
	}
	
	/* 必須コード  - ここから */
	
	private static final String thisEncoding = "MS932";
	
	// 種族の特性
	@Override
	public String getRecorderId() { return "iEPG2"; }
	
	private String errmsg = "";
	

	@Override
	public String getChDatHelp() { return
			"Web番組表にDimora/ザテ/王国/EDCBを、またはレコーダプラグインにEDCB/TVTest/RDデジ系を追加している場合ここの設定は不要です。"+
			"それ以外の場合はwikiを見て設定してください。";
	}
	
	/*
	 *	予約を実行する
	 */
	@Override
	public boolean PostRdEntry(ReserveList r)
	{
		//
		errmsg = "";
		
		String station_id = null;
		int event_id = -1;
		
		if ( r.getContentId() != null ) {
			if ( ContentIdEDCB.isValid(r.getContentId()) ) {
				ContentIdEDCB.decodeContentId(r.getContentId());
				station_id = ContentIdEDCB.getStId();
				event_id = ContentIdEDCB.getEvId();
			}
			else if ( ContentIdDIMORA.isValid(r.getContentId()) ) {
				ContentIdDIMORA.decodeContentId(r.getContentId());
				station_id = ContentIdDIMORA.getStId();
				event_id = ContentIdDIMORA.getEvId();
			}
			else if ( ContentIdREGZA.isValid(r.getContentId()) ) {
				ContentIdREGZA.decodeContentId(r.getContentId());
				station_id = ContentIdREGZA.getStId();
				event_id = ContentIdREGZA.getEvId();
			}
		}
		if ( station_id == null ) {
			if (getCC().getCH_WEB2CODE(r.getCh_name()) == null) {
				errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
				System.out.println(getErrmsg());
				return(false);
			}
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		setErrmsg("");
		
		String iepgFile = "env/hogehoge.tvpid";

		//
		try {
			
			GregorianCalendar ca = CommonUtils.getCalendar(r.getRec_pattern());
			if ( ca == null ) {
				setErrmsg("日付指定しか利用出来ません。");
				return(false) ;
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("Content-type: application/x-tv-program-digital-info; charset=shift_jis\r\n");
			sb.append("version: 2\r\n");
			if ( station_id != null ) {
				sb.append("station: "+station_id+"\r\n");
				sb.append("station-name: "+r.getCh_name()+"\r\n");
			}
			else {
				sb.append("station: "+getCC().getCH_WEB2REC(r.getCh_name())+"\r\n");
				sb.append("station-name: "+getCC().getCH_WEB2CODE(r.getCh_name())+"\r\n");
			}
			sb.append(String.format("year: %04d\r\n", ca.get(Calendar.YEAR)));
			sb.append(String.format("month: %02d\r\n", ca.get(Calendar.MONTH)+1));
			sb.append(String.format("date: %02d\r\n", ca.get(Calendar.DAY_OF_MONTH)));
			sb.append("start: "+r.getAhh()+":"+r.getAmm()+"\r\n");
			sb.append("end: "+r.getZhh()+":"+r.getZmm()+"\r\n");
			sb.append("program-title: "+r.getTitle()+"\r\n");
			if ( event_id > 0 ) {
				// futuer use.
				sb.append("program-id: "+event_id+"\r\n");
			}
			TVProgram.ProgGenre gr = TVProgram.ProgGenre.get(r.getRec_genre());
			if ( gr != null ) {
				sb.append("genre-1: "+gr.toIEPG()+"\r\n");
				
				if (r.getRec_subgenre() != null) {
					TVProgram.ProgSubgenre subg = TVProgram.ProgSubgenre.get(gr,r.getRec_subgenre());
					if ( subg != null ) {
						sb.append("subgenre-1: "+subg.toIEPG()+"\r\n");
					}
				}
			}
			sb.append("\r\n");
			sb.append(r.getDetail());
			
			if ( ! CommonUtils.write2file(iepgFile, sb.toString(), thisEncoding) ) {
				errmsg = "iEPGファイルの書き出しに失敗しました： "+iepgFile;
				return false;
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}

		// iEPGファイルを開く
		String emsg = CommonUtils.openFile(iepgFile);
		if (emsg != null) {
			setErrmsg(emsg);
			return false;
		}
		
		//
		long no = 0;
		for (ReserveList x : getReserves()) {
			if (Long.valueOf(x.getId()) > no) {
				no = Long.valueOf(x.getId());
			}
		}
		r.setId(String.valueOf(++no));

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
		ReservesToFile(getReserves(), getRsvedFile());

		return(true);
	}
	
	/*
	 * 
	 */
	@Override
	public String getErrmsg() {
		return(errmsg.replaceAll("[\r\n]", ""));
	}

	public PlugIn_RecRD_iEPG2() {
		super();
		this.setTunerNum(4);
	}
}
