package taiSync;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.RecorderList;
import tainavi.TextValueSet;

public class ReqCtrl {

	private ArrayList<TextValueSet> encoder = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> vrate = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> arate = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> device = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> dvdcompat = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> xchapter = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> mschapter = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> mvchapter = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> autodel = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> aspect = new ArrayList<TextValueSet>(); 
	private ArrayList<TextValueSet> lvoice = new ArrayList<TextValueSet>(); 
	
	private String defFile = "";
	
	private String version = "";
	public String getVersion() { return version; }
	
	// コンストラクタ
	public ReqCtrl(String defPath) {
		
		//
		defFile = defPath;
		
		//
		Matcher ma = Pattern.compile("mail_(.+?)\\.def").matcher(defFile);
		if (ma.find()) {
			version = ma.group(1);
		}
		
		try {
			encoder.clear();
			vrate.clear();
			arate.clear();
			device.clear();
			dvdcompat.clear();
			xchapter.clear();
			mschapter.clear();
			mvchapter.clear();
			autodel.clear();
			aspect.clear();
			lvoice.clear();
			
			BufferedReader r = new BufferedReader(new FileReader(defFile));
			String s ;
			while ( (s = r.readLine()) != null ) {
				String[] b = s.split(",");
				if ( b.length >= 3 ) {
					TextValueSet t = new TextValueSet() ;
					t.setText(b[1]) ;
					t.setValue(b[2]) ;
					
					if ( b[0].equals("7") ) {
						encoder.add(t) ;
					}
					else if ( b[0].equals("10") ) {
						vrate.add(t) ;
					}
					else if ( b[0].equals("11") ) {
						arate.add(t) ;
					}
					else if ( b[0].equals("12") ) {
						device.add(t) ;
					}
					else if ( b[0].equals("14") ) {
						dvdcompat.add(t) ;
					}
					else if ( b[0].equals("17") ) {
						xchapter.add(t) ;
					}
					else if ( b[0].equals("18") ) {
						mschapter.add(t) ;
					}
					else if ( b[0].equals("19") ) {
						mvchapter.add(t) ;
					}
					else if ( b[0].equals("13") ) {
						autodel.add(t) ;
					}
					else if ( b[0].equals("15") ) {
						lvoice.add(t) ;
					}
					else if ( b[0].equals("16") ) {
						aspect.add(t) ;
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// リクエスト文字列を生成する
	public String getReqStr(RecorderList rec, ReserveInfo r, int idx, boolean appendDT) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("open");
		sb.append(" ");
		sb.append(rec.getRecorderBroadcast());
		sb.append(" ");
		sb.append("prog ");
		sb.append("add ");
		sb.append(r.getStarts().get(idx).replaceAll("/", "").substring(0,8));
		sb.append(" ");
		sb.append(r.getAhh()+r.getAmm());
		sb.append(" ");
		sb.append(r.getZhh()+r.getZmm());
		sb.append(" ");
		sb.append(r.getCh_code());
		sb.append(" ");
		sb.append(_text2val(encoder,r.getTuner()));
		sb.append(" ");
		String rec_mode = _text2val(vrate,r.getRec_mode()) ;
		if ( ! rec_mode.equals("-")) {
			sb.append(rec_mode);
			sb.append(" ");
			String audio = _text2val(arate,r.getRec_audio());
			if ( audio != null ) {
				sb.append(audio);
				sb.append(" ");
			}
		}
		sb.append(_text2val(device,r.getRec_device()));
		sb.append(" ");
		sb.append(_text2val(dvdcompat,r.getRec_dvdcompat()));
		String chapter_mode = _text2val(xchapter,r.getRec_xchapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		chapter_mode = _text2val(mvchapter,r.getRec_mvchapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		else {
			sb.append(" ");
			sb.append("CPN");	// マジックチャプターOFFをつけないとエラー発生
		}
		chapter_mode = _text2val(mschapter,r.getRec_mschapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		
		String val;
		
		val = _text2val(autodel, r.getRec_autodel());
		if (val != null) {
			sb.append(" ");
			sb.append(val);
		}
		val = _text2val(aspect, r.getRec_aspect());
		if (val != null) {
			sb.append(" ");
			sb.append(val);
		}
		val = _text2val(lvoice, r.getRec_lvoice());
		if (val != null) {
			sb.append(" ");
			sb.append(val);
		}
		
		sb.append(" ");
		sb.append((r.getExec())?("RY"):("RN"));
		sb.append("\r\n");
		if ( ! r.getAutocomplete()) {
			sb.append(r.getTitle());
			if (r.getRec_pattern_id() < 11 && appendDT == true) {
				sb.append(" ");
				sb.append(r.getStarts().get(idx).substring(5,10).replace("/", "_"));
			}
			sb.append("\r\n");
		}
		
		return(sb.toString());
	}
	
	//
	private String _text2val(ArrayList<TextValueSet> a, String text) {
		for ( TextValueSet t : a ) {
			if (t.getText().equals(text)) {
				return(t.getValue());
			}
		}
		return(null);
	}
}
