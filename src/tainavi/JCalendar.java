package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * カレンダー部品
 */
public class JCalendar extends JPanel {

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
    private static final int DAY_FONT_SIZE = 14;
    private static final int DAY_WIDTH = 33;
    private static final int DAY_HEIGHT = 25;

    private static final int YEAR_SPINNER_WIDTH = 85;
    private static final int MONTH_SPINNER_WIDTH = 70;

    private static final int YM_FONT_SIZE = 10;
    private static final int YM_BUTTON_WIDTH = DAY_WIDTH;
    private static final int YM_LABEL_WIDTH = DAY_WIDTH*3;
    private static final int YM_HEIGHT = DAY_HEIGHT;

    private static final int COLNUM = 7;
    private static final int ROWNUM = 6;

    private static final String[] WEEK_NAMES =
    		new String[] {"日", "月", "火", "水", "木", "金", "土"};

    private static final Border BORDER_NORMAL = null;
    private static final Border BORDER_SELECTED = new LineBorder(Color.BLACK, 2);

    enum Style{
    	Spinner,
    	Button,
    };

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
    private JSpinner  jSpinnerYear = null;
    private JSpinner  jSpinnerMonth = null;

    private JButton jButtonYearMinus = null;
    private JButton jButtonMonthMinus = null;
    private JLabel jLabelYearMonth = null;
    private JButton jButtonMonthPlus = null;
    private JButton jButtonYearPlus = null;

    private JLabel[]    jLabelWeeks = new JLabel[COLNUM];
    private JButton[][]  jButtonDays = new JButton[ROWNUM][COLNUM];
    private JButton jButtonThisMonth = null;

    private Font fontNormal = null;
    private Font fontToday = null;
    private Font fontWeek = null;
    private Font fontYearMonth = null;

    private CalendarListener listener = null;

    private GregorianCalendar calSelDate = null;
    private GregorianCalendar calMinDate = null;
    private GregorianCalendar calMaxDate = null;

    private Style style = Style.Button;
    private boolean popupMenuEnabled = true;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
    public JCalendar() {
    	this(Style.Button, true);
    }

	public JCalendar(Style s, boolean b){
    	style = s;
    	popupMenuEnabled = b;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    	createFont();

        add(createYearMonthPanel());
        add(createDayPanel());

        setSelectedDate((GregorianCalendar)null);
    }

	/*******************************************************************************
	 * static 公開メソッド
	 ******************************************************************************/
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
     * 選択日をYYYY/MM/DD形式で指定する
     */
    public void setSelectedDate(String date){
    	setSelectedDate(getCalendar(date));
    }

    /*
     * 選択日をGregorianCalendarクラスで指定する
     */
    public void setSelectedDate(GregorianCalendar cal){
    	calSelDate = cal;

    	if (cal == null)
    		cal = new GregorianCalendar();

    	setYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1);

        updateDayButtons();
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
    	calMinDate = cal;
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
    	calMaxDate = cal;
    }

    /*
     * キーボードフォーカスを取得する
     *
     * (非 Javadoc)
     * @see javax.swing.JComponent#requestFocus()
     */
    @Override
    public void requestFocus(){
    	if (style == Style.Spinner){
    		JTextField tm = ((JSpinner.DefaultEditor)jSpinnerMonth.getEditor()).getTextField();
    		tm.requestFocus();
    	}
    }

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
    /*
     * フォントを生成する
     */
    private void createFont(){
    	JButton b = new JButton();

		Font font = b.getFont();
		fontNormal = font.deriveFont(font.getStyle() & ~Font.BOLD, DAY_FONT_SIZE);
		fontToday = font.deriveFont(font.getStyle() | Font.BOLD, DAY_FONT_SIZE);
		fontWeek = font.deriveFont(font.getStyle() | Font.BOLD, DAY_FONT_SIZE);
		fontYearMonth = font.deriveFont(font.getStyle() | Font.BOLD, YM_FONT_SIZE);
    }

    /*
     * 年月選択用パネル
     */
    private JPanel createYearMonthPanel(){
        JPanel panel = new JPanel();

        if (style == Style.Spinner){
        	FlowLayout layout = new FlowLayout();
        	layout.setHgap(10);
        	layout.setVgap(0);
            panel.setLayout(layout);

	        jSpinnerYear = createYearSpinner();
	        jSpinnerMonth = createMonthSpinner();

	        panel.add(jSpinnerYear);
	        panel.add(jSpinnerMonth);
        }
        else{
        	FlowLayout layout = new FlowLayout();
        	layout.setHgap(0);
        	layout.setVgap(0);
            panel.setLayout(layout);

        	jButtonYearMinus = createYMButton("<<", "前年へ移動", ma_yearMinus);
        	jButtonMonthMinus = createYMButton("<", "前月へ移動", ma_monthMinus);
        	jLabelYearMonth = createYMLabel();
        	jButtonMonthPlus = createYMButton(">",  "翌月へ移動", ma_monthPlus);
        	jButtonYearPlus = createYMButton(">>",  "翌年へ移動", ma_yearPlus);

        	panel.add(jButtonYearMinus);
        	panel.add(jButtonMonthMinus);
        	panel.add(jLabelYearMonth);
        	panel.add(jButtonMonthPlus);
        	panel.add(jButtonYearPlus);
        }

        return panel;
    }

    /*
     * 年選択用スピナーを作成する
     */
    private JSpinner createYearSpinner(){
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerNumberModel(2021, 2000, 2999, 1 ));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#年"));
        spinner.addChangeListener(cl_yearChanged);

        Dimension dy = spinner.getPreferredSize();
        dy.width = YEAR_SPINNER_WIDTH;
        spinner.setPreferredSize(dy);

    	return spinner;
    }

    /*
     * 月選択用スピナーを作成する
     */
    private JSpinner createMonthSpinner() {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerNumberModel(1, 0, 13, 1 ));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#月"));
        spinner.addChangeListener(cl_monthChanged);

        Dimension dm = spinner.getPreferredSize();
        dm.width = MONTH_SPINNER_WIDTH;
        spinner.setPreferredSize(dm);

        return spinner;
    }

    /*
     * 年月用ボタンを作成する
     */
    private JButton createYMButton(String text, String tooltip, MouseAdapter ma){
    	JButton button = new JButton();

    	button.setText(text);
        button.setPreferredSize(new Dimension(YM_BUTTON_WIDTH, YM_HEIGHT));
        button.setFont(fontYearMonth);
        button.setToolTipText(tooltip + (popupMenuEnabled ? "(右クリックでメニュー)" : ""));

        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);

        button.addMouseListener(ma_flatButton);
        button.addMouseListener(ma);

        return button;
    }

    /*
     * 年月用ラベルを作成する
     */
    private JLabel createYMLabel(){
    	JLabel label = new JLabel();
    	label.setBackground(Color.LIGHT_GRAY);
    	label.setPreferredSize(new Dimension(YM_LABEL_WIDTH, YM_HEIGHT));
        label.setFont(fontNormal);
    	label.setHorizontalAlignment(JLabel.CENTER);
        label.setToolTipText(popupMenuEnabled ? "(右クリックでメニュー)" : "");
    	label.addMouseListener(ma_ymLabel);

    	return label;
    }

    /*
     * カレンダーパネル
     */
    private JPanel createDayPanel(){
        JPanel panel = new JPanel();

        panel.setLayout(null);

        for( int c=0; c<COLNUM; c++) {
        	JLabel label =  createWeekLabel(c);
        	label.setBounds(DAY_WIDTH*c, 0, DAY_WIDTH, DAY_HEIGHT);
            panel.add(label);
            jLabelWeeks[c] = label;
        }

        JCalendar cal = this;

        ActionListener al_daySelected = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();

    			String date = String.format("%04d/%02d/%02d",
    					getSelectedYear(),
    					getSelectedMonth(),
    					Integer.valueOf(button.getText()));

    			if (listener != null)
    				listener.notifyDateChange(cal, date);
			}
        };

        for( int r=0; r<ROWNUM; r++) {
            for( int c=0; c<COLNUM; c++) {
            	JButton button = createDayButton(c, al_daySelected);
            	button.setBounds(DAY_WIDTH*c, DAY_HEIGHT*(r+1), DAY_WIDTH, DAY_HEIGHT);
            	panel.add(button);
                jButtonDays[r][c] = button;
            }
        }

    	JButton button = createDayButton(1, al_thisMonthButton);
        button.setFont(fontNormal);
        button.setText("今月へ");
    	button.setBounds(DAY_WIDTH*5, DAY_HEIGHT*ROWNUM, DAY_WIDTH*2, DAY_HEIGHT);
    	panel.add(button);
        jButtonThisMonth = button;

        panel.setPreferredSize(new Dimension(DAY_WIDTH*COLNUM, DAY_HEIGHT*(ROWNUM+1)));

        return panel;
    }

    /*
     * 曜日用ラベルを作成する
     */
    private JLabel createWeekLabel(int c){
       	JLabel label =  new JLabel(WEEK_NAMES[c]);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(DAY_WIDTH, DAY_HEIGHT));
        label.setOpaque(true);
        label.setBorder(null);
        label.setForeground(c == 0 ? Color.RED : c == 6 ? Color.BLUE : Color.BLACK);
        label.setFont(fontWeek);

        return label;
    }

    /*
     * 日付用ボタンを作成する
     */
    private JButton createDayButton(int c, ActionListener al){
    	JButton button = new JButton();
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(DAY_WIDTH, DAY_HEIGHT));
        button.setForeground(c == 0 ? Color.RED : c == 6 ? Color.BLUE : Color.BLACK);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);

        button.addMouseListener(ma_flatButton);
        button.addActionListener(al);

        return button;
    }

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/
    /*
     * 年の選択変更
     */
    private ChangeListener cl_yearChanged = new ChangeListener(){
        public void stateChanged(ChangeEvent e) {
        	updateDayButtons();
        }
    };

    /*
     * 月の選択変更
     */
    private ChangeListener cl_monthChanged = new ChangeListener(){
        public void stateChanged(ChangeEvent e) {
        	int month = getSelectedMonth();
        	if (month >= 13){
        		jSpinnerYear.setValue((int)jSpinnerYear.getValue()+1);
        		jSpinnerMonth.setValue(1);
        	}
        	else if (month <= 0){
        		jSpinnerYear.setValue((int)jSpinnerYear.getValue()-1);
        		jSpinnerMonth.setValue(12);
        	}

        	updateDayButtons();
        }
    };

    /*
     * 前年へ
     */
    private MouseAdapter ma_yearMinus = new MouseAdapter(){
		@Override
		public void mouseClicked(MouseEvent e) {
			setYearMonth(getSelectedYear()-1, getSelectedMonth());
        	updateDayButtons();
		}
    };

    /*
     * 前月へ
     */
    private MouseAdapter ma_monthMinus = new MouseAdapter(){
		@Override
		public void mouseClicked(MouseEvent e) {
			int year = getSelectedYear();
			int month = getSelectedMonth()-1;
			if (month <= 0){
				year --;
				month = 12;
			}

			setYearMonth(year, month);
        	updateDayButtons();
		}
    };

    /*
     * 翌月へ
     */
    private MouseAdapter ma_monthPlus = new MouseAdapter(){
		@Override
		public void mouseClicked(MouseEvent e) {
			int year = getSelectedYear();
			int month = getSelectedMonth()+1;
			if (month >= 13){
				year ++;
				month = 1;
			}

			setYearMonth(year, month);
        	updateDayButtons();
		}
    };

    /*
     * 翌年へ
     */
    private MouseAdapter ma_yearPlus = new MouseAdapter(){
		@Override
		public void mouseClicked(MouseEvent e) {
			setYearMonth(getSelectedYear()+1, getSelectedMonth());
        	updateDayButtons();
		}
    };

    /*
     * 年月ラベルのマウスリスナー
     */
    MouseAdapter ma_ymLabel = new MouseAdapter(){
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3){
				JLabel label = (JLabel)e.getSource();
				Dimension sz = label.getSize();

				if (e.getX() < sz.width/2){
					showYearMenu(label, e.getX(), e.getY());
				}
				else{
					showMonthMenu(label, e.getX(), e.getY());
				}
			}
		}
    };

    /*
     * フラットボタンのマウスリスナー
     */
    MouseAdapter ma_flatButton = new MouseAdapter(){
		@Override
		public void mousePressed(MouseEvent e) {
			JButton button = (JButton) e.getSource();
			if (e.getButton() == MouseEvent.BUTTON3){
				if (button == jButtonYearMinus || button == jButtonYearPlus)
					showYearMenu(button, e.getX(), e.getY());
				else if (button == jButtonMonthMinus || button == jButtonMonthPlus)
					showMonthMenu(button, e.getX(), e.getY());
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			JButton button = (JButton) e.getSource();
			if (button.isEnabled())
				button.setContentAreaFilled(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			JButton button = (JButton) e.getSource();
			if (button.isEnabled())
				button.setContentAreaFilled(false);
		}
    };

    /*
     * 今月へ
     */
    private ActionListener al_thisMonthButton = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent e) {
	    	GregorianCalendar cal = new GregorianCalendar();

	    	setYearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1);
        	updateDayButtons();
		}
    };

    /*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
    /*
     * カレンダーを更新する
     */
    private void updateDayButtons(){
    	int year = getSelectedYear();
    	int month = getSelectedMonth();

        GregorianCalendar cal = new GregorianCalendar();
        boolean isThisMonth = cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month-1;

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month-1);
        cal.set(Calendar.DATE, 1);

        int dow0 = cal.get(Calendar.DAY_OF_WEEK);

        clearDayButtons();

        int dayNum = cal.getActualMaximum(Calendar.DATE);
        for( int day=1; day<=dayNum; day++ ) {
           	int no = day + dow0 - 2;
           	JButton button = jButtonDays[no/COLNUM][no%COLNUM];

           	button.setText(Integer.toString(day));
        }

        for( int r=0; r<ROWNUM; r++) {
            for( int c=0; c<COLNUM; c++) {
                JButton button = jButtonDays[r][c];

            	int day = r*COLNUM + c + 2 - dow0;
                boolean valid = isValidDate(year, month, day);
                boolean selected = isSelectedDate(year, month, day);
                boolean today = isTodayDate(year, month, day);
                boolean holiday = isHolidayDate(year, month, day);

    			button.setFont(today ? fontToday : fontNormal);
                button.setBorder(selected ? BORDER_SELECTED : BORDER_NORMAL);
                button.setVisible( button.getText().length() != 0 );
                button.setEnabled(valid);
                button.setBorderPainted(selected);
                button.setContentAreaFilled(!valid);
                button.setForeground((c == 0 || holiday) ? Color.RED : c == 6 ? Color.BLUE : Color.BLACK);
            }
        }

        jButtonThisMonth.setVisible(!isThisMonth);
    }

    /*
     * 日付ボタンのテキストをクリアする
     */
    private void clearDayButtons(){
        for( int r=0; r<ROWNUM; r++) {
            for( int c=0; c<COLNUM; c++) {
                JButton button = jButtonDays[r][c];
                button.setText("");
            }
        }
    }

    /*
     * 西暦年選択メニューを表示する
     */
    private void showYearMenu(JComponent comp, int x, int y){
    	if (!popupMenuEnabled)
    		return;

		// メニューの作成
		JPopupMenu menu = new JPopupMenu();

		ActionListener al = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JMenuItem item = (JMenuItem) e.getSource();

				Matcher ma = Pattern.compile("(\\d+)年").matcher(item.getText());
				if (ma.find()){
					setYearMonth(Integer.parseInt(ma.group(1)), getSelectedMonth());
		        	updateDayButtons();
				}
			}

		};

        Calendar cal = new GregorianCalendar();

		int yearSel = getSelectedYear();
		for (int year=yearSel-6; year<=yearSel+6; year++){
			JMenuItem item = new JMenuItem(year + "年");
			if (year == cal.get(Calendar.YEAR)){
				Font f = item.getFont();
				item.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
			}
			item.addActionListener(al);
			menu.add(item);
		}

		menu.show(comp, x, y);
    }

    /*
     * 月選択メニューを表示する
     */
    private void showMonthMenu(JComponent comp, int x, int y){
    	if (!popupMenuEnabled)
    		return;

		// メニューの作成
		JPopupMenu menu = new JPopupMenu();

		ActionListener al = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JMenuItem item = (JMenuItem) e.getSource();

				Matcher ma = Pattern.compile("(\\d+)月").matcher(item.getText());
				if (ma.find()){
					setYearMonth(getSelectedYear(), Integer.parseInt(ma.group(1)));
		        	updateDayButtons();
				}
			}

		};

        Calendar cal = new GregorianCalendar();
        int yearSel = getSelectedYear();
		for (int month=1; month<=12; month++){
			JMenuItem item = new JMenuItem(month + "月");
			if (yearSel == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH)+1){
				Font f = item.getFont();
				item.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
			}
			item.addActionListener(al);
			menu.add(item);
		}

		menu.show(comp, x, y);
    }

    /*
     * 選択中の年月を部品に反映する
     */
    private void setYearMonth(int year, int month){
    	if (style == Style.Spinner){
            jSpinnerYear.setValue(year);
            jSpinnerMonth.setValue(month);
    	}
    	else{
    		jLabelYearMonth.setText(String.format("%d年%d月", year, month));

            Calendar cal = new GregorianCalendar();
			Font f = jLabelYearMonth.getFont();
			if (year == cal.get(Calendar.YEAR) && month == cal.get(Calendar.MONTH)+1){
				jLabelYearMonth.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
			}
			else{
				jLabelYearMonth.setFont(f.deriveFont(f.getStyle()&~Font.BOLD));
			}
    	}
    }

    /*
     * 選択中の西暦年を取得する
     */
    private int getSelectedYear(){
    	if (style == Style.Spinner)
    		return (int)jSpinnerYear.getValue();
    	else{
    		Matcher ma = Pattern.compile("(\\d+)年(\\d+)月").matcher(jLabelYearMonth.getText());
			return ma.find() ? Integer.parseInt(ma.group(1)) : 2000;
    	}
    }

    /*
     * 選択中の月を取得する
     */
    private int getSelectedMonth(){
    	if (style == Style.Spinner)
    		return (int)jSpinnerMonth.getValue();
    	else{
    		Matcher ma = Pattern.compile("(\\d+)年(\\d+)月").matcher(jLabelYearMonth.getText());
			return ma.find() ? Integer.parseInt(ma.group(2)) : 1;
    	}
    }

    /*
     * 指定された日が選択日かどうかを取得する
     */
    private boolean isSelectedDate(int year, int month, int day){
    	if (calSelDate == null)
    		return false;

        return
        		calSelDate.get(Calendar.YEAR) == year &&
        		calSelDate.get(Calendar.MONTH) == month-1 &&
        		calSelDate.get(Calendar.DATE) == day;
    }

    /*
     * 指定された日が当日かどうかを取得する
     */
    private boolean isTodayDate(int year, int month, int day){
        Calendar cal = new GregorianCalendar();

        return
        		cal.get(Calendar.YEAR) == year &&
        		cal.get(Calendar.MONTH) == month-1 &&
        		cal.get(Calendar.DATE) == day;
    }

    /*
     * 指定された日が有効かどうかを返す
     */
    private boolean isValidDate(int year, int month, int day){
    	GregorianCalendar cal = new GregorianCalendar(year, month-1, day);
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
     * 指定された日が祝日かどうかを返す
     */
    private boolean isHolidayDate(int year, int month, int day){
    	if (listener != null)
    		return listener.isHoliday(year, month, day);
    	else
    		return false;
    }

    /*
     * YYYY/MM/DD形式からGregorianCalendarクラスを取得する
     */
	public static GregorianCalendar getCalendar(String date) {
		Matcher ma = Pattern.compile("^(\\d{4})/(\\d{1,2})/(\\d{1,2})$").matcher(date);
		if ( ! ma.find()) {
			return null;
		}

		return new GregorianCalendar(
				Integer.valueOf(ma.group(1)),
				Integer.valueOf(ma.group(2))-1,
				Integer.valueOf(ma.group(3)),
				0,
				0,
				0);
	}
}