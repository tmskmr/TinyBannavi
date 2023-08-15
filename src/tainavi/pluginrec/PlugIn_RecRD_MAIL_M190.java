package tainavi.pluginrec;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.ReserveList;

public class PlugIn_RecRD_MAIL_M190 extends PlugIn_RecRD_MAIL implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_MAIL_M190 clone() {
		return (PlugIn_RecRD_MAIL_M190) super.clone();
	}
	
	@Override
	public String getRecorderId() { return "REGZA DBR-M190(Mail)"; }

	@Override
	protected String getDefFile() { return "env/mail_m190.def"; }

	@Override
	protected String getMailBody(ReserveList r, String passwd) {
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("open");
		sb.append(" ");
		sb.append(this.getBroadcast());
		sb.append(" ");
		sb.append("prog ");
		sb.append("add ");
		sb.append(String.format("%04d%02d%02d ",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE)));
		sb.append(" ");
		sb.append(r.getAhh()+r.getAmm());
		sb.append(" ");
		sb.append(r.getZhh()+r.getZmm());
		sb.append(" ");
		//sb.append(cc.getCH_NAME2CODE(r.getCh_name()));
		sb.append(getChCode().getCH_WEB2CODE(r.getCh_name()));
		sb.append(" ");
		//sb.append(text2value(encoder,r.getTuner()));
		//sb.append(" ");
		sb.append(text2value(vrate,r.getRec_mode()));
		sb.append(" ");
		//if (r.getRec_mode().indexOf("[TS") != 0) {
		//	sb.append(text2value(arate,r.getRec_audio()));
		//	sb.append(" ");
		//}
		sb.append(text2value(device,r.getRec_device()));
		sb.append(" ");
		/*
		sb.append(text2value(dvdcompat,r.getRec_dvdcompat()));
		String chapter_mode = text2value(xchapter,r.getRec_xchapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		chapter_mode = text2value(mvchapter,r.getRec_mvchapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		else {
			sb.append(" ");
			sb.append("CPN");	// マジックチャプターOFFをつけないとエラー発生
		}
		chapter_mode = text2value(mschapter,r.getRec_mschapter());
		if (chapter_mode != null) {
			sb.append(" ");
			sb.append(chapter_mode);
		}
		*/
		sb.append(text2value(mvchapter,r.getRec_mvchapter()));
		sb.append(" ");
		sb.append(text2value(aspect,r.getRec_aspect()));
		sb.append(" ");
		//sb.append((r.getExec())?("RY"):("RN"));
		//sb.append("\r\n");
		//sb.append(r.getTitle());
		sb.append("\r\n");
		
		return sb.toString();
	}
}
