package tainavi.pluginrec;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.ReserveList;

public class PlugIn_RecRD_MAIL_Z1 extends PlugIn_RecRD_MAIL_Z9500 implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_MAIL_Z1 clone() {
		return (PlugIn_RecRD_MAIL_Z1) super.clone();
	}

	@Override
	public String getRecorderId() { return "REGZA Z1(Mail)"; }

	@Override
	protected String getDefFile() { return "env/mail_z1.def"; }

	@Override
	protected String getMailBody(ReserveList r, String passwd) {
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		String msg = "dtvopen ";
		msg += passwd+" ";
		msg += String.format("%04d%02d%02d ",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE))+" ";
		msg += r.getAhh()+r.getAmm()+" ";
		msg += r.getZhh()+r.getZmm()+" ";
		msg += getChCode().getCH_WEB2CODE(r.getCh_name())+" ";
		msg += text2value(device,r.getRec_device());
		msg += "\r\n";
		return msg;
	}
}
