package tainavi.pluginrec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;


public class PlugIn_RecRD_MAIL extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecRD_MAIL clone() {
		return (PlugIn_RecRD_MAIL) super.clone();
	}
	
	/* 必須コード  - ここから */
	
	// 種族の特性
	public String getRecorderId() { return "RD(Mail)"; }
	public RecType getType() { return RecType.MAIL; }
	
	// 個体の特性
	private ChannelCode cc = new ChannelCode(getRecorderId());
	private String rsvedFile = "";
	
	private String errmsg = "";

	protected String getDefFile() { return "env/mail.def"; }

	// 公開メソッド
	
	/*
	 * 
	 */
	public String Myself() {
		return("MAIL"+":"+getMacAddr()+":"+getRecorderId());
	}
	public ChannelCode getChCode() {
		return cc;
	}
	
	// 繰り返し予約をサポートしていない
	@Override
	public boolean isRepeatReserveSupported() { return false; }
	
	/*
	 * 
	 */
	public boolean ChangeChannel(String Channel) {
		return false;
	}

	/*
	 *	レコーダーから予約一覧を取得する 
	 */
	public boolean GetRdReserve(boolean force)
	{
		errmsg = "";
		
		System.out.println("Through: GetRdReserve("+force+")");
		
		String defFile = getDefFile();
		
		FileReader fr = null;
		BufferedReader r = null;
		try {
			encoder.clear();
			vrate.clear();
			arate.clear();
			device.clear();
			dvdcompat.clear();
			aspect.clear();
			xchapter.clear();
			mschapter.clear();
			mvchapter.clear();
			
			fr = new FileReader(defFile);
			r = new BufferedReader(fr);
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
					else if ( b[0].equals("16") ) {
						aspect.add(t) ;
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
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (r != null) try { r.close(); } catch (Exception e) {};
			if (fr != null) try { fr.close(); } catch (Exception e) {};
		}
		
		// 予約一覧をキャッシュから
		rsvedFile = "env/reserved."+getIPAddr()+"_"+getPortNo()+"_"+getRecorderId()+".xml";
		
		File f = new File(rsvedFile);
		if ( force == false && f.exists()) {
			// キャッシュから読み出し（予約一覧）
			setReserves(ReservesFromFile(rsvedFile));
			if (getReserves().size() > 0) {
				System.out.println("+read from="+rsvedFile);
			}
			return(true);
		}
		
		return(true);
	}
	
	/*
	 *	予約を実行する
	 */
	public boolean PostRdEntry(ReserveList r)
	{
		errmsg = "";
		
		//
		if (cc.getCH_WEB2CODE(r.getCh_name()) == null) {
			errmsg = "【警告】Web番組表の放送局名「"+r.getCh_name()+"」をCHコードに変換できません。CHコード設定を修正してください。" ;
			System.out.println(errmsg);
			return(false);
		}
		
		System.out.println("Run: PostRdEntry("+r.getTitle()+")");

		// メール本文を作る
		String msg = "";
		String message = "";
		
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		if (c == null) {
			errmsg = "日付指定しか利用出来ません。" ;
			return(false) ;
		}
		msg = getMailBody(r, this.getBroadcast());
		
		try {
			message = new String(msg.getBytes("MS932"),"Shift_JIS");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		// メールを送信する
		try {
			Properties props = new Properties() ;
			props.put("mail.smtp.host", this.getIPAddr()) ;
			props.put("mail.host", this.getIPAddr()) ;
			props.put("mail.smtp.port", this.getPortNo()) ;
			props.put("mail.smtp.auth", "true") ;
			props.put("mail.smtp.starttls.enable","true");
			
			Session session = Session.getInstance( props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(getUser(), getPasswd()) ;
				}
			});
			//session.setDebug(true);
			MimeMessage mimeMessage = new MimeMessage(session);
			mimeMessage.setRecipients(Message.RecipientType.TO, this.getMacAddr());
			mimeMessage.setFrom(new InternetAddress(this.getUser()));
			mimeMessage.setSubject(new Date().toString());
			mimeMessage.setSentDate(new Date());
			//mimeMessage.setText(message, "ISO-2022-JP");
			mimeMessage.setContent(message, "text/html;charset=iso-2022-jp");
			mimeMessage.setHeader("Content-Transfer-Encoding", "7bit"); 
			Transport.send(mimeMessage);
        } catch (AuthenticationFailedException e) {
        	errmsg = "SMTP認証に失敗しました："+e.toString() ;
        	return(false) ;
        } catch (MessagingException e) {
        	errmsg = "メール送信に失敗しました："+e.toString() ;
        	return(false) ;
		}
		
		// 予約ID
		long no = 0;
		for (ReserveList x : getReserves()) {
			if (Long.valueOf(x.getId()) > no) {
				no = Long.valueOf(x.getId());
			}
		}
		
		r.setId(String.valueOf(++no));
		// 予約パターンID
		r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
		
		// 次回予定日
		r.setRec_nextdate(CommonUtils.getNextDate(r));
		
		// 録画長
		r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
		
		// 開始日時・終了日時
		getStartEndDateTime(r);
		
		// 予約リストを更新
		getReserves().add(r);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		return(true);
	}
	protected String getMailBody(ReserveList r, String passwd) {
		GregorianCalendar c = CommonUtils.getCalendar(r.getRec_pattern());
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("open");
		sb.append(" ");
		sb.append(this.getBroadcast());
		sb.append(" ");
		sb.append("prog ");
		sb.append("add ");
		sb.append(String.format("%04d%02d%02d ",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DATE))+" ");
		sb.append(" ");
		sb.append(r.getAhh()+r.getAmm());
		sb.append(" ");
		sb.append(r.getZhh()+r.getZmm());
		sb.append(" ");
		sb.append(cc.getCH_WEB2CODE(r.getCh_name()));
		sb.append(" ");
		sb.append(text2value(encoder,r.getTuner()));
		sb.append(" ");
		sb.append(text2value(vrate,r.getRec_mode()));
		sb.append(" ");
		if (r.getRec_mode().indexOf("[TS") != 0) {
			sb.append(text2value(arate,r.getRec_audio()));
			sb.append(" ");
		}
		sb.append(text2value(device,r.getRec_device()));
		sb.append(" ");
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
		sb.append(" ");
		sb.append((r.getExec())?("RY"):("RN"));
		sb.append("\r\n");
		sb.append(r.getTitle());
		sb.append("\r\n");
		
		return sb.toString();
	}
	
	/*
	 *	予約を更新する
	 */
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		System.out.println("Through: UpdateRdEntry()");
		
		errmsg = "更新処理は無効です。";

		return(false);
	}

	/*
	 *	予約を削除する
	 */
	public ReserveList RemoveRdEntry(String delid) {
		errmsg = "";
		
		System.out.println("Through: RemoveRdEntry()");

		// 削除対象を探す
		ReserveList rx = null;
		for (  ReserveList reserve : getReserves() )  {
			if (reserve.getId().equals(delid)) {
				rx = reserve;
				break;
			}
		}
		if (rx == null) {
			return(null);
		}
		
		// 予約リストを更新
		getReserves().remove(rx);
		
		// キャッシュに保存
		ReservesToFile(getReserves(), rsvedFile);
		
		errmsg = "鯛ナビのエントリは削除しました。REGZA上で実際のエントリを削除して下さい。";
		
		return(rx);
	}
	
	/*
	 * 
	 */
	public String getErrmsg() {
		return(errmsg.replaceAll("\\\\r\\\\n", ""));
	}
}
