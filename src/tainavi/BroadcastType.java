package tainavi;

/**
 * CHコード設定で使ってたけど汎用的に使えそうな気がしてきたので分離してみた
 */
public enum BroadcastType {
	
	TERRA	("地上波",	"DFS"),
	BS		("BS",		"BSDT"),
	CS		("CS",		"CSDT"),
	CAPTURE	("キャプチャ",	""),
	NONE	("指定なし",	""),
	;
	
	String name;
	String header;
	
	private BroadcastType(String name, String header) {
		this.name = name;
		this.header = header;
	}
	
	public String getName() { return name; }
	
	public String getHeader() { return header; }
	
	public static BroadcastType get(String name) {
		for ( BroadcastType b : BroadcastType.values() ) {
			if ( b.name.equals(name) ) {
				return b;
			}
		}
		return NONE;
	}

	/**
	 * 各レコーダの設定内容から放送波の種類を判別する
	 * @param selected
	 * @param recChName
	 * @param chCode
	 * @return
	 */
	public static BroadcastType get(String selected, String recChName, String chCode) {
		
		if ( "EpgDataCap_Bon".equals(selected) || "TVTest".equals(selected) ) {
			if ( chCode != null ) {
				String chid = ContentIdEDCB.getChId(chCode);
				if ( chid != null ) {
					if ( chid.startsWith("0004") ) {
						return BS;
					}
					if ( chid.startsWith("0006") || chid.startsWith("0007") ) {
						return CS;
					}
					else {
						return TERRA;
					}
				}
			}
			return NONE;
		}
		else if ( selected.startsWith("VARDIA RD-") || selected.startsWith("REGZA RD-")  || selected.startsWith("REGZA DBR-Z") ) {
			if ( chCode != null ) {
				String[] d = chCode.split(":");
				if ( d.length == 3 ) {
					if ( d[1].equals("4") ) {
						return BS;
					}
					else if ( d[1].equals("6") || d[1].equals("7") ) {
						return CS;
					}
					else if ( ! d[1].matches("^0+$") ) {
						return TERRA;
					}
				}
			}
			return NONE;
		}
		else if ( selected.startsWith("REGZA") && chCode != null && chCode.matches("^[Gg][0-9A-Fa-f]{8}$") ) {
			if ( chCode.matches("^[Gg]0004....$") ) {
				return BS;
			}
			else if ( chCode.matches("^[Gg]000[67]....$") ) {
				return CS;
			}
			else if ( chCode.matches("^[Gg]7.......$") ) {
				return TERRA;
			}
			return NONE;
		}
		else if ( selected.startsWith("DIGA ") ) {
			if ( recChName != null ) {
				if ( recChName.startsWith("地上D ") ) {
					return TERRA;
				}
				else if ( recChName.startsWith("BS ") ) {
					return BS;
				}
				else if ( recChName.startsWith("CS ") || recChName.startsWith("CS2 ")) {
					return CS;
				}
			}
			return NONE;
		}
		else if ( "iEPG2".equals(selected) ) {
			if ( recChName != null ) {
				if ( recChName.startsWith(TERRA.getHeader()) ) {
					return TERRA;
				}
				else if ( recChName.startsWith(BS.getHeader()) ) {
					return BS;
				}
				else if ( recChName.startsWith(CS.getHeader()) ) {
					return CS;
				}
			}
			return NONE;
		}

		return null;
	}

}
