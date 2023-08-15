package tainavi;

import java.io.File;
import java.util.ArrayList;


public class AutoReserveInfoList extends ArrayList<AutoReserveInfo> {

	private static final long serialVersionUID = 1L;
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	private static final String BASENAME = "autorsv";	// basenameを指定しなかった場合のデフォルト値
	
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	private final String envXml;
	
	
	/*******************************************************************************
	 * getter/setter
	 ******************************************************************************/
	
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	/**
	 * シリアライズ用のデフォルトコンストラクタ
	 */
	@Deprecated
	public AutoReserveInfoList() {
		super();
		envXml = null;
	}
	
	/**
	 * コンストラクタ
	 * @param basename null値は指定してした場合は{@link #BASENAME}が使用される
	 */
	public AutoReserveInfoList(String envdir, String basename, String recid, String ipaddr, String portno) {
		super();
		this.envXml = String.format("%s%s%s.%s_%s_%s.xml", envdir, File.separator, ((basename==null)?BASENAME:basename), recid, ipaddr, portno);
	}

	/*******************************************************************************
	 * 操作系メソッド
	 ******************************************************************************/
	
	public boolean exists() {
		if (envXml == null ) {
			return false;
		}
		return new File(envXml).exists();
	}
	
	/*******************************************************************************
	 * シリアライズ・デシリアライズ
	 ******************************************************************************/
	
	/**
	 * テキスト形式で保存する
	 */
	public boolean save() {
		if ( envXml == null ) {
			return false;
		}
		
		return CommonUtils.writeXML(envXml, this);
	}
	
	/**
	 * テキスト形式で読み込む
	 */
	public boolean load() {
		if ( envXml == null ) {
			return false;
		}
		
		return CommonUtils.readXML(envXml, this);
	}
	
}
