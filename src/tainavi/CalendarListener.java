package tainavi;

/*
 * カレンダーのイベントを処理するリスナー
 */
public interface CalendarListener {

	/**
	 * 日付が選択された
	 *
	 * @param cal カレンダー部品
	 * @param date 選択された日(YYYY/MM/DD)
	 */
	public void notifyDateChange(JCalendar cal, String date);

	/*
	 * 祝日かどうかを取得する
	 */
	public boolean isHoliday(int year, int month, int day);
}