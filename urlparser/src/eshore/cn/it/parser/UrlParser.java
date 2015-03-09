package eshore.cn.it.parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * class<code>UrlParser</code>
 * 此类通过下载指定页面抽取锚链接以及锚文本信息
 * 通过特定算法识别锚文本信息对应的关键英文单词
 * 为以后实现通过url链接识别网页类别积累语料库
 * 结果目前保存为 data目录下 keywords.csv文件
 * 其中特色为关键词识别算法，详情况请看： parseTextKeyWords
 * 整个类的设计采用mvc三层架构，具有一定的通用性，对按照真是意义编码的url网站具有普遍适应性
 * 可以将爬取层单独成组件 、可以将解析层单独作为组件、可以将持久层单独成组件
 * @author   clebeg
 * @version	 0.0.1
 * @see      java.lang.Class
 * @since    JDK1.8
 * */
public class UrlParser {
	//指定记录锚文本以及链接，为关键字提取做准备
	private Map<String, Set<String>> textLinks;
	
	//指定记录锚文本提取的关键字
	private Map<String, Set<String>> textKeyWords;
	
	//数据保存目录为
	private String dataDir = "data";
	
	public static void main(String[] args) {
		UrlParser up = new UrlParser();
		String url = "http://news.sina.com.cn/guide/"; //指定特定页面，可以作为命令行参数
		String cssPath = "div#tab01 a";						   //具体css path, 控制解析范围	
		up.parseExecute(url, cssPath);
	}
	
	public void parseExecute(String url, String cssPath) {
		try {
			parseTextLinks(url, cssPath);		//第一步：获取想要的锚文本链接信息
			parseTextKeyWords(url);				//第二步：获取想要的锚文本及关键词
			writeCSVTextKeyWords();				//第三步：持久化锚文本及关键词
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}
	
	/**
	 * method<code>parseTextLinks</code>
	 * 该方法通过利用Jsoup下载网页提取指定cssPath包围的部分链接
	 * 按照锚文本作为关键字，保存到map中
	 * @param url 指定需要解析的url
	 * @param cssPath 指定需要解析的页面范围
	 * @throws IOException 
	 */
	private void parseTextLinks(String url, String cssPath) throws IOException {
		//注意这里使用 HashMap 是线程不安全的，以后扩展为多线程是要特别注意
		this.textLinks = new HashMap<String, Set<String>>();
		
		//解析成 dom 树
		Document doc = Jsoup.connect(url).get();
		//通过jsoup的特殊选择器选择处所有的链接
        Elements links = doc.select(cssPath);
		
        //保存所有的链接以锚文本作为key
		for(Element link : links) {
			String text = link.text().trim();
			if (text != null && text.equalsIgnoreCase("") == false) {
				Set<String> ls = this.textLinks.get(text);
				if (ls == null) {
					ls = new HashSet<String>();
					ls.add(link.attr("abs:href"));
					this.textLinks.put(text, ls);
				} else 
					ls.add(link.attr("abs:href"));
			}
		}
	}
	
	
	/**
	 * method<code>parseTextKeyWords</code>
	 * 遍历所有的锚文本为key链接为values的map
	 * 按照特定的算法提取链接中的关键词
	 * @param url 原始页面的url
	 */
	private void parseTextKeyWords(String url) {
		this.textKeyWords = new HashMap<String, Set<String>>();
		
		//获取原始url的域名
		String mainDomain = getDomain(url);									
		
		if (mainDomain != null) {											
			Set<String> domainSet = new HashSet<String>();
			Set<Entry<String, Set<String>>> es = this.textLinks.entrySet();
			Iterator<Entry<String, Set<String>>> it = es.iterator();
			//循环所有的链接，提取关键字
			while(it.hasNext()) {											
				Entry<String, Set<String>> textLink = it.next();
				String key = textLink.getKey();
				Set<String> values = textLink.getValue();
				
				Set<String> ls = this.textKeyWords.get(key);
				if (ls == null) {
					//如果还没有为此文本提取过关键字，则提取
					ls = getKeyWords(mainDomain, domainSet, values);
					this.textKeyWords.put(key, ls);
				} else
					ls.addAll(getKeyWords(mainDomain, domainSet, values));
			}
		}
		
	}

	/**
	 * method<code>getKeyWords</code>
	 * 提取链接关键字，通过比较原始链接域名，自身链接特殊关键词
	 * 按照特定的算法提取链接中的关键词
	 * @param mainDomain 原始页面的域名
	 * @param domainSet	   已经收集的域名集合
	 * @param values	   此关键字对应所有的urls
	 */
	private Set<String> getKeyWords(String mainDomain, Set<String> domainSet,
			Set<String> values) {
		
		Set<String> keyWords = new HashSet<String>();
		
		//循环提取每个链接的关键词
		for (String url : values) {
			try {
				URL u = new URL(url);
				
				if (domainSet.add(u.getHost()) == true) {
					//如果域名关键词还没有提取过，那么
					//一个小算法，得到两个域名最长公共子序列
					String tmp = getLongestCommonSubString(u.getHost(), mainDomain);
					
					if (tmp != null)
						keyWords.add(tmp);
				}
				//将在对应路径信息中将所有的关键词分解
				String[] strs = u.getPath().split("/");
				for (String s : strs) {
					s = s.trim();
					if (s.equalsIgnoreCase("") == false && isInteger(s) == false)
						keyWords.add(s);
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return keyWords;
	}

	private String getLongestCommonSubString(String host, String mainDomain) {
		char[] fs = host.toCharArray();
		char[] ms = mainDomain.toCharArray();
		int i, j;
		for (i = host.length() - 1, j = mainDomain.length() - 1;
				i >= 0 && j >= 0; i--,j--) {
			if (fs[i] != ms[j])
				break;
		}
		if (i <= 0)
			return null;
		return host.substring(0, i + 1);
	}

	private String getDomain(String url) {
		URL reurl;
		try {
			reurl = new URL(url);
			return reurl.getHost();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
		
	}

	/**
	 * method<code>writeCSVTextKeyWords</code>
	 * 提取链接关键字信息之后写入csv文件当中
	 * 这个方法可以提取作为持久层组件
	 */
	private void writeCSVTextKeyWords() {
		File dataFile = new File(this.dataDir, "keywords.csv");
		FileWriter writer = null;
		try {
			writer = new FileWriter(dataFile, true);
			Set<Entry<String, Set<String>>> es = this.textKeyWords.entrySet();
			Iterator<Entry<String, Set<String>>> it = es.iterator();
			while(it.hasNext()) {
				Entry<String, Set<String>> textLink = it.next();
				String key = textLink.getKey() + ":";
				Set<String> values = textLink.getValue();
				for (String str : values)
					key += str + ",";
				key = key.substring(0, key.length() - 1);
				key += System.lineSeparator();
				System.out.println(key);
				writer.write(key);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				writer = null;
				e.printStackTrace();
			}
		}
		
		
		
	}

	public Map<String, Set<String>> getTextLinks() {
		return textLinks;
	}

	public Map<String, Set<String>> getTextKeyWords() {
		return textKeyWords;
	}

	/**
	 * method<code>getKeyWords</code>
	 * 判断是否为整数  
	 * @param str 传入的字符串  
	 * @return 是整数返回true,否则返回false  
	 */    
	public boolean isInteger(String str) {    
	    Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");    
	    return pattern.matcher(str).matches();    
	}  
	
}
