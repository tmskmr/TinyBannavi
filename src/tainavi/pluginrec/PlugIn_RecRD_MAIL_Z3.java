package tainavi.pluginrec;

import java.util.Calendar;
import java.util.GregorianCalendar;

import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.ReserveList;

public class PlugIn_RecRD_MAIL_Z3 extends PlugIn_RecRD_MAIL_Z9500 implements HDDRecorder,Cloneable {

	@Override
	public PlugIn_RecRD_MAIL_Z3 clone() {
		return (PlugIn_RecRD_MAIL_Z3) super.clone();
	}

	@Override
	public String getRecorderId() { return "REGZA Z3(Mail)"; }

	@Override
	protected String getDefFile() { return "env/mail_z3.def"; }

	@Override
	protected String getMailBody(ReserveList r, String passwd) {
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		String msg = "dtvopen ";
		msg += passwd+" ";
		msg += String.format("%04d%02d%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE))+" ";
		msg += r.getAhh()+r.getAmm()+" ";
		msg += r.getZhh()+r.getZmm()+" ";
		msg += getChCode().getCH_WEB2CODE(r.getCh_name())+" ";
		msg += text2value(device,r.getRec_device())+" ";
		msg += text2value(vrate,r.getRec_mode())+" ";
		msg += "\r\n";
		return msg;
	}
}
