package searchEngine;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

public class SearchEngine {
	
	private static final Logger log = Logger.getLogger(SearchEngine.class.getName());
	
	private static HashMap<String,LinkedList<String>> searchIndex = new HashMap<String, LinkedList<String>>();
	
	private static HashMap<String,LinkedList<String>> linkGraph = new HashMap<String, LinkedList<String>>();
	
	private static LinkedList<String> willCrawl = new LinkedList<String>();
	
	private static LinkedList<String> hasCrwaled = new LinkedList<String>();
	
	private static HashMap<String, Double> pageRank = new HashMap<String, Double>();


	public static void main(String[] args) {
		webCrawler("http://testsearchhtml.appspot.com/dp.html");
		log.info("index -----------------------------------------------------");
		for(String word:searchIndex.keySet()){
			String key = word.toString();
			for(String url:searchIndex.get(word)){
				System.out.println(key + ": " + url);
			}
		}
		log.info("link graph-----------------------------------------------------");
		for(String link: linkGraph.keySet()){
			for(String outLink: linkGraph.get(link)){
				System.out.println(link + ": " + outLink);
			}
		}
		log.info("-----------------------------------------------------");
		pageRankAlgorithm(linkGraph);
		for(String word:pageRank.keySet()){
			String key = word.toString();
			System.out.println(key + ": " + pageRank.get(key));
		}
		log.info("sorted rank -----------------------------------------------------");
		LinkedHashMap<String,LinkedList<String>> sortMAP = sortByComparator(pageRank);
		for(String word:sortMAP.keySet()){
			String key = word.toString();
			System.out.println(key + ": " + pageRank.get(key));
		}
		log.info("final reuslt -----------------------------------------------------");
		LinkedList<String> rankedURL = luoruixiangSearch(searchIndex, pageRank, "dynamic programming");
		for(String word:rankedURL){
			String key = word.toString();
			System.out.println(key + ": " + pageRank.get(key));
		}
	}
	
	/**
	 * @param args
	 */
	
	public static String convertPagetoString(String weburl) {
		String page = "";
		try {
			URL url = new URL(weburl);
			URLConnection urlConnection;
			try {
				urlConnection = url.openConnection();
				InputStream in = urlConnection.getInputStream();
				String encoding = urlConnection.getContentEncoding();
				encoding = encoding == null ? "UTF-8" : encoding;
				page = IOUtils.toString(in, encoding);
			} catch (IOException e) {
				log.info("The site cannot be crawled!");
				e.printStackTrace();
			}
		} catch (MalformedURLException e1) {
			log.info("The url is bad!");
			e1.printStackTrace();
		}
		if(page != "" && page.length() > 0){
			page = page.substring(page.indexOf("<body>"), page.indexOf("</body>"));
			page = page.toLowerCase();
		}
		return page;
	}
	
	public static LinkedList<String> convertPageToWord(String keyword){
		LinkedList<String> keywordList = new LinkedList<String>();
		keyword = keyword.toLowerCase();
		String[] keywords = keyword.split(" |\n");
		for(String key : keywords){
			keywordList.addLast(key);
		}
		// add the anchor text
		LinkedList<String> allWholeLinks = new LinkedList<String>();
		allWholeLinks = retrieveAllLink(keyword,2);
		for(String wholeLink: allWholeLinks){
			String anchorText = wholeLink.substring(wholeLink.indexOf('>') + 1, wholeLink.indexOf('<'));
			String[] anchorTextWord = anchorText.split(" ");
			for(String textWord: anchorTextWord){
				if(!keywordList.contains(textWord)){
					keywordList.addLast(textWord);
				}
			}
		}
		return keywordList;
	}

	public static Pair get_next_whole_link(String page){
		int startIndex = -1;
		int endIndex = -1;
		Pair returnPair = new Pair("", new Integer(-1));
		startIndex = page.indexOf("<a href=");
		if(startIndex == -1){
			returnPair = new Pair("", new Integer(0));
		}
		else{
			endIndex = page.indexOf("a>", startIndex + 1);
			returnPair = new Pair(page.substring(startIndex + 1, endIndex),new Integer(endIndex));
		}
		return returnPair;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Pair get_next_link(String page){
		int startIndex = -1;
		int endIndex = -1;
		Pair returnPair = new Pair("", new Integer(-1));
		int lookIndex = page.indexOf("<a href=");
		if(lookIndex == -1){
			returnPair = new Pair("", new Integer(0));
		}
		else{
			startIndex = page.indexOf('"', lookIndex);
			endIndex = page.indexOf('"', startIndex + 1);
			returnPair = new Pair(page.substring(startIndex + 1, endIndex),new Integer(endIndex));
		}
		return returnPair;
	}
	
	public static LinkedList<String> retrieveAllLink(String page, int pattern){
		LinkedList<String> allLinks= new LinkedList<String>();
		while(true){
			Pair pair;
			if(pattern == 1){
				pair = get_next_link(page);
			}
			else{
				pair = get_next_whole_link(page);
			}
			if(pair.getLink() != ""){
				allLinks.addLast((String) pair.getLink());
				int a = (Integer)pair.getEndQuote();
				page = page.substring(a);
			}
			else
				break;
		}
		return allLinks;
	}
	
	public static void addPageToIndex(HashMap<String,LinkedList<String>> engineIndex,String pageContent, String pageUrl){
		
		LinkedList<String> urlWords = convertPageToWord(pageContent);
		// Delete the unnecessary words, use iterator instead of foreach, otherwise, ConcurrentModificationException.
		Iterator<String> it = urlWords.iterator();
		while(it.hasNext()){
			String value = it.next().toString();
			if(value.contains(">") || value.contains("<") || value.length() > 20 || value.length() == 0){
				it.remove();
			}
		}
		for(String word:urlWords){
			word = word.replaceAll("\n", "");
			if(engineIndex.containsKey(word)){
				LinkedList<String> temp1 = new LinkedList<String>();
				temp1 = engineIndex.get(word);
				if(!temp1.contains(pageUrl)){
					temp1.addLast(pageUrl);
					engineIndex.put(word,temp1);
				}
			}
			else{
				LinkedList<String> temp = new LinkedList<String>();
				temp.addLast(pageUrl);
				engineIndex.put(word,temp);
			}
		}
	}
	
	public static void webCrawler(String seed){
		willCrawl.addLast(seed);
		while(willCrawl.size() > 0){
			String pageUrl = willCrawl.pop();
			if(!hasCrwaled.contains(pageUrl)){
				String words = convertPagetoString(pageUrl);
				// whether the page has content
				if(words.length() > 0){
					LinkedList<String> outLinks = retrieveAllLink(words, 1);
					addPageToIndex(searchIndex, words, pageUrl);
					linkGraph.put(pageUrl, outLinks);
					for(String link: outLinks){
						if(!willCrawl.contains(link)){
							willCrawl.addLast(link);
						}
					}
					hasCrwaled.addLast(pageUrl);
				}
			}
		}
	}
	
	public static void pageRankAlgorithm(HashMap<String, LinkedList<String>> webgraph){
		double d = 0.85;
		double numPages = webgraph.size();
		// make the initial value for pageRank of every page
		for(String pageUrl: webgraph.keySet()){
			pageRank.put(pageUrl, 1.0/numPages);
		}
		// iterate until converging
		double difference = 1;
		while(difference != 0){
		//for(int i=0;i<10;i++){	
			HashMap<String, Double> newRank = new HashMap<String, Double>();
			for(String pageUrl: webgraph.keySet()){
				double rankPart = (1 - d)/numPages;
				for(String otherPageUrl: webgraph.keySet()){
					for(String outLink: webgraph.get(otherPageUrl)){
						if(outLink.equals(pageUrl)){
							rankPart = rankPart + d*pageRank.get(otherPageUrl)/webgraph.get(otherPageUrl).size();
						}
					}
				}
				newRank.put(pageUrl, rankPart);
			}
			difference = 0;
			for(String pageUrl: newRank.keySet()){
				difference = difference + Math.abs(newRank.get(pageUrl) - pageRank.get(pageUrl));
			}
			pageRank = newRank;
		}
	}
	
	public static LinkedList<String> luoruixiangSearch(HashMap<String,LinkedList<String>> webIndex,HashMap<String, Double> ranks, String searchString){
		LinkedList<String> keywords = new LinkedList<String>();
		String[] temp = searchString.split(" ");
		for(String i: temp){
			keywords.addLast(i);
		}
		// the result URLs
		LinkedList<String> RankedURL = new LinkedList<String>();
		LinkedList<String> FinalResult = new LinkedList<String>();
		HashMap<String, Double> FinalSortedPageRank = new HashMap();
		// iterate the keywords
		Iterator<String> it = keywords.iterator();
		// get the sorted ranks of the whole webgraph
		Map<String, LinkedList<String>> sortedPageRank = sortByComparator(ranks);
		List<String> LowTOHighUrlList = new ArrayList<String>(sortedPageRank.keySet());
		// for every keyword, get the list of urls, add it to the last of the list
		LinkedList<String> listForOneWord = new LinkedList<String>();
		while(it.hasNext()){
			String keyword = it.next().toString();
			if(searchIndex.containsKey(keyword)){
				LinkedList<String> waitToRank = searchIndex.get(keyword);
				for (int k = LowTOHighUrlList.size()-1; k >= 0; k--){
					if(listForOneWord.size() == 3)
						break;
					else{
						String url = LowTOHighUrlList.get(k);
						if(waitToRank.contains(url)){
							listForOneWord.addLast(url);
						}
					}
				}
			}
			for(String st: listForOneWord){
				if(!RankedURL.contains(st)){
					RankedURL.addLast(st);
				}
			}
			listForOneWord.clear();
		}
		HashMap<String, Double> tempMap = new HashMap();
		for(String url: RankedURL){
			tempMap.put(url, ranks.get(url));
		}
		FinalSortedPageRank = sortByComparator(tempMap);
		for(String url1: FinalSortedPageRank.keySet()){
			FinalResult.addFirst(url1);
		}
		return FinalResult;		
	}
	
	
	
	public static LinkedHashMap sortByComparator(Map unsortMap){
		// sort the new list based on comparator by value
		LinkedList RankedURLList = new LinkedList(unsortMap.entrySet());
		Collections.sort(RankedURLList, new Comparator(){
			public int compare(Object o1, Object o2){
				return ((Comparable)((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		// create the new sorted pageRank map based on the sorted list
		LinkedHashMap sortedPageRank = new LinkedHashMap();
		for(Iterator itt = RankedURLList.iterator();itt.hasNext();){
			Map.Entry entry = (Map.Entry) itt.next();
			sortedPageRank.put(entry.getKey(), entry.getValue());
		}
		return sortedPageRank;
	}
}

