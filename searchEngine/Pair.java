package searchEngine;

import java.lang.String;

@SuppressWarnings("hiding")
public class Pair<String,Integer> {
	
	private String link;
	
	private Integer endQuote;
	
	public Pair(String link, Integer endQuote){
		this.link = link;
		this.endQuote = endQuote;
	}
	
	public String getLink(){
		return this.link;
	}
	
	public Integer getEndQuote(){
		return this.endQuote;
	}
}


