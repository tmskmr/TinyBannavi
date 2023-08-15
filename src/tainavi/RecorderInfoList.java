package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * {@link RecorderInfo} のリストを実現するクラスです. 
 * @version 3.15.4β～
 */

public class RecorderInfoList extends ArrayList<RecorderInfo> {
	
	private static final long serialVersionUID = 1L;
	
	private final String rFileOld = "env"+File.separator+"recorderlist.xml";
	private final String rFile = "env"+File.separator+"recorderinfolist.xml";
	private final String passEncMark = "Encrypted:";
	
	// セーブ・ロード
	public boolean save() {
		System.out.println("レコーダ一覧を保存します: "+rFile);
		
		RecorderInfoList rl = new RecorderInfoList();
		
		for (RecorderInfo r : this) {
			// パスワードを隠す
			RecorderInfo rc = (RecorderInfo) r.clone();
			rc.setRecorderPasswd(passEncMark+b64.enc(EncryptPassword.enc(r.getRecorderPasswd())));
			rl.add(rc);
		}
		
		//
		if ( ! CommonUtils.writeXML(rFile, rl) ) {
			System.out.println("レコーダ一覧の保存に失敗しました： "+rFile);
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public boolean load() {
		
		System.out.println("レコーダ一覧を読み込みます: "+rFile);

		boolean isoldclass = false;
		ArrayList<RecorderInfo> rl = null;
		
		if ( ! new File(rFile).exists() ) {
			if ( ! new File(rFileOld).exists() ) {
				System.err.println("レコーダ一覧が読み込めなかったので登録なしで起動します.");
				return false;
			}
				
			// 旧RecorderInfoList対策
			isoldclass = true;
			rl = new ArrayList<RecorderInfo>();
			ArrayList<RecorderList> rlx = (ArrayList<RecorderList>) CommonUtils.readXML(rFileOld);
			for ( RecorderList rx : rlx ) {
				RecorderInfo r = new RecorderInfo();
				FieldUtils.deepCopy(r, rx);
				rl.add(r);
			}
		}
		else {
			rl = (RecorderInfoList) CommonUtils.readXML(rFile);
		}
		if ( rl == null ) {
			System.err.println("レコーダ一覧が読み込めなかったので登録なしで起動します.");
			return false;
		}
		
		this.clear();
		for (RecorderInfo r : rl) {
			if (r.getRecorderPasswd() != null) {
				if (r.getRecorderPasswd().indexOf(passEncMark) == 0) {
					// パスワードを復元する
					r.setRecorderPasswd(EncryptPassword.dec(b64.dec(r.getRecorderPasswd().substring(passEncMark.length()))));
				}
			}
			
			this.add(r);
		}
		
		if ( isoldclass && this.save() ) {
			System.err.println("レコーダ一覧ファイルを置き換えます： "+rFileOld+"->"+rFile);
			new File(rFileOld).delete();
		}
		
		return true;
	}
	
	@Override
	public boolean add(RecorderInfo r) {
		if ( r == null ) throw new NullPointerException();
		return super.add(r);
	}
}