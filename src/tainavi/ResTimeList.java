package tainavi;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * <P>録画予約の開始・終了日時と重複度を計算します
 */
public class ResTimeList extends ArrayList<ResTimeItem> {
	/*
	 * ResTimeItemオブジェクト配列の比較関数（開始時刻順に並ぶようにする）
	 *
	 */
	public class ResTimeItemComparator implements Comparator<ResTimeItem> {
		@Override
		public int compare(ResTimeItem p1, ResTimeItem p2) {
			int rc = p1.getRecorder().compareTo(p2.getRecorder());
			if (rc != 0)
				return rc;

			return p1.getStart().compareTo(p2.getStart());
		}
	}

	/*
	 * ResTimeItemオブジェクト配列に予約時間枠をマージしながら追加する
	 */
	public void mergeResTimeItem(String rec, String start, String end, ReserveList rl, HDDRecorder recorder){
		Boolean b = false;

		// 長さ０の時間枠は無視する
		if (start.equals(end))
			return;

		StringBuilder sb = new StringBuilder("");

		sb.append("<TR>");
		sb.append("<TD>" + start.substring(11) + "～" + end.substring(11) + "</TD>");
		if (rl != null){
			sb.append("<TD>" + rl.getCh_name() + "</TD>");
			String color = recorder != null ? recorder.getColor(rl.getTuner()) : "000000";
			sb.append("<TD><STRONG><FONT COLOR=" + color + ">" + rl.getTuner() + "</FONT></STRONG></TD>");
			sb.append("<TD><STRONG><FONT COLOR=BLUE><U>" + rl.getTitle() + "</U></FONT></STRONG></TD>");
		}
		sb.append("</TR>");

		String tooltip = sb.toString();

		// すでにある予約枠と順に比較する
		for (ResTimeItem item2 : this){
			String start2 = item2.getStart();
			String end2 = item2.getEnd();
			String rec2 = item2.getRecorder();
			if (!rec2.equals(rec))
				continue;

			// 比較対象(item2)の予約枠の開始時刻より前に終わる場合はループを抜け、単に追加する
			//  start          end
			//   |--------------|
			//                      |<-- item2 -->|
			if (end.compareTo(start2) <= 0){
				break;
			}
			// item2の終了時刻より後に始まる場合は次の予約枠へ
			//                  start          end
			//                   |--------------|
			//  |<-- item2 -->|
			else if (start.compareTo(end2) >= 0){
				continue;
			}
			// item2の開始時刻より前に始まる場合
			//  start
			//   |---------------?
			//    <- a ->|<-- item2 -->|
			else if (start.compareTo(start2) < 0){
				// item2の開始時刻までの枠(a)を再帰呼出しにより追加する
				mergeResTimeItem(rec, start, start2, rl, recorder);

				// item2の終了時刻より後に終わる場合
				//  start                         end
				//   |-----------------------------|
				//    <- a ->|<-- item2 -->|<- b ->
				if (end.compareTo(end2) >= 0){
					// item2のカウントを増やす
					item2.addCount(tooltip);

					// item2の終了時刻後の枠(b)を再帰呼出しにより追加する
					mergeResTimeItem(rec, end2, end, rl, recorder);
				}
				// item2の終了時刻より前に終わる場合
				//  start          end
				//   |--------------|<- c ->
				//    <- a ->|<--- item2 -->|
				else{
					// item2の終了時間までの枠(c)を追加する
					ResTimeItem itemT = new ResTimeItem(rec, end, end2, item2.getCount(), item2.getTooltip());
					addResTimeItem(itemT);

					// item2の時間を短くし、カウントを増やす
					item2.setEnd(end);
					item2.addCount(tooltip);
				}
			}
			// item2のの開始時刻より後に始まる場合
			//        start
			//  <- d ->|---------------?
			// |<------- item2 -------->|
			else{
				// item2の開始時刻からの枠(d)を追加する
				if (start.compareTo(start2) > 0){
					ResTimeItem itemH = new ResTimeItem(rec, start2, start, item2.getCount(), item2.getTooltip());
					addResTimeItem(itemH);
				}

				// item2の終了時刻より後に終わる場合
				//        start                end
				//  <- d ->|--------------------|
				// |<------- item2 ---->|<- e ->
				if (end.compareTo(end2) >= 0){
					// item2の開始時刻と長さを変更し、カウントを増やす
					item2.setStart(start);
					item2.setEnd(end2);
					item2.addCount(tooltip);

					// item2の終了時刻後の枠(e)を再帰呼出しにより追加する
					mergeResTimeItem(rec, end2, end, rl, recorder);
				}
				// item2の予約枠の終了時間より前に終わる場合
				//        start          end
				//  <- d ->|--------------|<- f ->
				// |<----------- item2 ---------->|
				else{
					// item2の終了時刻前の枠(f)を追加する
					ResTimeItem itemT = new ResTimeItem(rec, end, end2, item2.getCount(), item2.getTooltip());
					addResTimeItem(itemT);

					// item2の開始時刻と長さを変更し、カウントを増やす
					item2.setStart(start);
					item2.setEnd(end);
					item2.addCount(tooltip);
				}
			}

			b = true;
			break;
		}

		// 処理が完了しなかった場合は単に追加する
		if (!b){
			ResTimeItem item = new ResTimeItem(rec, start, end, tooltip);
			addResTimeItem(item);
		}

		// 開始時刻の昇順になるように並び替える
		this.sort(new ResTimeItemComparator());
	}

	/*
	 * 指定した期間の最大予約数を返す
	 */
	public int getMaxResCount(String rec, String start, String end){
		int count = 0;

		// すでにある予約枠と順に比較する
		for (ResTimeItem item2 : this){
			String rec2 = item2.getRecorder();
			if (!rec2.equals(rec))
				continue;

			String start2 = item2.getStart();
			String end2 = item2.getEnd();
			int count2 = item2.getCount();

			if (start.compareTo(end2) < 0 && end.compareTo(start2) > 0){
				if (count2 > count)
					count = count2;
			}
		}

		return count;
	}
	/*
	 * ResTimeItemオブジェクト配列に予約時間枠を追加する
	 */
	private void addResTimeItem(ResTimeItem item){
		if (item == null)
			return;

		this.add(item);

		// 開始時刻の昇順になるように並び替える
		this.sort(new ResTimeItemComparator());
	}
}
