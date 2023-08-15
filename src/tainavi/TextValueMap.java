package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;


/**
 * TextValueSetをこれで置き換える予定<BR>
 * @since 3.22.2b
 */
public class TextValueMap extends LinkedHashMap<String,String> {

	private static final long serialVersionUID = 1L;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	
	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 保存されない
	private final String envXml;	// XML形式のファイル名
	private final String label;		// GUIに表示する場合の項目名
	
	// 保存される
	private String defaultKey;		// デフォルトのキー値
	private String selectedKey;		// 選択中のキー値
	
	/*******************************************************************************
	 * getter/setter
	 ******************************************************************************/
	
	public String getDefaultKey() {
		return this.defaultKey;
	}
	public void setDefaultKey(String s) {
		this.defaultKey = s;
	}

	public String getSelectedKey() {
		return this.selectedKey;
	}
	public void setSelectedKey(String s) {
		this.selectedKey = s;
	}

	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	/**
	 * シリアライズ用のデフォルトコンストラクタ 
	 */
	@Deprecated
	public TextValueMap() {
		super();
		this.label = null;
		this.envXml = null;
	}
	
	/**
	 * HDDレコーダ用のコンストラクタ
	 */
	public TextValueMap(String label, String envdir, String basename, String recid, String ipaddr, String portno) {
		this(label,envdir,basename,String.format("%s_%s_%s",recid,ipaddr,portno));
	}
	
	/**
	 * 汎用のコンストラクタ
	 */
	public TextValueMap(String label, String envdir, String basename, String id) {
		super();
		this.label = label;
		this.envXml = String.format("%s%s%s.%s.xml", envdir, File.separator, basename, id);
	}
	
	
	/*******************************************************************************
	 * 操作系メソッド
	 ******************************************************************************/

	/**
	 * GUIで表示する項目名
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * 保存するファイル名
	 */
	public String getFilename() {
		return this.envXml;
	}
	
	/**
	 * このエントリーがデフォルトであることを示すマークを付けることのできる put()
	 */
	public String put(String key, String value, boolean isdefault) {
		String obj = super.put(key, value);
		if ( isdefault ) {
			defaultKey = key;
		}
		return obj;
	}

	/**
	 * 使わせたくないのだよ
	 */
	@Deprecated
	@Override
	public String put(String key, String value) {
		return super.put(key, value);
	}
	
	/**
	 * エントリーを選択する
	 */
	public Entry<String,String> setSelectedEntry(String key) {
		Entry<String,String> entry = getEntry(key);
		if ( entry != null ) {
			selectedKey = entry.getKey();
		}
		return entry;
	}
	
	/**
	 * デフォルトのエントリーを返す
	 */
	public Entry<String,String> getDefultEntry() {
		return this.getEntry(defaultKey);
	}
	
	/**
	 * 選択中のエントリーを返す
	 */
	public Entry<String,String> getSelectedEntry() {
		return this.getEntry(selectedKey);
	}

	/**
	 * 指定のエントリを返す（外部からはget(key)を使えばいいので非公開）
	 */
	private Entry<String,String> getEntry(String key) {
		if ( key == null ) {
			return null;
		}
		for ( Entry<String,String> entry : super.entrySet() ) {
			if ( key.equals(entry.getKey()) ) {
				return entry;
			}
		}
		return null;
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
