package tainavi;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * <P>個々の予約の情報を保持します。
 * <P>本当はReserveInfoに名前を変えたいのだけど、影響範囲が大きすぎてできなかった。
 */
public class ReserveList implements Cloneable {
	private int no=0;	// idに移行(2.4β～)
	private String id=null;
	private String rec_pattern="";
	private int rec_pattern_id=0;
	private String rec_nextdate="";
	private String ahh="";
	private String amm="";
	private String zhh="";
	private String zmm="";
	private String rec_min="";
	private String tuner="";
	private String rec_mode="";
	private String title="";
	private String titlePop="";
	private String channel="";
	private String ch_name="";

	private String detail="";

	private String content_id="";

	private String rec_audio="";
	private String rec_folder="";
	private String rec_genre="";
	private String rec_subgenre="";
	private String rec_dvdcompat="";
	private String rec_device="";

	private String rec_xchapter="";
	private String rec_mschapter="";
	private String rec_mvchapter="";

	private String rec_aspect = "";
	private String rec_bvperf = "";
	private String rec_lvoice = "";
	private String rec_autodel = "";

	private String rec_portable = "";
	private String rec_option = "";

	private String startDateTime="";
	private String endDateTime="";

	private boolean exec=true;		// 予約実行ON/OFF

	private boolean pursues=false;	// 番組追跡

	private boolean autocomplete=false;	// タイトル自動補完

	private boolean updateonlyexec=false;	// 予約実行ON/OFFのみの更新

	private boolean appsrsv=false;	// 持出

	private boolean tunershort=false;	// チューナー不足警告

	private boolean modenabled=true;

	private boolean recorded=false;		// 成功した録画結果が存在しているかどうか

	private boolean autoreserved=false;	// 自動予約かどうか

	private HashMap<String, String> hidden_params = new HashMap<String, String>();

	//
	@Override
	public ReserveList clone() {
		try {
			ReserveList p = (ReserveList) super.clone();
			p.rec_device = (this.rec_device!=null)?(this.rec_device):("");
			return p;
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}



	/**
	 * 繰り返し予約で、すでに終了日時を過ぎている場合は、開始日時／終了日時／次回実行予定日を更新する
	 */
	private void refreshNextDateTime() {
		if (rec_pattern_id == HDDRecorder.RPTPTN_ID_BYDATE)
			return;		// 単日予約は関係がない

		if (endDateTime.compareTo(CommonUtils.getDateTime(0)) >= 0)
			return;		// 終了済みだからリフレッシュしたい

		if ( ! (ahh.matches("^\\d+$") && zhh.matches("^\\d+$")))
			return;		// しかし時刻の情報がなかった

		ArrayList<String> starts = new ArrayList<String>();
		ArrayList<String> ends = new ArrayList<String>();
		CommonUtils.getStartEndList(starts, ends, this);
		if (starts.size() > 0) {
			GregorianCalendar c = CommonUtils.getCalendar(starts.get(0));
			if (c != null) {
				startDateTime = starts.get(0);
				endDateTime = ends.get(0);
				rec_nextdate = CommonUtils.getDate(c);
				return;
			}
		}
	}

	/*
	 * ほげほげ
	 */

	/**
	 * @deprecated 大昔、予約IDが数値だけだったころの名残
	 * @see #getId()
	 */
	public int getNo() {return no;}
	/**
	 * @deprecated 大昔、予約IDが数値だけだったころの名残
	 * @see #setId(String)
	 */
	public void setNo(int d) { no=d;}

	public String getId() {return id;}
	public void setId(String s) { id=s;}

	public String getRec_pattern() {return rec_pattern;}
	public void setRec_pattern(String s) { rec_pattern=s;}
	public int getRec_pattern_id() {return rec_pattern_id;}
	public void setRec_pattern_id(int i) { rec_pattern_id=i;}
	public String getAhh() {return ahh;}
	public void setAhh(String s) { ahh=s;}
	public String getAmm() {return amm;}
	public void setAmm(String s) { amm=s;}
	public String getZhh() {return zhh;}
	public void setZhh(String s) { zhh=s;}
	public String getZmm() {return zmm;}
	public void setZmm(String s) { zmm=s;}
	public String getRec_min() {return rec_min;}
	public void setRec_min(String s) { rec_min=s;}
	public String getTuner() {return tuner;}
	public void setTuner(String s) { tuner=s;}
	public String getRec_mode() {return rec_mode;}
	public void setRec_mode(String s) { rec_mode=s;}
	public String getTitle() {return title;}
	public void setTitle(String s) { title=s;}
	public String getTitlePop() {return titlePop;}
	public void setTitlePop(String s) { titlePop=s;}
	public String getChannel() {return channel;}
	public void setChannel(String s) { channel=s;}
	public String getCh_name() {return ch_name;}
	public void setCh_name(String s) { ch_name=s;}

	public String getDetail() {return detail;}
	public void setDetail(String s) { detail=s;}

	public String getContentId() {return content_id;}
	public void setContentId(String s) { content_id=s;}

	public String getRec_audio() {return rec_audio;}
	public void setRec_audio(String s) { rec_audio=s;}
	public String getRec_folder() {return rec_folder;}
	public void setRec_folder(String s) { rec_folder=s;}
	public String getRec_genre() {return rec_genre;}
	public void setRec_genre(String s) { rec_genre=s;}
	public String getRec_subgenre() {return rec_subgenre;}
	public void setRec_subgenre(String s) { rec_subgenre=s;}
	public String getRec_dvdcompat() {return rec_dvdcompat;}
	public void setRec_dvdcompat(String s) { rec_dvdcompat=s;}
	public String getRec_device() {return rec_device;}
	public void setRec_device(String s) { rec_device=s;}

	public String getRec_xchapter() {return rec_xchapter;}
	public void setRec_xchapter(String s) { rec_xchapter=s;}
	public String getRec_mschapter() {return rec_mschapter;}
	public void setRec_mschapter(String s) { rec_mschapter=s;}
	public String getRec_mvchapter() {return rec_mvchapter;}
	public void setRec_mvchapter(String s) { rec_mvchapter=s;}

	public String getRec_aspect() { return rec_aspect; }
	public void setRec_aspect(String s) { rec_aspect = s; }
	public String getRec_bvperf() { return rec_bvperf; }
	public void setRec_bvperf(String s) { rec_bvperf = s; }
	public String getRec_lvoice() { return rec_lvoice; }
	public void setRec_lvoice(String s) { rec_lvoice = s; }
	public String getRec_autodel() { return rec_autodel; }
	public void setRec_autodel(String s) { rec_autodel = s; }

	public String getRec_portable(){ return rec_portable; }
	public void setRec_portable(String s){ rec_portable = s; }
	public String getRec_option(){ return rec_option; }
	public void setRec_option(String s){ rec_option = s; }

	public String getStartDateTime() {
		refreshNextDateTime();
		return startDateTime;
	}
	public void setStartDateTime(String s) { startDateTime = s;}
	public String getEndDateTime() {
		refreshNextDateTime();
		return endDateTime;
	}
	public void setEndDateTime(String s) { endDateTime = s;}

	public String getRec_nextdate()  {
		refreshNextDateTime();
		return rec_nextdate;
	}
	public void setRec_nextdate(String s) { rec_nextdate = s;}

	public boolean getExec() {return exec;}
	public void setExec(boolean b) { exec=b;}
	public boolean getPursues() {return pursues;}
	public void setPursues(boolean b) { pursues=b;}
	public boolean getAutocomplete() {return autocomplete;}
	public void setAutocomplete(boolean b) { autocomplete=b;}
	public boolean getUpdateOnlyExec() {return updateonlyexec;}
	public void setUpdateOnlyExec(boolean b) { updateonlyexec=b;}
	public boolean getAppsRsv() {return appsrsv;}
	public void setAppsRsv(boolean b) { appsrsv=b;}
	public boolean getModEnabled() {return modenabled;}
	public void setModEnabled(boolean b) { modenabled=b;}

	public boolean getRecorded() {return recorded;}
	public void setRecorded(boolean b) { recorded=b;}

	public boolean getAutoreserved() {return autoreserved;}
	public void setAutoreserved(boolean b) { autoreserved=b;}

	public boolean getTunershort() { return tunershort; }
	public void setTunershort(boolean b) { tunershort = b; }

	public HashMap<String,String> getHidden_params() { return hidden_params; }
	public void setHidden_params(HashMap<String,String> a) { hidden_params = a; }

}
