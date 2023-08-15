package tainavi.plugintv;

import java.io.File;
import java.util.ArrayList;

import tainavi.AreaCode;
import tainavi.TVProgram;
import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;



public class PlugIn_CSPDimora extends PlugIn_TVPDimora implements TVProgram,Cloneable {

	//private static final String thisEncoding = "UTF-8";
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getTVProgramId() { return "Dimora(CSデジ)"; }
	
	@Override
	public boolean isAreaSelectSupported() { return false; }
	
	@Override
	public ProgType getType() { return ProgType.PROG; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.CS2; }

	//
	@Override
	public PlugIn_CSPDimora clone() {
		return (PlugIn_CSPDimora) super.clone();
	}

	@Override
	protected String getCenterInfoId() { return "ChannelTypeC"; }
	@Override
	protected String getChType() { return "8"; }
	@Override
	protected String getCenterCode(String id, String code) { return code; }
	
	@Override
	protected String getProgCacheFile(String areacode, String adate) { return String.format(getProgDir()+File.separator+"DimoraCS_%s_%s.html", areacode, adate.substring(6,8)); }
	
	
	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
	
	/*
	 * 公開メソッド
	 */
	
	@Override
	public String getDefaultArea() {return "全国";}
	private String getDefaultCode() {return "03";}
	
	@Override
	public void loadAreaCode() {
		aclist = new ArrayList<AreaCode>();
		AreaCode ac = new AreaCode();
		ac.setArea(getDefaultArea());
		ac.setCode(getDefaultCode());
		ac.setSelected(true);
		aclist.add(ac);
	}
	@Override
	public void saveAreaCode() {}
	
	@Override
	public String getArea(String code) { return(getDefaultArea()); }
	@Override
	public String getCode(String area) { return(getDefaultCode()); }
	@Override
	public String setSelectedAreaByName(String area) { return(getDefaultCode()); }
	@Override
	public String getSelectedArea() { return(getDefaultArea()); }
	@Override
	public String getSelectedCode() { return(getDefaultCode()); }
	
	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送地域を取得する（TVAreaから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
	
	
	
	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここから　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
	
	/*
	 * 公開メソッド
	 */

	// ここにはなにもない.
	
	/*
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 * ★★★★★　放送局を選択する（TVCenterから降格）－ここまで　★★★★★
	 * ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
	 */
}
