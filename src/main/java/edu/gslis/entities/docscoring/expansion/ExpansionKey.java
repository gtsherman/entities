package edu.gslis.entities.docscoring.expansion;

import edu.gslis.searchhits.SearchHit;
import edu.gslis.utils.Stopper;

public class ExpansionKey {
	
	private SearchHit doc;
	private Stopper stopper;
	private int limit;

	public ExpansionKey(SearchHit doc, Stopper stopper, int limit) {
		this.doc = doc;
		this.stopper = stopper;
		this.limit = limit;
	}
	
	public SearchHit getDoc() {
		return doc;
	}
	
	public Stopper getStopper() {
		return stopper;
	}
	
	public int getLimit() {
		return limit;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExpansionKey) {
			ExpansionKey other = (ExpansionKey) obj;
			return doc.equals(other.getDoc()) &&
					limit == other.getLimit();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return (doc.getDocno() + limit).hashCode();
	}

}
