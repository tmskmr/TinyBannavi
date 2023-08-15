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
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import tainavi.Env.AAMode;
import tainavi.Env.DblClkCmd;
import tainavi.Env.SnapshotFmt;
import tainavi.Env.UpdateOn;
import tainavi.JTXTButton.FontStyle;
import tainavi.TVProgram.ProgOption;


/**
 * オブジェクトのフィールドを操作するユーティリティーメソッド群
 *
 * @version 3.22.2b CommonUtils#FieldCopy()を{@link #deepCopy(Object, Object)}に移動<BR>
 */
public class FieldUtils {

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	private static boolean debuglv2 = false;

	/*******************************************************************************
	 * 定数とか
	 ******************************************************************************/

	private static final String SPCH_LF = "$LF$";
	private static final String SPCH_NULL = "$NULL$";

	private static final String SPMK_CM = "#";
	private static final String SPMK_SEP = "=";
	private static final String SPMK_LF = "\n";

	private static final String SPHD_MOD = SPMK_CM+" MODIFIED : ";
	private static final String SPHD_VER = SPMK_CM+" VERSION : ";
	private static final String SPHD_DEP = SPMK_CM+" DEPRECATED : ";
	private static final String SPHD_UNS = SPMK_CM+" UNSUPPORTED : ";
	private static final String SPHD_NOE = SPMK_CM+" NOELEMENT : ";

	private static final String CMNT_AS = " AS ";

	private static final String MSGID = "[設定保存] ";
	private static final String ERRID = "[ERROR]"+MSGID;

	/*******************************************************************************
	 * 保存
	 ******************************************************************************/

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean save(String envText, Object root) {

		StringBuilder sb = new StringBuilder();

		sb.append(SPHD_MOD);
		sb.append(CommonUtils.getDateTime(0));
		sb.append(SPMK_LF);
		sb.append(SPHD_VER);
		sb.append(VersionInfo.getVersionNumber());
		sb.append(SPMK_LF);
		sb.append(SPMK_LF);

		Field[] fd = root.getClass().getDeclaredFields();
		for ( Field fx : fd ) {
			fx.setAccessible(true);
			if ( Modifier.isFinal(fx.getModifiers()) ) {
				continue;
			}
			if ( Modifier.isStatic(fx.getModifiers()) ) {
				continue;
			}

			try {

				String key = fx.getName();
				Class cls = fx.getType();
				Object obj = fx.get(root);

				if ( fx.getAnnotation(Deprecated.class) != null ) {
					sb.append(SPHD_DEP);
					sb.append(key);
					sb.append(CMNT_AS);
					sb.append(cls.getName());
					sb.append(SPMK_LF);
					continue;
				}

				int n = -1;
				ArrayList objlst = null;
				Class ocls = null;
				if ( cls == ArrayList.class ) {
					objlst = (ArrayList) obj;

					// nullの要素がある可能性がある
					if ( objlst.size() > 0 ) {
						ParameterizedType paramType = (ParameterizedType) fx.getGenericType();
						ocls = (Class) paramType.getActualTypeArguments()[0];
					}

					n = 1;
				}
				else if ( cls == HashMap.class ) {
					objlst = new ArrayList();
					HashMap map = (HashMap) obj;
					for ( Object o : map.entrySet().toArray() ) {
						objlst.add(o);

						// これは必ずEntrySetだね
						if ( ocls == null ) {
							ocls = o.getClass();
						}
					}
					n = 1;
				}
				else {
					objlst = new ArrayList();
					objlst.add(obj);

					// obj==nullの可能性がある
					ocls = cls;
				}

				if ( ! objlst.isEmpty() ) {
					for ( Object o : objlst ) {

						//Class ocls = o.getClass();
						String val = obj2str(o,ocls);

						if ( val != null ) {
							sb.append(key);
							if ( n >= 1 ) {
								// ArrayList or HashMap
								sb.append("[");
								sb.append(String.valueOf(n));
								sb.append("]");
								n++;
							}
							sb.append(SPMK_SEP);
							sb.append(val);
							sb.append(SPMK_LF);
						}
						else {
							sb.append(SPHD_UNS);
							sb.append(key);
							if ( n >= 1 ) {
								sb.append("[");
								sb.append(String.valueOf(n));
								sb.append("]");
								n++;
							}
							sb.append(CMNT_AS);
							sb.append(ocls.getName());
							sb.append(SPMK_LF);
						}
					}
				}
				else {
					sb.append(key);
					sb.append("[0]");
					sb.append(SPMK_SEP);
					sb.append(SPMK_LF);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

    	System.out.println(MSGID+"テキスト形式で保存します: "+envText);
    	if ( ! CommonUtils.write2file(envText, sb.toString()) ) {
        	System.err.println(ERRID+"保存に失敗しました");
        	return false;
    	}

		return true;
	}


	/*******************************************************************************
	 * 取得
	 ******************************************************************************/

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean load(String envText, Object root) {

    	if ( new File(envText).exists() ) {
        	System.out.println(MSGID+"テキスト形式で読み込みます: "+envText);
        	String buf = CommonUtils.read4file(envText, false);
        	if ( buf != null ) {

	    		Field[] fd = root.getClass().getDeclaredFields();

	        	int lineno = 0 ;
	        	for ( String str : buf.split(SPMK_LF) ) {

	        		++lineno;

	        		if ( str.startsWith(SPHD_MOD) ) {
	        			System.out.println(MSGID+str);
	        			continue;
	        		}

	        		if ( str.startsWith(SPMK_CM) || str.matches("^\\s*$") ) {
	        			continue;
	        		}

	        		String[] a = str.split(SPMK_SEP, 2);
	        		if ( a.length != 2 ) {
	        			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
	        			break;
	        		}

	        		Field fx = null;
	        		for ( Field f : fd ) {
        				if ( f.getName().equals(a[0].replaceFirst("\\[\\d+\\]$","")) ) {
        					fx = f;
        					break;
        				}
	        		}
	        		if ( fx == null ) {
	        			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
	        			break;
	        		}

    				fx.setAccessible(true);

					if ( Modifier.isFinal(fx.getModifiers()) ) {
						continue;
					}
					if ( Modifier.isStatic(fx.getModifiers()) ) {
						continue;
					}

					if ( fx.getAnnotation(Deprecated.class) != null ) {
						System.out.println(MSGID+SPHD_DEP+fx.getName());
						break;
					}

					Class cls = fx.getType();

					try {
						if ( cls == ArrayList.class ) {
							ArrayList list = (ArrayList) fx.get(root);
							if ( fx.get(root) == null ) {
								System.out.println(ERRID+"初期化されていないフィールド: "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
								break;
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
								break;
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

								String[] b = a[1].split(Pattern.quote(SPCH_LF),2);
								if ( b.length != 2 ) {
				        			System.err.println(ERRID+"不正な記述： "+envText+" at "+lineno+"行目 "+str);
				        			continue;
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
					}
					catch (UnsupportedOperationException e) {
						System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
					}
					catch (Exception e) {
						// エラー項目はスキップする
						System.out.println(ERRID+e.getMessage()+": "+envText+" at "+lineno+"行目 ("+cls.getName()+") "+str);
						e.printStackTrace();
					}
	        	}
        	}

        	return true;
    	}

		return false;
	}


	/*******************************************************************************
	 * 保存（部品）
	 ******************************************************************************/

	@SuppressWarnings("rawtypes")
	private static String obj2str(Object obj, Class cls) {
		if ( cls == String.class ) {
			return(obj == null ? SPCH_NULL : ((String) obj).replaceAll(SPMK_LF, SPCH_LF));
		}
		else if ( cls == int.class || cls == Integer.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Integer) obj));
		}
		else if ( cls == float.class || cls == Float.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Float) obj));
		}
		else if ( cls == boolean.class || cls == Boolean.class ) {
			return(obj == null ? SPCH_NULL : String.valueOf((Boolean) obj));
		}
		else if ( cls == Rectangle.class ) {
			Rectangle ra = (Rectangle) obj;
			return(obj == null ? SPCH_NULL : String.format("%d,%d,%d,%d",ra.x,ra.y,ra.width,ra.height));
		}
		else if ( cls == Color.class ) {
			return(obj == null ? SPCH_NULL : CommonUtils.color2str((Color) obj));
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
		else if ( obj instanceof Entry ) {
			Entry t = (Entry) obj;
			String k = obj2str(t.getKey(), t.getKey().getClass());
			String v = obj2str(t.getValue(), t.getValue().getClass());
			return(obj == null ? SPCH_NULL : k+SPCH_LF+v);
		}

		return null;
	}


	/*******************************************************************************
	 * 取得（部品）
	 ******************************************************************************/

	@SuppressWarnings("rawtypes")
	private static Object str2obj(String str, Class cls) throws UnsupportedOperationException {
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

		throw new UnsupportedOperationException("未対応の項目");
	}



	/*******************************************************************************
	 * ディープコピーする
	 ******************************************************************************/

	/**
	 * <P>オブジェクトからオブジェクトへフィールドのコピー（ディープコピー）を行います。
	 * <P>新しく作ったインスタンスに入れ替えたいけど、ポインタを変えたくないので中身だけコピーできないか？という時に使います。
	 * <P>fromにあってtoにないフィールドについては無視して最後まで処理を続行します（というか型違いとかも無視します）。
	 * <P>多分遅いので、設定ファイルの読み出しなど使用頻度の少ないところで利用します。
	 * <P>final修飾、static修飾のある変数はコピーされません
	 * @param to : HashMapクラス、ArrayListクラスの場合は、コピー先にインスタンスが存在している必要があります（nullはだめ）
	 * @param from
	 * @version 3.15.4β～
	 */
	public static boolean deepCopy(final Object to, final Object from) {
		try {
			return FieldCopy(to,from,null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static boolean FieldCopy(final Object to, final Object from, final Field fn) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ConcurrentModificationException, InstantiationException  {

		// 継承しているクラスのリストを作成する
		ArrayList<Class> fromCL = new ArrayList<Class>();
		ArrayList<Class> toCL = new ArrayList<Class>();
		for ( Class c=from.getClass(); c!=null && ! Object.class.equals(c); c=c.getSuperclass() ) {
			//if (debug) System.out.println("[DEBUG] FROM "+c.getName());
			fromCL.add(c);
		}
		for ( Class c=to.getClass(); c!=null && ! Object.class.equals(c); c=c.getSuperclass() ) {
			//if (debug) System.out.println("[DEBUG] TO "+c.getName());
			toCL.add(c);
		}

		// 両方の開始位置をそろえる
		while ( fromCL.size() > 0 && ! toCL.contains(fromCL.get(0)) ) {
			if (debuglv2) System.out.println("[DEBUG] removed FROM "+fromCL.get(0).getName());
			fromCL.remove(0);
		}
		while ( toCL.size() > 0 && ! fromCL.contains(toCL.get(0)) ) {
			if (debuglv2) System.out.println("[DEBUG] removed TO "+toCL.get(0).getName());
			toCL.remove(0);
		}

		int i=0;
		for ( Class c : fromCL ) {

			i++;

			String ctype = (i>1)?(" (super class)"):("");
			String cname = c.getName();
			String fname = (fn!=null)?(" name="+fn.getName()):("");

			if ( isHashMap(to,from,c,fn) ) {
				if (debuglv2) System.err.println("[DEBUG] FieldCopy("+cname+") *** TERM *** deep copy"+ctype+fname);
				return true;
			}

			if ( isLeaf(to,from,c,fn) ) {
				if (debuglv2) System.err.println("[DEBUG] FieldCopy("+cname+") *** TERM *** leaf"+ctype+fname);
				return true;
			}

			Field[] fd = c.getDeclaredFields();

			// フィールドなんかねーよ
			if ( fd.length == 0 ) {
				if ( fn == null ) {
					// 継承だけして追加のフィールドがない場合にここに入る
					if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") no field"+ctype+fname);
					continue;
				}

				if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") *** TERM *** no field"+ctype+fname);
				return isCopyable(to,from,c,fn);	// 終端
			}

			// （フィールド）たんけんぼくのまち。チョーさん生きてるよ！
			for ( Field f : fd ) {
				f.setAccessible(true);

				String xcname = f.getType().getName();
				String xfname = " name="+f.getName();

				int mod = f.getModifiers();

				if ( Modifier.isFinal(mod) ) {
					if (debuglv2) System.err.println("[DEBUG] FieldCopy("+xcname+") : FINAL field "+ctype+xfname);
					continue;
				}
				if ( Modifier.isStatic(mod) ) {
					if (debug) System.err.println("[DEBUG] FieldCopy("+xcname+") : STATIC field "+ctype+xfname);
					continue;
				}


				Object o = f.get(from);
				if ( o == null ) {
					if (debug) System.err.println("[DEBUG] FieldCopy("+xcname+") *** TERM *** null value FROM"+ctype+xfname);
					f.set(to, null);
					continue;	// 終端
				}

				Class xc = o.getClass();
				xcname = xc.getName();

				if ( isHashMap(to,o,xc,f) ) {
					if (debuglv2) System.err.println("[DEBUG] FieldCopy("+xcname+") *** TERM *** deep copy"+ctype+xfname);
					continue;
				}

				if ( isLeaf(to,o,xc,f) ) {
					if (debuglv2) System.err.println("[DEBUG] FieldCopy("+xcname+") *** TERM *** leaf"+ctype+xfname);
					continue;
				}

				isCopyable(to,o,xc,f);	// 終端
			}
		}

		return true;
	}

	// HashMapとか、コピーする
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static boolean isHashMap(Object to, Object from, Class c, Field f) throws InstantiationException, IllegalAccessException, ConcurrentModificationException  {

		String cname = c.getName();
		String fname = " name="+((f==null)?("<TOP>"):(f.getName()));

		for ( Class cx : unCloneables ) {
			if ( cx.equals(c) ) {
				if ( HashMap.class.equals(c) || ArrayList.class.equals(c) ) {

					final Object px = (f==null)?(to):(f.get(to));
					if ( px == null ) {
						// コピー先にインスタンスが存在している必要がある
						if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isCloneable() : must have been initialized"+fname);
						return true;
					}

					// 個別（キャスト！）

					if ( HashMap.class.equals(c) ) {
						HashMap<Object, Object> o = (HashMap<Object, Object>) from;
						HashMap<Object, Object> p;
						if ( f != null ) {
							// フィールドならclone()では社ローコピーのせいで同じものを指してるに違いない、新しいインスタンスを作らないといけない
							p = (HashMap<Object, Object>) c.newInstance();
						}
						else {
							// 自身（orスーパークラス）なら別物だろうからキャストで大丈夫だろう
							p = (HashMap<Object, Object>) to;
						}

						p.clear(); // とりあえず消す
						if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isHashMap() : HashMap "+cname+fname+" size="+o.size());
						for ( Entry<Object, Object> entry: o.entrySet() ) {
							if (debuglv2) System.err.println("[DEBUG] FieldCopy("+cname+") / isHashMap() : copy"+fname+" key="+entry.getKey()+" value="+entry.getValue());
							p.put(entry.getKey(), entry.getValue());
						}

						if ( f != null ) {
							f.set(to, p);
						}
					}
					else if ( ArrayList.class.equals(c) ) {
						ArrayList<Object> o = (ArrayList<Object>) from;
						ArrayList<Object> p = null;
						if ( f != null ) {
							p = (ArrayList<Object>) c.newInstance();
						}
						else {
							p = (ArrayList<Object>) to;
						}

						p.clear(); // とりあえず消す
						if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isHashMap() : ArrayList "+cname+fname+" size="+o.size());
						for ( Object entry: o ) {
							if (debuglv2) System.err.println("[DEBUG] FieldCopy("+cname+") / isHashMap() : copy"+fname+" value="+entry);
							p.add(entry);
						}

						if ( f != null ) {
							f.set(to, p);
						}
					}
				}
				else {
					if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isHashMap() : unsupported"+fname);
				}
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private static final Class[] unCloneables = { HashMap.class, AbstractMap.class, ArrayList.class, AbstractList.class };

	// Integerとか、コピーする
	@SuppressWarnings("rawtypes")
	private static boolean isLeaf(Object to, Object from, Class c, Field f) throws IllegalArgumentException, IllegalAccessException {

		String cname = c.getName();
		//String fname = " name="+((f==null)?("<TOP>"):(f.getName()));

		if ( from instanceof String || from instanceof Boolean || from instanceof Number || from instanceof Color || from instanceof Enum || from instanceof Component ) {
			if ( f == null ) {
				if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isLeaf() : <Top> is not allowed");
				return true;
			}

			//if (debuglv2) System.err.println("[DEBUG] FieldCopy("+cname+") / isLeaf() : leaf field"+fname);
			f.set(to, from);
			return true;
		}

		return false;
	}

	//
	@SuppressWarnings("rawtypes")
	private static boolean isCopyable(Object to, Object from, Class c, Field f) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {

		String cname = c.getName();
		String fname = " name="+((f==null)?("<TOP>"):(f.getName()));

		// コピーに失敗して例外の発生したフィールドについては無視します

		if ( c.isPrimitive() ) {
			if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isCopyable() : primitive"+fname);
			f.set(to, from);
		}
		else if ( c.isArray() ) {
			if ( f == null ) {
				// 自分自身だとコピーするしかないが、しない。redim()があればなんとかできたのに！
				if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isCopyable() : not copyable array"+fname);
			}
			else {
				// フィールドなら入れ物は作る。要素はコピー
				Class comp = c.getComponentType();
				if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / isCopyable() : array of "+comp.getName()+fname+" lenth="+Array.getLength(from));
				Object[] o = (Object[]) from;
				Object[] p = (Object[]) Array.newInstance(comp, Array.getLength(from));

				for ( int i=o.length-1; i>=0; i-- ) {
					if (debuglv2) System.err.println("[DEBUG] FieldCopy("+comp.getName()+") / isCopyable() : copy"+fname+" value="+o[i]);
					p[i] = o[i];
				}
			}
		}
		else {
			// そのほかは可能な限りcloneする
			invokeClone(to,from,c,f);
		}

		return true;
	}

	//
	@SuppressWarnings("rawtypes")
	private static boolean invokeClone(Object to, Object from, Class c, Field f) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		final String cname = c.getName();
		final String fname = " name="+((f==null)?("<TOP>"):(f.getName())); // よく考えたらここにf==nullで入ってくることはない

		try {
			@SuppressWarnings("unchecked")
			Method m = c.getMethod("clone");
			f.set(to, m.invoke(from));
			if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / invokeClone() : invoke clone "+cname+fname);
			return true;
		}
		catch (NoSuchMethodException e) {
			// ちょっとこわいのでコピーせずに無視
			if (debug) System.err.println("[DEBUG] FieldCopy("+cname+") / invokeClone() : clone unsupported and ignored "+fname);
		}
		return false;
	}
}
