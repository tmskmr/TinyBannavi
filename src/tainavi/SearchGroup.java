package tainavi;

import java.util.ArrayList;
import java.util.Iterator;

public class SearchGroup implements Iterator<String>,Iterable<String> {
	
	private String name;
	private ArrayList<String> members = new ArrayList<String>();
	
	private int idx;
	
	public void setName(String s) { name = s; }
	public String getName() { return name; }
	public void setMembers(ArrayList<String> a) { members = a; }
	public ArrayList<String> getMembers() { return members; }
	
	public void add(String s) { members.add(s); }
	
	public boolean replace(String oldMember, String newMember) {
		for ( int i=0; i<members.size(); i++ ) {
			if ( members.get(i).equals(oldMember) ) {
				 members.set(i, newMember);
				 return true;
			}
		}
		return false;
	}
	
	@Override
	public Iterator<String> iterator() {
		idx = -1;
		return this;
	}
	@Override
	public boolean hasNext() {
		return (members.size() > (idx+1));
	}
	@Override
	public String next() {
		return members.get(++idx);
	}
	@Override
	public void remove() {
		members.remove(idx);
	}
}
