package tainavi.pluginrec;

import java.io.File;

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

public class PlugIn_RecRD_NULL extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_NULL clone() {
		return (PlugIn_RecRD_NULL) super.clone();
	}
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	@Override
	public String getRecorderId() { return "NULL"; }
	@Override
	public RecType getType() { return RecType.NULL; }
	
	@Override
	public boolean isAutocompleteSupported() { return false; }
	@Override
	public boolean isRecChNameNeeded() { return false; }
	@Override
	public boolean isChCodeNeeded()  { return false; }

	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	@Override
	public String getChDatHelp() { return
			"下記CHコードはCH設定から自動生成して表示しているものですが、NULLプラグインへの反映は自動では行われません。"+
			"初回起動時、またはCH設定変更時は必ず「更新を確定する」ボタンを押して確定してください。";
	}

	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	public ChannelCode getChCode() {
		return cc;
	}
	
	private final ChannelCode cc = new ChannelCode(getRecorderId());
	
	public String getErrmsg() {
		return(errmsg.replaceAll("[\r\n]", ""));
	}
	
	protected void setErrmsg(String s) {
		errmsg = s;
	}
	
	private String errmsg = "";

	/*******************************************************************************
	 * フリーオプション関係
	 ******************************************************************************/
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	private String rsvedFile = "";
	
	protected String getRsvedFile() {
		return(rsvedFile);
	}
	
	protected void setRsvedFile(String s) {
		rsvedFile = s;
	}

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public PlugIn_RecRD_NULL() {
		super();
		this.setTunerNum(4);
		this.setUseCalendar(false);
	}
	
	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	public boolean ChangeChannel(String Channel) {
		return false;
	}

	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/
	
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
					t.setText("N"+i);
					t.setValue("N"+i);
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
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	public boolean PostRdEntry(ReserveList r)
	{
		//
		if (getChCode().getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(getErrmsg());
			return(false);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");
		
		setErrmsg("");

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

	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		System.out.println("Through: UpdateRdEntry()");
		
		setErrmsg("更新処理は無効です。");

		return(false);
	}

	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
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
	
	/*******************************************************************************
	 * 非公開メソッド
	 ******************************************************************************/
	
}
