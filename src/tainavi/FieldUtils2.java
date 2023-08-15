package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.NoSuchObjectException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import tainavi.Env.AAMode;
import tainavi.Env.DblClkCmd;
import tainavi.Env.SnapshotFmt;
import tainavi.Env.UpdateOn;
import tainavi.JTXTButton.FontStyle;
import tainavi.TVProgram.ProgOption;


/**
 * save()/load()は将来的にこちらに移行したい。　※未完成 
 */
@Deprecated
public class FieldUtils2 {
	
	public static void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;
	
	private static boolean debuglv2 = false;
	
	
	/*******************************************************************************
	 * 定数とか
	 ******************************************************************************/
	
	// 特殊値
	private static final String SPCH_LF = "$LF$";		// 改行値
	private static final String SPCH_NULL = "$NULL$";	// NULL値

	// 記号類
	private static final String SPMK_DOT = ".";			// メンバ名の区切り文字
	private static final String SPMK_CM = "#";			// コメント行
	private static final String SPMK_ALELM = "@";		// 配列のエレメントマーク
	private static final String SPMK_HMELM_K = "%k";	// 連想配列のエレメントマーク
	private static final String SPMK_HMELM_V = "%v";	// 連想配列のエレメントマーク
	private static final String SPMK_SEP = "=";			// key=value
	private static final String SPMK_LF = "\n";			// 改行文字
	
	// コメント
	private static final String SPHD_MOD = SPMK_CM+" MODIFIED : ";		// 更新日時
	private static final String SPHD_VER = SPMK_CM+" VERSION : ";		// 鯛ナビバージョン
	private static final String SPHD_CLS = SPMK_CM+" CLASS : ";			// ターゲットクラス
	private static final String SPHD_DEP = SPMK_CM+" DEPRECATED : ";	// 廃止されている変数
	//private static final String SPHD_UNSUP = SPMK_CM+" UNSUPPORTED : ";	// シリアライズできない変数
	private static final String SPHD_UNSOL = SPMK_CM+" UNSOLVED : ";	// パラメータの型がわからない
	private static final String SPHD_NOE = SPMK_CM+" NOELEMENT : ";		// エレメントを持たない配列・連想配列
	//private static final String SPHD_FL  = SPMK_CM+" MY FIELDS ";		// ここからフィールドブロック
	//private static final String SPHD_EL  = SPMK_CM+" MY ELEMENTS ";	// ここからエレメントブロック
	
	private static final String CMNT_AS = " AS ";
	
	// ログ関連
	private static final String MSGID = "[設定保存] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	
	
	/*******************************************************************************
	 * シリアライズ
	 ******************************************************************************/

	/**
	 * <P>自身がArrayList<T>やHashMap<K,V>やそのサブクラスだと、ジェネリクスのクラスが判定できないためシリアライズ不可。
	 * ArrayList<String>やHashMap<String,String>のサブクラスにしてやればＯＫ。
	 * <P>final、static修飾子のあるフィールドには未対応。
	 * <P>自身のスーパークラスのフィールドにも未対応。
	 * <P>値がサブクラスだと正しくデシリアライズできない。
	 */
	public static boolean save(String envText, Object root) {
		
		try {
			StringBuilder sb = new StringBuilder();
			
			// ヘッダ部
			sb.append(getHeader(root.getClass().getName()));
			
			// メンバ部
			String s = getBody("",root,root.getClass(),null);
			if ( s == null ) {
				return false;
			}
			sb.append(s);
			
			String text = sb.toString();
			
			if (debug) System.err.println(text);
			
	    	System.out.println(MSGID+"テキスト形式で保存します: "+envText);
	    	if ( ! CommonUtils.write2file(envText, text) ) {
	        	System.err.println(ERRID+"保存に失敗しました");
	        	return false;
	    	}
			
			return true;
		}
		catch ( Exception e ) {
			// ジェネリックの扱いに失敗したとか
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String getHeader(String name) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(SPHD_MOD);
		sb.append(CommonUtils.getDateTime(0));
		sb.append(SPMK_LF);
		sb.append(SPHD_VER);
		sb.append(VersionInfo.getVersionNumber());
		sb.append(SPMK_LF);
		sb.append(SPHD_CLS);
		sb.append(name);
		sb.append(SPMK_LF);
		sb.append(SPMK_LF);
		
		return sb.toString();
		
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static String getBody(String parent, Object root, Class rootClass, ParameterizedType rootParam) throws ClassCastException {
		
		StringBuilder sb = new StringBuilder();
		
		// null値ならオブジェクトの種類とか気にしなくていいよね
		if ( root == null ) {
			sb.append(parent);
			sb.append(SPMK_SEP);
			sb.append(SPCH_NULL);
			sb.append(SPMK_LF);
			return sb.toString();
		}
		
		// 自身がArrayListのサブクラスだった場合に子要素をひとつずつテキスト化する
		if ( root instanceof ArrayList ) {
			
			if ( ((ArrayList) root).size() == 0 ) {
				// 空の配列
				sb.append(SPHD_NOE);
				sb.append(parent);
				sb.append(SPMK_LF);
				return sb.toString();
			}
			
			// デシリアライズ可能かどうか、パラメータの型を確認する
			ParameterizedType paramType = ( rootParam != null ) ? rootParam : (ParameterizedType) root.getClass().getGenericSuperclass();
			if ( ! (paramType.getActualTypeArguments()[0] instanceof Class) ) {
				// 確認できなかったので、多分load()できない
				sb.append(SPHD_UNSOL);
				sb.append(parent);
				sb.append(SPMK_LF);
			}
			
			for ( Object obj : (ArrayList) root ) {
				String s;
				if ( obj != null ) {
					s = getBody(parent+SPMK_ALELM,obj,obj.getClass(),null);
				}
				else {
					s = getBody(parent+SPMK_ALELM,null,null,null);
				}
				if ( s == null ) {
					return null;
				}
				
				sb.append(s);
			}

			if ( root.getClass() == ArrayList.class ) {
				return sb.toString();
			}
			
			// フィールドのシリアライズに続く
		}
		// 自身がHashMapのサブクラスだった場合に子要素をひとつずつテキスト化する
		else if ( root instanceof HashMap ) {
			
			if ( ((HashMap) root).size() == 0 ) {
				// 空の連想配列
				sb.append(SPHD_NOE);
				sb.append(parent);
				sb.append(SPMK_LF);
				return sb.toString();
			}
			
			// デシリアライズ可能かどうか、パラメータの型を確認する
			ParameterizedType paramType = ( rootParam != null ) ? rootParam : (ParameterizedType) root.getClass().getGenericSuperclass();
			if ( ! (paramType.getActualTypeArguments()[0] instanceof Class &&
					paramType.getActualTypeArguments()[1] instanceof Class) ) {
				// 確認できなかったので、多分load()できない
				sb.append(SPHD_UNSOL);
				sb.append(parent);
				sb.append(SPMK_LF);
			}
			
			for ( Iterator it = ((HashMap) root).entrySet().iterator(); it.hasNext(); ) {
				Entry obj = (Entry) it.next();
				
				String ks;
				String vs;
				if ( obj.getKey() != null ) {
					ks = getBody(parent+SPMK_HMELM_K,obj.getKey(),obj.getKey().getClass(),null);
				}
				else {
					ks = getBody(parent+SPMK_HMELM_K,null,null,null);
				}
				if ( obj.getValue() != null ) {
					vs = getBody(parent+SPMK_HMELM_V,obj.getValue(),obj.getValue().getClass(),null);
				}
				else {
					vs = getBody(parent+SPMK_HMELM_V,null,null,null);
				}
				
				if ( ks == null || vs == null ) {
					return null;
				}
				
				sb.append(ks);
				sb.append(vs);
			}
			
			if ( root.getClass() == HashMap.class || root.getClass().getSuperclass() == HashMap.class ) {
				return sb.toString();
			}
			
			// フィールドのシリアライズに続く
		}
		// 特別な型はそのままでいーんじゃない？（自身）
		else {
			String vs = obj2str(root,rootClass);
			if ( vs != null ) {
				sb.append(parent);
				sb.append(SPMK_SEP);
				sb.append(vs);
				sb.append(SPMK_LF);
				
				return sb.toString();	// おわれ
			}
		}

		Field[] fd = rootClass.getDeclaredFields();
		for ( Field fx : fd ) {
			fx.setAccessible(true);
			
			// finalは駄目
			if ( Modifier.isFinal(fx.getModifiers()) ) {
				continue;
			}
			// staticは駄目
			if ( Modifier.isStatic(fx.getModifiers()) ) {
				continue;
			}

			try {
				
				// フィールドの解析
				String key = fx.getName();
				Class cls = fx.getType();
				Object obj = fx.get(root);

				// フィールド変数のジェネリクスを解決するための情報を取得する
				Type ty = fx.getGenericType();
				ParameterizedType pty = (ParameterizedType) ((ty instanceof ParameterizedType) ? ty : null);

				// 使用禁止は駄目
				if ( fx.getAnnotation(Deprecated.class) != null ) {
					sb.append(SPHD_DEP);
					sb.append(parent);
					sb.append(SPMK_DOT);
					sb.append(key);
					sb.append(CMNT_AS);
					sb.append(cls.getName());
					sb.append(SPMK_LF);
					continue;
				}
				
				// 特別な型はそのままでいーんじゃない？（フィールド）
				{
					String vs = obj2str(obj,cls);
					if ( vs != null ) {
						sb.append(parent);
						sb.append(SPMK_DOT);
						sb.append(key);
						sb.append(SPMK_SEP);
						sb.append(vs);
						sb.append(SPMK_LF);
						continue;
					}
				}
				
				// 掘り下げる
				sb.append(getBody(parent+SPMK_DOT+key,obj,cls,pty));
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		return sb.toString();
	}
	
	
	/*******************************************************************************
	 * デシリアライズ
	 ******************************************************************************/
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean load(String envText, Object root) {
		
    	if ( ! new File(envText).exists() ) {
    		return false;
    	}
    	
    	System.out.println(MSGID+"テキスト形式で読み込みます: "+envText);
    	String buf = CommonUtils.read4file(envText, false);
    	if ( buf == null ) {
    		return false;
    	}
    	
    	// ArrayListのサブクラスならばパラメータ型を控えておく 
    	Class alparam = null;
    	Field[] alfd = null;
    	Class hmparam_k = null;
    	Class hmparam_v = null;
    	Field[] hmfd_k = null;
    	Field[] hmfd_v = null;
    	try {
	    	if ( root instanceof ArrayList ) {
				ParameterizedType paramType = (ParameterizedType) root.getClass().getGenericSuperclass();
				alparam = (Class) paramType.getActualTypeArguments()[0];
				alfd = alparam.getDeclaredFields();
	    	}
	    	else if ( root instanceof HashMap ) {
				ParameterizedType paramType = (ParameterizedType) root.getClass().getGenericSuperclass();
				hmparam_k = (Class) paramType.getActualTypeArguments()[0];
				hmparam_v = (Class) paramType.getActualTypeArguments()[1];
				hmfd_k = hmparam_k.getDeclaredFields();
				hmfd_v = hmparam_v.getDeclaredFields();
	    	}
    	}
    	catch ( ClassCastException e ) {
    		e.printStackTrace();
    		return false;
    	}
    	
    	// フィールド
    	Field[] tmpFd = root.getClass().getDeclaredFields();
    	
    	Object tmpRoot = root;
    	Object keyRoot = null;
    	String[] lines = buf.split(SPMK_LF);
    	for ( int lineno=1; lines.length>=lineno; lineno++ ) {
    		
    		String str = lines[lineno-1];
    		
    		if ( str.startsWith(SPHD_MOD) ) {
    			System.out.println(MSGID+str);
    			continue;
    		}
    		
    		if ( str.startsWith(SPMK_CM) || str.matches("^\\s*$") ) {
    			continue;
    		}
    		
    		if ( str.startsWith(SPMK_ALELM) ) {
    			if ( alparam == null ) {
    				return true;	// 終了していいよ
    			}
    			
    			// 自身がArrayListのサブクラスだった場合に子要素をひとつずつオブジェクト化する
    			try {
	    			if ( str.startsWith(SPMK_ALELM+SPMK_SEP) ) {
	    				// 特殊型の場合
	    				tmpRoot = str2obj(str.substring((SPMK_ALELM+SPMK_SEP).length()),alparam);
	    				
	    				((ArrayList) root).add(tmpRoot);
	    			}
	    			else {
	    				// 通常型の場合
	    				tmpRoot = alparam.newInstance();
	    				tmpFd = alfd;
	    				
	    				((ArrayList) root).add(tmpRoot);
	    			}
	    			continue;
    			}
    			catch ( Exception e ) {
    				System.err.println(ERRID+"オブジェクトのインスタンスが作成できません.");
    				e.printStackTrace();
    				return false;
    			}
    		}
    		else if ( str.startsWith(SPMK_HMELM_K) ) {
    			if ( hmparam_k == null ) {
    				return true;	// 終了していいよ
    			}
    			
    			// 自身がHashMapのサブクラスだった場合にキー要素をオブジェクト化する
    			try {
	    			if ( str.startsWith(SPMK_HMELM_K+SPMK_SEP) ) {
	    				// 特殊型の場合
	    				keyRoot = str2obj(str.substring((SPMK_HMELM_K+SPMK_SEP).length()),hmparam_k);
	    				tmpRoot = null;
	    				tmpFd = null;
	    			}
	    			else {
	    				// 通常型の場合
	    				keyRoot = tmpRoot = hmparam_k.newInstance();
	    				tmpFd = hmfd_k;
	    			}
	    			continue;
    			}
    			catch ( Exception e ) {
    				System.err.println(ERRID+"オブジェクトのインスタンスが作成できません.");
    				e.printStackTrace();
    				return false;
    			}
    		}
    		else if ( str.startsWith(SPMK_HMELM_V) ) {
    			if ( hmparam_v == null ) {
    				return true;	// 終了していいよ
    			}
    			
    			// 自身がHashMapのサブクラスだった場合にバリュー要素をオブジェクト化する
    			try {
	    			if ( str.startsWith(SPMK_HMELM_V+SPMK_SEP) ) {
	    				// 特殊型の場合
	    				tmpRoot = str2obj(str.substring((SPMK_HMELM_V+SPMK_SEP).length()),hmparam_v);
	    				
	    				((HashMap) root).put(keyRoot, tmpRoot);
	    				keyRoot = tmpRoot = null;
	    				tmpFd = null;
	    			}
	    			else {
	    				// 通常型の場合
	    				tmpRoot = hmparam_v.newInstance();
	    				tmpFd = hmfd_v;
	    				
	    				((HashMap) root).put(keyRoot,tmpRoot);
	    				keyRoot = null;
	    			}
	    			continue;
    			}
    			catch ( Exception e ) {
    				System.err.println(ERRID+"オブジェクトのインスタンスが作成できません.");
    				e.printStackTrace();
    				return false;
    			}
    		}
    		
    		String[] a = str.split(SPMK_SEP, 2);
    		if ( a.length != 2 ) {
    			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
    			return false;
    		}
    		
    		if ( ! setBody(tmpRoot,tmpFd,a,envText,lineno,str) ) {
    			return false;
    		}
    		
    	}
    	
		return true;
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean setBody(Object root, Field[] fd, String[] a, String envText, int lineno, String str) {
		
		Field fx = null;
		for ( Field f : fd ) {
			if ( f.getName().equals(a[0].replaceFirst("\\[\\d+\\]$","")) ) {
				fx = f;
				break;
			}
		}
		if ( fx == null ) {
			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
			return false;	// エラー
		}
		
		fx.setAccessible(true);
		
		// static、finalで修飾された変数
		if ( Modifier.isFinal(fx.getModifiers()) ) {
			return true;	// スキップ
		}
		if ( Modifier.isStatic(fx.getModifiers()) ) {
			return true;	// スキップ
		}
		
		// ディスコンな変数
		if ( fx.getAnnotation(Deprecated.class) != null ) {
			System.out.println(MSGID+SPHD_DEP+fx.getName());
			return true;	// スキップ
		}
		
		Class cls = fx.getType();
		
		try {
			if ( cls == ArrayList.class ) {
				ArrayList list = (ArrayList) fx.get(root);
				if ( fx.get(root) == null ) {
					System.out.println(ERRID+"初期化されていないフィールド: "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
					return false;	// エラー
				}
				
				if ( a[0].endsWith("[0]") ) {
					// no element.
					list.clear();
				}
				else {
					if ( a[0].endsWith("[1]") ) {
						// newした時のデフォルト値が入っているからリセットじゃー
						list.clear();
					}
					ParameterizedType paramType = (ParameterizedType) fx.getGenericType();
					Class ocls = (Class) paramType.getActualTypeArguments()[0];
					Object obj = str2obj(a[1],ocls);
					list.add(obj);
				}
			}
			else if ( cls == HashMap.class ) {
				HashMap map = (HashMap) fx.get(root);
				if ( fx.get(root) == null ) {
					System.out.println(ERRID+"初期化されていないフィールド: "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
					return false;	// エラー
				}
				String[] b = a[1].split(Pattern.quote(SPCH_LF),2);
				if ( b.length != 2 ) {
        			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
					return false;	// エラー
				}
				
				if ( a[0].endsWith("[0]") ) {
					// no element.
					map.clear();
				}
				else {
					if ( a[0].endsWith("[1]") ) {
						// newした時のデフォルト値が入っているからリセットじゃー
						map.clear();
					}
					ParameterizedType paramType = (ParameterizedType) fx.getGenericType();
					Class kcls = (Class) paramType.getActualTypeArguments()[0];
					Class vcls = (Class) paramType.getActualTypeArguments()[1];
					Object k = str2obj(b[0],kcls);
					Object v = str2obj(b[1],vcls);
					map.put(k,v);
				}
			}
			else {
				Object obj = str2obj(a[1],cls);
				fx.set(root, obj);
			}
			
			return true;	// 成功
		}
		catch (UnsupportedOperationException e) {
			System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
		}
		catch (NoSuchObjectException e) {
			System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
		}
		catch (ClassCastException e) {
			System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
		}
		catch (Exception e) {
			// エラー項目はスキップする
			System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
			e.printStackTrace();
		}
		
		return false;	//エラー
	}
	
	/*******************************************************************************
	 * シリアライズ（部品）
	 ******************************************************************************/
	
	@SuppressWarnings("rawtypes")
	private static String obj2str(Object obj, Class cls) {
		// 基本的なオブジェクト
		if ( cls == String.class ) {
			return(obj == null ? SPCH_NULL : ((String) obj).replaceAll(SPMK_LF, SPCH_LF));
		}
		else if ( cls == boolean.class || cls == Boolean.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Boolean) obj));
		}
		else if ( cls == int.class || cls == Integer.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Integer) obj));
		}
		else if ( cls == float.class || cls == Float.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Float) obj));
		}
		else if ( cls == double.class || cls == Double.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Float) obj));
		}
		// 特別扱いしたいオブジェクト
		else if ( cls == Color.class ) {
			return(obj == null ? SPCH_NULL : CommonUtils.color2str((Color) obj));
		}
		else if ( cls.isEnum() ) {
			return(obj == null ? SPCH_NULL : obj.toString());
		}
		/*
		else if ( cls == Rectangle.class ) {
			Rectangle ra = (Rectangle) obj;
			return(obj == null ? SPCH_NULL : String.format("%d,%d,%d,%d",ra.x,ra.y,ra.width,ra.height));
		}
		else if ( cls == DblClkCmd.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf(((DblClkCmd) obj).getId()));
		}
		else if ( cls == SnapshotFmt.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf(((SnapshotFmt) obj).getId()));
		}
		else if ( cls == AAMode.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf(((AAMode) obj).getId()));
		}
		else if ( cls == UpdateOn.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf(((UpdateOn) obj).getId()));
		}
		else if ( cls == FontStyle.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf(((FontStyle) obj).getId()));
		}
		else if ( cls == TextValueSet.class ) {
			TextValueSet t = (TextValueSet) obj;
			return(obj == null ? SPCH_NULL : t.getText()+SPCH_LF+t.getValue());
		}
		else if ( cls == ProgOption.class ) {
			return(obj == null ? SPCH_NULL : ((ProgOption) obj).toString());
		}
		/*
		else if ( obj instanceof Entry ) {
			Entry t = (Entry) obj;
			String k = obj2str(t.getKey(), t.getKey().getClass());
			String v = obj2str(t.getValue(), t.getValue().getClass());
			return(obj == null ? SPCH_NULL : k+SPCH_LF+v);
		}
		*/
		
		return null;
	}
	
	
	/*******************************************************************************
	 * デシリアライズ（部品）
	 * @throws NoSuchObjectException 
	 ******************************************************************************/
	
	@SuppressWarnings("rawtypes")
	private static Object str2obj(String str, Class cls) throws UnsupportedOperationException, NoSuchObjectException {
		if ( cls == String.class ) {
			return(str.equals(SPCH_NULL) ? null : str.replaceAll(SPCH_LF, SPMK_LF));
		}
		else if ( cls == int.class || cls == Integer.class ) {
			try {
				return(str.equals(SPCH_NULL) ? null : Integer.valueOf(str));
			}
			catch ( NumberFormatException e ) {
				throw new UnsupportedOperationException("数値に変換できない");
			}
		}
		else if ( cls == float.class || cls == Float.class ) {
			try {
				return(str.equals(SPCH_NULL) ? null : Float.valueOf(str));
			}
			catch ( NumberFormatException e ) {
				throw new UnsupportedOperationException("数値に変換できない");
			}
		}
		else if ( cls == boolean.class || cls == Boolean.class ) {
			return(str.equals(SPCH_NULL) ? null : Boolean.valueOf(str));
		}
		else if ( cls == Rectangle.class ) {
			try {
				String[] a = str.split(",");
				if ( a.length == 4 ) {
					return(str.equals(SPCH_NULL) ? null : new Rectangle(Integer.valueOf(a[0]),Integer.valueOf(a[1]),Integer.valueOf(a[2]),Integer.valueOf(a[3])));
				}
				else {
					throw new UnsupportedOperationException("変換できない");
				}
			}
			catch ( NumberFormatException e ) {
				throw new UnsupportedOperationException("数値に変換できない");
			}
		}
		else if ( cls == Color.class ) {
			Color c = CommonUtils.str2color(str);
			if ( c != null ) {
				return(c);
			}
			else {
				throw new UnsupportedOperationException("色に変換できない ");
			}
		}
		else if ( cls == DblClkCmd.class ) {
			DblClkCmd dcc = DblClkCmd.get(str);
			if ( dcc != null ) {
				return(dcc);
			}
			else {
				throw new UnsupportedOperationException("変換できない");
			}
		}
		else if ( cls == SnapshotFmt.class ) {
			SnapshotFmt sf = SnapshotFmt.get(str);
			if ( sf != null ) {
				return(sf);
			}
			else {
				throw new UnsupportedOperationException("変換できない");
			}
		}
		else if ( cls == AAMode.class ) {
			AAMode aam = AAMode.get(str);
			if ( aam != null ) {
				return(aam);
			}
			else {
				throw new UnsupportedOperationException("変換できない");
			}
		}
		else if ( cls == UpdateOn.class ) {
			UpdateOn uo = UpdateOn.get(str);
			if ( uo != null ) {
				return(uo);
			}
			else {
				throw new UnsupportedOperationException(ERRID+"変換できない");
			}
		}
		else if ( cls == FontStyle.class ) {
			FontStyle fs = FontStyle.get(str);
			if ( fs != null ) {
				return(fs);
			}
			else {
				throw new UnsupportedOperationException("変換できない");
			}
		}
		else if ( cls == TextValueSet.class ) {
			String a[] = str.split(Pattern.quote(SPCH_LF),2);
			if ( a.length == 2 ) {
				TextValueSet t = new TextValueSet();
				t.setText(a[0]);
				t.setValue(a[1]);
				return (t);
			}
			else {
				throw new UnsupportedOperationException("変換できない");
			}
		}
		else if ( cls == ProgOption.class ) {
			for ( ProgOption po : ProgOption.values() ) {
				if ( po.toString().equals(str) ) {
					return po;
				}
			}
			throw new UnsupportedOperationException("変換できない");
		}
		
		throw new NoSuchObjectException("未対応の項目");
	}
	
}
