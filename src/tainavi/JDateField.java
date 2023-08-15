package tainavi;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.accessibility.Accessible;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/*
 * 日付入力部品
 */
public class JDateField extends JComboBox<String> implements CalendarListener{
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
    private JTextField textField = null;
    private JPopupMenu popupOrg = null;
    private JPopupMenu popup = null;
    private JCalendar calendar = null;

	/*******************************************************************************
	 * 部品以外のメンバー
	 ******************************************************************************/
    private JCalendar.Style style = JCalendar.Style.Button;
	private boolean allowNoYear = false;
    private GregorianCalendar calMinDate = null;
    private GregorianCalendar calMaxDate = null;

	private Instant last_shown = null;

	private CalendarListener listener = null;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
    public JDateField() {
    	this(JCalendar.Style.Button);
    }

	public JDateField(JCalendar.Style s){
		super();

    	style = s;

    	setEditable(true);
    	setMaximumRowCount(0);

    	createComponents();
	}

	/*******************************************************************************
	 * 公開メソッド
	 ******************************************************************************/
    /*
     * リスナーを登録する
     */
    public void setListener(CalendarListener l){
    	listener = l;
    }

	/*
	 * ポップアップを表示する
	 *
	 * @see javax.swing.JComboBox#showPopup()
	 */
	@Override
	public void showPopup(){
       	setSelectedDate(getCalendar(getText()));

		if (last_shown != null && Instant.now().minusMillis(100).isBefore(last_shown))
			return;

		popup.show(this, 0, getBounds().height);

		last_shown = Instant.now();
	}

	/*
	 * ポップアップを非表示にする
	 *
	 * @see javax.swing.JComboBox#hidePopup()
	 */
	@Override
	public void hidePopup(){
		if (popup.isVisible())
			popup.setVisible(false);
	}

	/*
	 * MM/DDのみを許すかをセットする
	 */
	public void setAllowNoYear(boolean b){
		allowNoYear = b;
	}

	/*
	 * アクションリスナーを追加する
	 */
	@Override
	public void addActionListener(ActionListener al){
		textField.addActionListener(al);
	}

    /*
     * 選択日をYYYY/MM/DD形式で指定する
     */
    public void setSelectedDate(String date){
    	setSelectedDate(getCalendar(date));
    }

    /*
     * 選択日をGregorianCalendarクラスで指定する
     */
    public void setSelectedDate(GregorianCalendar cal){
		cleanUpCalendarTime(cal);
    	if (calendar != null)
    		calendar.setSelectedDate(cal);
    }

    /*
     * 有効な最小日をYYYY/MM/DD形式で指定する
     */
    public void setMinDate(String date){
    	setMinDate(getCalendar(date));
    }

    /*
     * 有効な最小日をGregorianCalendarクラスで指定する
     */
    public void setMinDate(GregorianCalendar cal){
		cleanUpCalendarTime(cal);
    	calMinDate = cal;
    	if (calendar != null)
    		calendar.setMinDate(cal);
    }

    /*
     * 有効な最大日をYYYY/MM/DD形式で指定する
     */
    public void setMaxDate(String date){
    	setMaxDate(getCalendar(date));
    }

    /*
     * 有効な最大日をGregorianCalendarクラスで指定する
     */
    public void setMaxDate(GregorianCalendar cal){
		cleanUpCalendarTime(cal);
    	calMaxDate = cal;
    	if (calendar != null)
    		calendar.setMaxDate(cal);
    }

    /*
     * テキストを取得する
     */
    public String getText(){
    	return textField.getText();
    }

    /*
     * テキストをセットする
     */
    public void setText(String s){
        setSelectedItem(s);
       	setSelectedDate(getCalendar(s));
    }

    /*
     * 有効な日付を持っているか
     */
    public boolean hasValidDate(){
    	return isValidDate(textField.getText());
    }

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
    /*
     * コンポーネントを作成する
     */
    private void createComponents(){
    	// オリジナルのポップアップを取得する
    	Accessible accessible = this.getUI().getAccessibleChild(this,  0);
    	if (accessible  instanceof JPopupMenu ){
    		popupOrg = (JPopupMenu) accessible;
    		popupOrg.addPropertyChangeListener(pcl_popupOrg);
    	}

    	// テキストフィールドを取得する
    	try{
//	    	buttonArrow = (JButton)this.getComponent(0);
			textField = (JTextField) this.getEditor().getEditorComponent();
			textField.getDocument().addDocumentListener(dl_textField);
			textField.addMouseListener(new TextEditPopupMenu());
    	}
    	catch(ClassCastException e){

    	}

    	// カレンダーのポップアップを作成する
    	calendar = new JCalendar(style, false);
    	calendar.setListener(this);

    	String ESCKEYACTION = "escape";
		calendar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCKEYACTION);
		calendar.getActionMap().put(ESCKEYACTION, aa_calendarEscape);

    	popup = new JPopupMenu();
    	popup.add(calendar);
    	popup.addPopupMenuListener(pml_popup);
    }

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/
    /*
     * オリジナルのポップアップのプロパティ変更リスナー
     */
    private PropertyChangeListener pcl_popupOrg = new PropertyChangeListener(){
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// オリジナルが表示されたら強制的に非表示にして、カレンダーをポップアップする
			if (evt.getPropertyName().equals("visible") && evt.getNewValue() == Boolean.TRUE){
				popupOrg.setVisible(false);
				showPopup();
			}
		}
	};

	/*
	 * カレンダーのポップアップのリスナー
	 */
	private PopupMenuListener pml_popup = new PopupMenuListener(){
		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			// 非表示になる時に時刻を保存する
			last_shown = Instant.now();
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {
		}
	};

	/*
	 * テキストフィールドの文書変更リスナー
	 *
	 * 文書が変更されたら前景色を更新してポップアップを非表示にする
	 */
	private DocumentListener dl_textField = new DocumentListener(){
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateTextFieldForeground();
			hidePopup();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateTextFieldForeground();
			hidePopup();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateTextFieldForeground();
			hidePopup();
		}
	};

	/*
	 * カレンダーの時刻変更リスナー
	 *
	 * @see tainavi.CalendarListener#notifyDateChange(tainavi.JCalendar, java.lang.String)
	 */
	@Override
	public void notifyDateChange(JCalendar cal, String date) {
		// テキストをセットしてポップアップを非表示にする
		setText(date);
		hidePopup();

		if (listener != null)
			listener.notifyDateChange(cal, date);
	}

	/*
	 * カレンダーの祝日取得リスナー
	 * @see tainavi.CalendarListener#isHoliday(int, int, int)
	 */
	@Override
	public boolean isHoliday(int year, int month, int day) {
		if (listener != null)
			return listener.isHoliday(year, month, day);
		else
			return false;
	}

	/*
	 * カレンダーをESCキーで抜けるための仮想アクション
	 */
	private AbstractAction aa_calendarEscape = new AbstractAction(){
		@Override
		public void actionPerformed(ActionEvent e) {
			hidePopup();
		}
	};

    /*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
	/*
	 * テキストフィールドの前景色を更新する
	 */
	private void updateTextFieldForeground(){
		textField.setForeground(isValidDate((textField.getText())) ? Color.BLACK : Color.RED);
	}

	/*
	 * 指定された文字列が有効な日付かどうかを返す
	 */
	private boolean isValidDate(String s){
		if (s == null)
			return false;
		if (s.isEmpty())
			return true;

		// YYYY/MM/DD形式であること
		GregorianCalendar cal = getCalendar(s);
		if (cal == null)
			return false;

		// 最小値、最大値の間にあること
		if (!isValidDateRange(cal))
			return false;

		return true;
	}

    /*
     * 指定された日が最小値、最大値の間にあるかを返す
     */
    private boolean isValidDateRange(GregorianCalendar cal){
    	long time = cal.getTimeInMillis();

    	if (calMinDate != null){
    		long timeMin = calMinDate.getTimeInMillis();
    		if (time < timeMin)
    			return false;
    	}

    	if (calMaxDate != null){
    		long timeMax = calMaxDate.getTimeInMillis();
    		if (time > timeMax)
    			return false;
    	}

    	return true;
    }

    /*
     * カレンダーの時分秒をクリアする
     */
    private void cleanUpCalendarTime(GregorianCalendar cal){
    	if (cal == null)
    		return;

		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
    }

    /*
     * YYYY/MM/DD, MM/DD形式からGregorianCalendarクラスを取得する
     */
	private GregorianCalendar getCalendar(String date) {
		if (date == null)
			return null;

		GregorianCalendar cal = new GregorianCalendar();

		int year, month, day;

		// YYYY/MM/DD形式
		Matcher ma = Pattern.compile("^(\\d{4})/(\\d{2})/(\\d{2})$").matcher(date);
		if ( ma.find()) {
			year = Integer.parseInt(ma.group(1));
			month = Integer.parseInt(ma.group(2));
			day = Integer.parseInt(ma.group(3));
		}
		else{
			if (!allowNoYear)
				return null;

			// MM/DD形式
			Matcher mb = Pattern.compile("^(\\d{2})/(\\d{2})$").matcher(date);
			if (mb.find()){
				year = cal.get(Calendar.YEAR);
				month = Integer.parseInt(mb.group(1));
				day = Integer.parseInt(mb.group(2));
			}
			else
				return null;
		}

		// 年、月、日の値が有効な範囲であること
		if (!isValidDate(year, month, day))
			return null;

		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month-1);
		cal.set(Calendar.DATE, day);
		cleanUpCalendarTime(cal);

		return cal;
	}

	/*
	 * 年、月、日の値が有効な範囲であるかを返す
	 */
	private boolean isValidDate(int year, int month, int day){
		if (year < 1900 || year > 2100)
			return false;
		if (month < 1 || month > 12)
			return false;

		GregorianCalendar cal = new GregorianCalendar(year, month-1, 1, 0, 0, 0);
		if (day < 1 || day > cal.getActualMaximum(Calendar.DATE))
			return false;

		return true;
	}
}