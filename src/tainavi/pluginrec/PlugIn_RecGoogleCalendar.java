package tainavi.pluginrec;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.ChannelCode;
import tainavi.CommonUtils;
import tainavi.HDDRecorder;
import tainavi.HDDRecorderUtils;
import tainavi.ReserveList;
import tainavi.TextValueSet;
import tainavi.HDDRecorder.RecType;

import com.google.gdata.client.calendar.CalendarQuery;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Person;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.extensions.Recurrence;
import com.google.gdata.data.extensions.Reminder;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Reminder.Method;
import com.google.gdata.util.RedirectRequiredException;

/**
 * <P>レコーダへの予約のついでにGoogleカレンダーに情報を登録するプラグインです。
 * <P>通常の操作は行えません。
 */
public class PlugIn_RecGoogleCalendar extends HDDRecorderUtils implements HDDRecorder,Cloneable {

	public PlugIn_RecGoogleCalendar clone() {
		return (PlugIn_RecGoogleCalendar) super.clone();
	}
	
	/* 必須コード  - ここから */
	
	/*******************************************************************************
	 * 種族の特性
	 ******************************************************************************/
	
	public String getRecorderId() { return "GoogleCalendar"; }
	public RecType getType() { return RecType.CALENDAR; }
	
	@Override
	public boolean isReserveListSupported() { return false; }
	@Override
	public boolean isBackgroundOnly() { return true; }
	
	/*******************************************************************************
	 * 予約ダイアログなどのテキストのオーバーライド
	 ******************************************************************************/
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String[] wd = {"SU","MO","TU","WE","TH","FR","SA"};

	/*******************************************************************************
	 * CHコード設定、エラーメッセージ
	 ******************************************************************************/
	
	public ChannelCode getChCode() {
		return cc;
	}
	
	private ChannelCode cc = new ChannelCode(getRecorderId());
	
	public String getErrmsg() {
		return(errmsg);
	}
	
	protected void setErrmsg(String s) {
		errmsg = s;
	}
	
	private String errmsg = "";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private ArrayList<TextValueSet> vrate = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> arate = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> folder = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> encoder = new ArrayList<TextValueSet>();
	private ArrayList<TextValueSet> dvdcompat = new ArrayList<TextValueSet>();
	public ArrayList<TextValueSet> getVideoRateList() { return(vrate); }
	public ArrayList<TextValueSet> getAudioRateList() { return(arate); }
	public ArrayList<TextValueSet> getFolderList() { return(folder); }
	public ArrayList<TextValueSet> getEncoderList() { return(encoder); }
	public ArrayList<TextValueSet> getDVDCompatList() { return(dvdcompat); }

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public PlugIn_RecGoogleCalendar() {
		setUser("ほげほげ@gmail.com(サンプル)");
		setPasswd("********");
		setMacAddr("10(分で指定してください。不要な場合は0で)");
	}

	/*******************************************************************************
	 * チャンネルリモコン機能
	 ******************************************************************************/
	
	public boolean ChangeChannel(String Channel) {
		return false;
	}

	/*******************************************************************************
	 * レコーダーから予約一覧を取得する
	 ******************************************************************************/
	
	public boolean GetRdReserve(boolean force)
	{
		return(true);
	}
	
	/*******************************************************************************
	 * 新規予約
	 ******************************************************************************/
	
	public boolean PostRdEntry(ReserveList r)
	{
		try {
			String postUrl = "http://www.google.com/calendar/feeds/default/private/full";
			
			CalendarEventEntry myEntry = new CalendarEventEntry();
	
			// 番組タイトル
			myEntry.setTitle(new PlainTextConstruct(r.getTitle()));
			// 番組詳細
			myEntry.setContent(new PlainTextConstruct(r.getDetail()));
			// 放送局
			Where evLocation = new Where();
			evLocation.setValueString(r.getCh_name());
			myEntry.addLocation(evLocation);
			// 開始終了時刻
			if (r.getRec_pattern_id() == -1) {
				// 通常のレコーダプラグインではないので必要ない処理
				r.setRec_pattern_id(getRec_pattern_Id(r.getRec_pattern()));
				r.setRec_nextdate(CommonUtils.getNextDate(r));
				r.setRec_min(CommonUtils.getRecMin(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm()));
				getStartEndDateTime(r);
			}
			if (r.getRec_pattern_id() == 11) {
				// 単日
				When eventTimes = new When();
				Matcher ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(r.getStartDateTime());
				if (ma.find()) {
					DateTime startTime = DateTime.parseDateTime(ma.group(1)+"-"+ma.group(2)+"-"+ma.group(3)+"T"+ma.group(4)+":"+ma.group(5)+":00+09:00");
					eventTimes.setStartTime(startTime);
				}
				ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(r.getEndDateTime());
				if (ma.find()) {
					DateTime endTime = DateTime.parseDateTime(ma.group(1)+"-"+ma.group(2)+"-"+ma.group(3)+"T"+ma.group(4)+":"+ma.group(5)+":00+09:00");
					eventTimes.setEndTime(endTime);
				}
				myEntry.addTime(eventTimes);
			}
			else { 
				// 繰り返しパターン
				String startTime = "";
				Matcher ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(r.getStartDateTime());
				if (ma.find()) {
					startTime = ma.group(1)+ma.group(2)+ma.group(3)+"T"+ma.group(4)+ma.group(5)+"00";
				}
				String endTime = "";
				ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(r.getEndDateTime());
				if (ma.find()) {
					endTime = ma.group(1)+ma.group(2)+ma.group(3)+"T"+ma.group(4)+ma.group(5)+"00";
				}
				String recurData = "";
				if (r.getRec_pattern_id() == 10) {
					// 毎日
					recurData = "DTSTART;TZID=Japan:"+startTime+"\r\n"+
						"DTEND;TZID=Japan:"+endTime+"\r\n"+
						"RRULE:FREQ=DAILY\r\n";
				}
				else if (9 >= r.getRec_pattern_id() && r.getRec_pattern_id() >= 7) {
					// 帯
					String days = "";
					int d = ((CommonUtils.isLateNight(r.getAhh()))?(1):(0));
					for (int i=1; i<=r.getRec_pattern_id()-3; i++) {
						days += wd[(i+d)%7]+",";
					}
					days = days.substring(0, days.length()-1);
					System.out.println(days);
					recurData = "DTSTART;TZID=Japan:"+startTime+"\r\n"+
						"DTEND;TZID=Japan:"+endTime+"\r\n"+
						"RRULE:FREQ=WEEKLY;BYDAY="+days+"\r\n";
				}
				else if (r.getRec_pattern_id() < 7) {
					// 週次
					recurData = "DTSTART;TZID=Japan:"+startTime+"\r\n"+
						"DTEND;TZID=Japan:"+endTime+"\r\n"+
						"RRULE:FREQ=WEEKLY;BYDAY="+wd[r.getRec_pattern_id()]+"\r\n";
				}
				Recurrence recur = new Recurrence();
				recur.setValue(recurData);
				myEntry.setRecurrence(recur);
			}
			// リマインダー
			try {
				int reminderMinutes = Integer.valueOf(getMacAddr());
				if (reminderMinutes > 0) {
					Method methodType = Method.EMAIL;

					Reminder reminder = new Reminder();
					reminder.setMinutes(reminderMinutes);
					reminder.setMethod(methodType);

					myEntry.getReminder().add(reminder);
				}
			}
			catch (NumberFormatException e) {
				// なにもしないよー
			}
			// 登録アプリ
			Person author = new Person("tainavi", "http://sourceforge.jp/projects/tainavi/", getUser());
			myEntry.getAuthors().add(author);
			
			CalendarService myService = new CalendarService("tainavi");
			myService.setUserCredentials(getUser(), getPasswd());
			
			reportProgress("Googleカレンダーに登録しています");
			
			// リダイレクトのループ
			CalendarEventEntry insertedEntry = null;
			for (int i=0; i<2; i++) {
				try {
					insertedEntry = myService.insert(new URL(postUrl), myEntry);
					break;
				} catch (RedirectRequiredException e) {
					postUrl = e.getRedirectLocation();
					System.out.println("redirect to: "+postUrl);
				}
				insertedEntry = null;
			}
			if (insertedEntry == null) {
				setErrmsg("Googleカレンダーへの登録に失敗しました");
				return(false);
			}
			
			return(true);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setErrmsg("Googleカレンダーへの登録に失敗しました");
		return(false);
	}

	/*******************************************************************************
	 * 予約更新
	 ******************************************************************************/
	
	public boolean UpdateRdEntry(ReserveList o, ReserveList r) {
		
		{
			// 削除、だよ

			// 開始終了時刻
			String startTime = "";
			String endTime = "";
			String rrule = null;
			if (o.getRec_pattern_id() == 11) {
				// 単日
				Matcher ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(o.getStartDateTime());
				if (ma.find()) {
					startTime = ma.group(1)+"-"+ma.group(2)+"-"+ma.group(3)+"T"+ma.group(4)+":"+ma.group(5)+":00.000+09:00";
				}
				ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(o.getEndDateTime());
				if (ma.find()) {
					endTime = ma.group(1)+"-"+ma.group(2)+"-"+ma.group(3)+"T"+ma.group(4)+":"+ma.group(5)+":00.000+09:00";
				}
				
				System.out.println("単日予定の削除: "+o.getTitle()+"("+o.getCh_name()+"),"+startTime+","+endTime);
			}
			else {
				// 繰り返しパターン
				Matcher ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(o.getStartDateTime());
				if (ma.find()) {
					startTime = ma.group(4)+ma.group(5)+"00";
				}
				ma = Pattern.compile("^(\\d+?)/(\\d+?)/(\\d+?) (\\d+?):(\\d+?)$").matcher(o.getEndDateTime());
				if (ma.find()) {
					endTime = ma.group(4)+ma.group(5)+"00";
				}
				if (o.getRec_pattern_id() == 10) {
					// 毎日
					rrule = "RRULE:FREQ=DAILY\n";
				}
				else if (9 >= o.getRec_pattern_id() && o.getRec_pattern_id() >= 7) {
					// 帯
					String days = "";
					int d = ((CommonUtils.isLateNight(o.getAhh()))?(1):(0));
					for (int i=1; i<=o.getRec_pattern_id()-3; i++) {
						days += wd[(i+d)%7]+",";
					}
					days = days.substring(0, days.length()-1);
					rrule = "RRULE:FREQ=WEEKLY;BYDAY="+days+"\n";
				}
				else if (o.getRec_pattern_id() < 7) {
					// 週次
					rrule = "RRULE:FREQ=WEEKLY;BYDAY="+wd[o.getRec_pattern_id()]+"\n";
				}
				
				if (rrule == null) {
					System.err.println("繰り返し予定のパターンが不正： "+o.getRec_pattern_id());
					return false;
				}
				
				System.out.println("繰り返し予定の削除: "+o.getTitle()+"("+o.getCh_name()+"),"+rrule.replaceFirst("\n","")+","+startTime+","+endTime);
			}
			
			// 削除クエリ
			try {
				
				String feedUrl = "http://www.google.com/calendar/feeds/default/private/full";
				
				CalendarService myService = new CalendarService("tainavi");
				myService.setUserCredentials(getUser(),getPasswd());
				
				CalendarEventFeed myResultsFeed = null;

				String nDate = CommonUtils.getNextDate(o);
				GregorianCalendar c = CommonUtils.getCalendar(nDate+" "+o.getAhh()+":"+o.getAmm());
				String sDate = CommonUtils.getDateTime(c).replaceFirst("\\(.\\)","").replaceFirst(" ","T").replaceAll("/","-")+":00.000+09:00";
				c.add(Calendar.MINUTE, Integer.valueOf(o.getRec_min()));
				String eDate = CommonUtils.getDateTime(c).replaceFirst("\\(.\\)","").replaceFirst(" ","T").replaceAll("/","-")+":00.000+09:00";
				//System.err.println(sDate+","+eDate);
				
				// リダイレクトのループ
				for (int i=0; i<2; i++) {
					try {
						CalendarQuery myQuery = new CalendarQuery(new URL(feedUrl));
						//myQuery.setStringCustomParameter("q", "\""+o.getTitle()+"\"");
						myQuery.setStringCustomParameter("sortorder", "a");
						if (o.getRec_pattern_id() == 11) {
							myQuery.setStringCustomParameter("singleevents", "true");	// 単日
							myQuery.setStringCustomParameter("start-min", startTime);
							myQuery.setStringCustomParameter("start-max", endTime);
						}
						else {
							myQuery.setStringCustomParameter("singleevents", "false");	// 繰り返し
						}
						myQuery.setMinimumStartTime(DateTime.parseDateTime(sDate));
						myQuery.setMaximumStartTime(DateTime.parseDateTime(eDate));
						myQuery.setMaxResults(25);
						myResultsFeed = myService.query(myQuery,CalendarEventFeed.class);
						break;
					}
					catch (RedirectRequiredException e) {
						feedUrl = e.getRedirectLocation();
						System.out.println("redirect to: "+feedUrl);
					}
					myResultsFeed = null;
				}
				if (myResultsFeed == null) {
					setErrmsg("Googleカレンダーの更新に失敗しました");
					return(false);
				}
				
				System.out.println("削除候補が "+myResultsFeed.getEntries().size()+" 件見つかりました");
				
				for (CalendarEventEntry entry : myResultsFeed.getEntries()) {
					System.out.println("削除候補: "+entry.getTitle().getPlainText()+" ("+entry.getLocations().get(0).getValueString()+"),"+entry.getTimes().get(0).getStartTime().toString()+","+entry.getTimes().get(0).getEndTime().toString());
				}
				for (CalendarEventEntry entry : myResultsFeed.getEntries()) {
					// タイトル
					if ( ! entry.getTitle().getPlainText().equals(o.getTitle())) {
						continue;
					}
					// 放送局
					if ( entry.getLocations().size() == 0 || ! entry.getLocations().get(0).getValueString().equals(o.getCh_name())) {
						continue;
					}
					
					// 開始終了日時
					if (rrule == null) {
						// 単日
						if (entry.getTimes().size() == 0) {
							continue;
						}
						
						System.out.println("単日予定: "+entry.getTimes().get(0).getStartTime().toString()+" - "+entry.getTimes().get(0).getEndTime().toString());
						
						if ( ! entry.getTimes().get(0).getStartTime().toString().equals(startTime)) {
							continue;
						}
						if ( ! entry.getTimes().get(0).getEndTime().toString().equals(endTime)) {
							continue;
						}
					}
					else {
						// 繰り返し
						if (entry.getRecurrence() == null) {
							continue;
						}
						
						System.out.println("繰り返し予定: "+entry.getRecurrence().getValue());
						
						Matcher ma = Pattern.compile("(DTSTART;[^:]+?:\\d\\d\\d\\d\\d\\d\\d\\dT"+startTime+")").matcher(entry.getRecurrence().getValue());
						if ( ! ma.find()) {
							continue;
						}
						Matcher mb = Pattern.compile("(DTEND;[^:]+?:\\d\\d\\d\\d\\d\\d\\d\\dT"+endTime+")").matcher(entry.getRecurrence().getValue());
						if ( ! mb.find()) {
							continue;
						}
						Matcher mc = Pattern.compile("("+rrule+")").matcher(entry.getRecurrence().getValue());
						if ( ! mc.find()) {
							//System.out.println(entry.getRecurrence().getValue());
							continue;
						}
					}
					
					System.out.println("削除対象が見つかりました: "+entry.getTitle().getPlainText()+" ("+entry.getLocations().get(0).getValueString()+")");
					
					// 削除実行
					reportProgress("Googleカレンダーから削除（更新）しています");
					URL deleteUrl = new URL(entry.getEditLink().getHref());
					myService.delete(deleteUrl);
					if (r == null) {
						return(true);
					}
					break;
				}
				
				// 更新、だよ。
				if (r != null) {
					if (PostRdEntry(r)) {
						return(true);
					}
				}
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		
		setErrmsg("Googleカレンダーの更新に失敗しました");
		return(false);
	}

	/*******************************************************************************
	 * 予約削除
	 ******************************************************************************/
	
	public ReserveList RemoveRdEntry(String delno) {
		return(null);
	}
}
