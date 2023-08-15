package tainavi.pluginrec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;


/*
 * 
 */

public class PlugIn_RecRD_iEPG extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_iEPG clone() {
		return (PlugIn_RecRD_iEPG) super.clone();
	}

	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "iEPG"; }
	public RecType getType() { return RecType.EPG; }
	
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
	
	// 繰り返し予約をサポートしていない
	@Override
	public boolean isRepeatReserveSupported() { return false; }
	
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
		System.out.println("Run: GetRdReserve("+force+")");
		
		setRsvedFile("env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml");
		
		// チューナー数は自動生成
		{
			encoder.clear();
			if ( getTunerNum() >= 2 ) {
				for ( int i=1; i<=getTunerNum(); i++ ) {
					TextValueSet t = new TextValueSet();
					t.setText("E"+i);
					t.setValue("E"+i);
					encoder.add(t);
				}
			}
		}
		
		File f = new File(getRsvedFile());
		if ( f.exists()) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(getRsvedFile()));
		}
		
		return(true);
	}
	
	/*
	 *	予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r)
	{
		//
		if (getCC().getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(getErrmsg());
			return(false);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");
		
		setErrmsg("");

		String iepgFile = "env/hogehoge.tvpi";

		//
		BufferedWriter bw = null;
		OutputStreamWriter sw = null;
		FileOutputStream os = null;
		try {
			Matcher ma = Pattern.compile("^(\\d\\d\\d\\d)/(\\d\\d)/(\\d\\d)").matcher(r.getRec_pattern());
			if ( ! ma.find()) {
				setErrmsg("日付指定しか利用出来ません。");
				return(false) ;
			}
			
			os = new FileOutputStream(iepgFile);
			sw = new OutputStreamWriter(os,"MS932");
			bw = new BufferedWriter(sw);
			bw.write("Content-type: application/x-tv-program-info; charset=Shift_JIS\r\n");
			bw.write("version: 1\r\n");
			bw.write("station: "+getCC().getCH_WEB2CODE(r.getCh_name())+"\r\n");
			bw.write(String.format("year: %04d\r\n", Integer.valueOf(ma.group(1))));
			bw.write(String.format("month: %02d\r\n", Integer.valueOf(ma.group(2))));
			bw.write(String.format("date: %02d\r\n", Integer.valueOf(ma.group(3))));
			bw.write("start: "+r.getAhh()+":"+r.getAmm()+"\r\n");
			bw.write("end: "+r.getZhh()+":"+r.getZmm()+"\r\n");
			bw.write("program-title: "+r.getTitle()+"\r\n");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (bw != null) try { bw.close(); } catch (IOException e) {};
			if (sw != null) try { sw.close(); } catch (IOException e) {};
			if (os != null) try { os.close(); } catch (IOException e) {};
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
	 *	予約を更新する
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		System.out.println("Through: UpdateRdEntry()");
		
		setErrmsg("更新処理は無効です。");

		return(false);
	}

	/*
	 *	予約を削除する
	 */
	public ReserveList RemoveRdEntry(String delid) {
		
		System.out.println("Run: RemoveRdEntry()");

		setErrmsg("");

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
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), getRsvedFile());

		System.out.printf("\n<<< Message from RD >>> \"%s\"\n\n", "正常に削除できました。");

		return(rx);
	}
	
	/*
	 * 
	 */
	public String getErrmsg() {
		return(errmsg.replaceAll("[\r\n]", ""));
	}
	protected void setErrmsg(String s) {
		errmsg = s;
	}
	
	/*
	 * 
	 */
	protected String getRsvedFile() {
		return(rsvedFile);
	}
	protected void setRsvedFile(String s) {
		rsvedFile = s;
	}
	
	/*
	 * 
	 */
	protected ChannelCode getCC() {
		return(cc);
	}

	public PlugIn_RecRD_iEPG() {
		super();
		this.setTunerNum(4);
	}
}
