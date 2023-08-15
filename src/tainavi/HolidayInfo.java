package tainavi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 内閣府の「国民の祝日」CSVを取得して祝日情報を管理するクラス
 */
public class HolidayInfo{
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	private final static String FETCH_URI = "https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv";
	private final static String FILENAME = "env"+File.separator+"holidays.csv";
	private final static String encoding = "MS932";

	/*******************************************************************************
	 * 静的メンバー
	 ******************************************************************************/
	private static HashMap<String, HolidayInfo> holidays = null;

	/*******************************************************************************
	 * 部品以外のインスタンスメンバー
	 ******************************************************************************/
	private int year;
	private int month;
	private int day;
	private String holidayName;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	public HolidayInfo(){
		year = 0;
		month = 0;
		day = 0;
		holidayName = null;
	}

	/*******************************************************************************
	 * 公開メソッド
	 ******************************************************************************/
	// 祝日の西暦年を返す
	public int getYear(){
		return year;
	}

	// 祝日の月を返す
	public int getMonth(){
		return month;
	}

	// 祝日の日を返す
	public int getDay(){
		return day;
	}

	// 祝日の名称を返す
	public String getHolidayName(){
		return holidayName;
	}

	/*******************************************************************************
	 * 静的公開メソッド
	 ******************************************************************************/
	/*
	 * CSVファイルを読み込む
	 */
	public static boolean Load(String uri, int interval){
		if (IsCSVFileLifetimeExpired(interval)){
			FetchCSVFile(uri);
		}

		return LoadFromCSVFile(FILENAME);
	}

	/*
	 * CSVファイルを更新する
	 */
	public static boolean Refresh(String uri, int interval){
		if (!IsCSVFileLifetimeExpired(interval))
			return true;

		FetchCSVFile(uri);

		return LoadFromCSVFile(FILENAME);
	}

	/*
	 * CSVファイルを内閣府のサーバーからダウンロードする
	 */
	public static boolean FetchCSVFile(String uri){
		if (uri == null || uri.isEmpty())
			uri = FETCH_URI;

		BufferedWriter bw = null;
		BufferedReader br = null;
		try {
			// 内閣府のホームページに接続する
			URL url = new URL(uri);
			HttpURLConnection ucon = (HttpURLConnection)url.openConnection();
			ucon.setConnectTimeout(5000);
			ucon.setReadTimeout(5000);
			ucon.setRequestMethod("GET");
			ucon.connect();

			// レスポンスを一時ファイルに書き出す
			String tmpfile = FILENAME + ".tmp";
			bw = new BufferedWriter(new FileWriter(tmpfile));
			br = new BufferedReader(new InputStreamReader(ucon.getInputStream(), encoding));

			String str;
		    while((str = br.readLine()) != null){
		    	bw.write(str);
		    	bw.write("\n");
		    }

		    br.close();
		    br = null;
		    bw.close();
		    bw = null;

			// CSVファイルにリネームする
		    File ofile = new File(FILENAME);
		    if ( ofile.exists() && ! ofile.delete() ) {
				System.err.println("[HolidayInfo]CSVファイルを削除できない:" + ofile.getAbsolutePath());
		    }

		    File nfile = new File(tmpfile);
		    if ( ! nfile.renameTo(ofile) ) {
				System.err.println("[HolidayInfo]CSVファイルをリネームできない:" +
						nfile.getAbsolutePath() + "=>" + ofile.getAbsolutePath());
		    	return false;
		    }

		    return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			if (br != null) try { br.close(); } catch (Exception e) {}
			if (bw != null) try { bw.close(); } catch (Exception e) {}
		}

		return false;
	}

	/*
	 * CSVファイルを読み込む
	 */
	public static boolean LoadFromCSVFile(String path){
		if (path == null)
			return false;

		HashMap<String, HolidayInfo> map = new HashMap<String, HolidayInfo>();
		BufferedReader br = null;

		// CSVファイルが存在するかチェックする
		File file = new File(path);
		try {
			if (!file.exists()){
				System.err.println("[HolidayInfo]ファイルがない： " + file.getAbsolutePath());
				return false;
			}

			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

			// １行ずつ読み込む
			String str;
			while ((str = br.readLine()) != null) {
				// CSVの形式は YYYY/MM/DD,名称となっている
				Matcher ma = Pattern.compile("^(\\d{4})/(\\d{1,2})/(\\d{1,2}),(.*)$").matcher(str);
				if (!ma.find())
					continue;

				// 祝日情報を生成する
				HolidayInfo hi = new HolidayInfo();
				hi.year = Integer.parseInt(ma.group(1));
				hi.month = Integer.parseInt(ma.group(2));
				hi.day = Integer.parseInt(ma.group(3));
				hi.holidayName = ma.group(4);

				// キーに紐づけてハッシュマップに追加する
				String s = FormatKey( hi.year, hi.month, hi.day);
				map.put(s, hi);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("[HolidayInfo]ファイルの読み込みに失敗： "+file.getAbsolutePath());
			return false;
		}
		finally {
			if (br != null) try { br.close(); } catch (Exception e) {}
		}

		holidays = map;
		System.out.println("[HolidayInfo]祝日数： "+map.size());

		return true;
	}

	/*
	 * 指定した日が祝日かどうかを返す
	 */
    public static boolean IsHoliday(int year, int month, int day){
    	return IsHoliday(FormatKey(year, month, day));
    }

    /*
     * 指定した日が祝日の場合、その名前を返す
     */
    public static String GetHolidayName(int year, int month, int day){
    	return GetHolidayName(FormatKey(year, month, day));
    }

    /*
     * 指定した日の祝日情報を返す
     */
    public static HolidayInfo GetHolidayInfo(int year, int month, int day){
    	return GetHolidayInfo(FormatKey(year, month, day));
    }

    /*
     * YYYY/MM/DD形式で指定した日が祝日かどうかを返す
     */
	public static boolean IsHoliday(String date){
		HolidayInfo hi = GetHolidayInfo(date);

    	return (hi != null);
	}

    /*
     * YYYY/MM/DD形式で指定した日が祝日の場合、その名前を返す
     */
	public static String GetHolidayName(String date){
		HolidayInfo hi = GetHolidayInfo(date);

    	return (hi != null) ? hi.holidayName : null;
	}

    /*
     * YYYY/MM/DD形式で指定した日の祝日情報を返す
     */
	public static HolidayInfo GetHolidayInfo(String date){
		if (date == null || holidays == null)
			return null;

		if (date.length() > 10)
			date = date.substring(0, 10);

    	return holidays.get(date);
	}

	/*******************************************************************************
	 * 静的内部関数
	 ******************************************************************************/
    /*
     * ハッシュマップのキーを生成する
     */
	private static String FormatKey(int year, int month, int day){
		return String.format("%04d/%02d/%02d", year, month, day);
	}

	/*
	 * CSVファイルの寿命が尽きているかどうかを返す
	 */
	private static boolean IsCSVFileLifetimeExpired(int interval){
		// インターバルが０以下の場合は寿命はない
		if (interval <= 0)
			return false;

		// CSVファイルが存在しない場合は尽きているとみなす
		File file = new File(FILENAME);
		if (!file.exists())
			return true;

		try {
			BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			FileTime time = attrs.creationTime();
			Instant now = Instant.now();

			// ファイル生成後の経過ミリ秒数を計算する
			long elapsed = now.toEpochMilli() - time.toMillis();
			System.out.println("[HolidayInfo]経過時間(日):" + (elapsed/1000/24/3600));

			// インターバルを超えていない場合は尽きていない
			if ( elapsed/1000 < interval*24*3600)
				return false;
		} catch (IOException e) {
		}

		return true;
	}

}