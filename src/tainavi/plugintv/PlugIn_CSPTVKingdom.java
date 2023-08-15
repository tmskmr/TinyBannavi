package tainavi.plugintv;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.Center;
import tainavi.TVProgram;
import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;


public class PlugIn_CSPTVKingdom extends PlugIn_CSPTVKingdom110 implements TVProgram,Cloneable {

	final String thisEncoding = "UTF-8";
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	@Override
	public String getTVProgramId() { return "Gガイド.テレビ王国(スカパー!)"; }
	protected String getCSType() { return csCode; }
	
	@Override
	public boolean isAreaSelectSupported() { return false; }
	
	@Override
	public ProgType getType() { return ProgType.PROG; }
	@Override
	public ProgSubtype getSubtype() { return ProgSubtype.CS; }
	
	//
	public PlugIn_CSPTVKingdom clone() {
		return (PlugIn_CSPTVKingdom) super.clone();
	}

	@Override
	protected boolean _loadCenter(ArrayList<Center> newcrlist, String code, String type, String uri) {
		String response = webToBuffer(uri,thisEncoding,true);
		if ( response == null ) {
			System.err.println("放送局情報の取得に失敗しました: "+uri);
			return false;
		}
		
		reportProgress("放送局情報を取得しました: (1/1) "+uri);
		
		// 局名リストに追加する
		
		Matcher ma = Pattern.compile("\"9999\":\\s*\\{\\s*\"name\":\\s*(.+?)\\s*\\}\\s*},").matcher(response);
		if (ma.find()) {
			Matcher mb = Pattern.compile("\"([^\"]+?)\":\\s*\\{\\s*\"name\":\\s*\"([^\"]+?)\"\\s*\\},").matcher(ma.group(1));
			while (mb.find()) {
				String centerName = mb.group(2);
				Center cr = new Center();
				cr.setAreaCode(code);
				cr.setCenterOrig(centerName);
				cr.setLink(mb.group(1));
				cr.setType(type);
				cr.setEnabled(true);
				newcrlist.add(cr);
			}
		}
		
		return true;
	}
}
