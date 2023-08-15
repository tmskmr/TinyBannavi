package tainavi;

import java.awt.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 実際のレコーダの設定（IP、PORTなど）を保持するクラスです.
 * @since 3.15.4β
 */
public class RecorderInfo implements Cloneable {

	private String recorderName;
	private String recorderId;
	private String recorderIPAddr;
	private String recorderPortNo;
	private String recorderUser;
	private String recorderPasswd;
	private String recorderMacAddr;
	private String recorderBroadcast;
	private String recorderColor;
	private ArrayList<String> recorderColorList;
	private ArrayList<String> recorderEncoderList;
	private int recordedCheckScope;
	private int recorderTunerNum;
	private boolean useCalendar;
	private boolean useChChange;

	public RecorderInfo() {
		recorderId = "";
		recorderName = "";
		recorderIPAddr = "";
		recorderPortNo = "";
		recorderMacAddr = "";
		recorderBroadcast = "";
		recorderUser = "";
		recorderPasswd = "";
		recorderColor = "";
		recorderColorList = new ArrayList<String>();
		recorderEncoderList = new ArrayList<String>();
		recordedCheckScope = 14;
		recorderTunerNum = 0;
		useCalendar = true;
		useChChange = true;
	}

	public String MySelf() { return recorderIPAddr+":"+recorderPortNo+":"+recorderId; }

	public String getRecorderId() { return recorderId; }
	public void setRecorderId(String s) { recorderId = s; }

	public String getRecorderName() { return recorderName; }
	public void setRecorderName(String s) { recorderName = s; }

	public String getRecorderIPAddr() { return recorderIPAddr; }
	public void setRecorderIPAddr(String s) { recorderIPAddr = s; }

	public String getRecorderPortNo() { return recorderPortNo; }
	public void setRecorderPortNo(String s) { recorderPortNo = s; }

	public String getRecorderUser() { return recorderUser; }
	public void setRecorderUser(String s) { recorderUser = s; }

	public String getRecorderPasswd() { return recorderPasswd; }
	public void setRecorderPasswd(String s) { recorderPasswd = s; }

	public String getRecorderMacAddr() { return recorderMacAddr; }
	public void setRecorderMacAddr(String s) { recorderMacAddr = s; }

	public String getRecorderBroadcast() { return recorderBroadcast; }
	public void setRecorderBroadcast(String s) { recorderBroadcast = s; }

	public String getRecorderColor() {
		recorderColor = "";
		for (String s : recorderColorList) {
			recorderColor += s+";";
		}
		return recorderColor;
	}
	public void setRecorderColor(String s) {
		//
		recorderColorList.clear();
		//
		Matcher ma = Pattern.compile("^#......$").matcher(s);
		if (ma.find()) {
			recorderColorList.add(s);
		}
		else {
			ma = Pattern.compile("(#......);").matcher(s);
			while (ma.find()) {
				recorderColorList.add(ma.group(1));
			}
		}
		return;
	}

	public void clearEncoderColors() {
		recorderColorList.clear();
	}
	public void addEncoderColor(String c) {
		recorderColorList.add(c+";");
	}
	public String getEncoderColor(int n) {
		if ( recorderColorList.size() > n ) {
			return recorderColorList.get(n);
		}
		return CommonUtils.color2str(Color.RED);
	}
	public String getEncoderColor(String enc) {
		if (enc != null) {
			for (int i=0; i<recorderEncoderList.size() && i<recorderColorList.size(); i++) {
				if (enc.equals(recorderEncoderList.get(i))) {
					return recorderColorList.get(i);
				}
			}
		}
		return recorderColorList.get(0);
	}
	public int clearEncoderColorsSize() {
		return recorderColorList.size();
	}

	//
	public ArrayList<String> getRecorderEncoderList() { return recorderEncoderList; }
	public void setRecorderEncoderList(ArrayList<String> al) { recorderEncoderList = al; }

	public void clearEncoders() {
		recorderEncoderList.clear();
	}
	public void addEncoder(String enc) {
		recorderEncoderList.add(enc);
	}
	public String getEncoder(int n) {
		if ( recorderEncoderList.size() == 0 && n == 0) {
			// エンコーダ情報を持たないものはデフォルト文字列
			return "■";
		}
		else if ( recorderEncoderList.size() > n ) {
			return recorderEncoderList.get(n);
		}
		return "";
	}
	public int getEncoderSize() {
		return recorderEncoderList.size();
	}

	// 主にTvRock./EDCB用
	public int getRecordedCheckScope() {
		return recordedCheckScope;
	}
	public void setRecordedCheckScope(int n) {
		recordedCheckScope = n;
	}

	// 主にDIGA用
	public int getTunerNum() {
		return recorderTunerNum;
	}
	public void setTunerNum(int n) {
		recorderTunerNum = n;
	}

	public boolean getUseCalendar() { return useCalendar; }
	public void setUseCalendar(boolean b) { useCalendar = b; }

	public boolean getUseChChange() { return useChChange; }
	public void setUseChChange(boolean b) { useChChange = b; }

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}
	}
}
