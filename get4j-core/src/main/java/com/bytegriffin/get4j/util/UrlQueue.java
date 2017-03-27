package com.bytegriffin.get4j.util;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Url队列 <br>
 * 负责爬虫中全部的url处理 <br>
 */
public final class UrlQueue{

  //key: seed name  value: 该seed所涉及的所有已访问过的链接
  private static ConcurrentHashMap<String, ConcurrentQueue<String>> VISITED_LINKS =  new ConcurrentHashMap<String, ConcurrentQueue<String>>();
  //key: seed name  value: 该seed所涉及的所有未访问过的链接
  private static ConcurrentHashMap<String, ConcurrentQueue<String>> UN_VISITED_LINKS =  new ConcurrentHashMap<String, ConcurrentQueue<String>>();
  //key: seed name  value: 该seed所涉及的所有访问失败的所有url
  private static ConcurrentHashMap<String, ConcurrentQueue<String>> FAIL_VISITED_URLS =  new ConcurrentHashMap<String, ConcurrentQueue<String>>();
  //key: seed name  value: 该seed所涉及的所有已访问过的资源文件 css/js等 
  private static ConcurrentHashMap<String, ConcurrentQueue<String>> VISITED_RESOURCES =  new ConcurrentHashMap<String, ConcurrentQueue<String>>();
  //key: seed name  value: 该seed所涉及的所有未访问过的资源文件 css/js等 
  private static ConcurrentHashMap<String, ConcurrentQueue<String>> UN_VISITED_RESOURCES =  new ConcurrentHashMap<String, ConcurrentQueue<String>>();

  /**
   * 追加未访问的links队列<br/>
   * 首先判断新抓取的link是否在已访问的队列中，<br/>
   * 然后判断是否在未抓取的队列中<br/>
   * 如果都不在的话则将其加进未访问的队列中<br/>
   * @param seedName
   * @param url
   * @return
   */
  public static final void addUnVisitedLinks(String seedName, HashSet<String> links){
	  if (links == null || links.size() == 0) {
		  return;
	  }
	  for(String link : links){
		  ConcurrentQueue<String> constr = getVisitedLink(seedName);
		  if(constr == null || !constr.contains(link)){
			  newUnVisitedLink(seedName,link);
		  }
	  }
  }
  
  /**
   * 追加未访问的resources队列<br/>
   * 首先判断新抓取的resource是否在已访问的队列中，<br/>
   * 然后判断是否在未抓取的队列中<br/>
   * 如果都不在的话则将其加进未访问的队列中<br/>
   * @param seedName
   * @param resources
   * @return
   */
  public static final void addUnVisitedResources(String seedName, HashSet<String> resources){
	  for(String resource : resources){
		  ConcurrentQueue<String> constr = getVisitedResource(seedName);
		  if(constr == null || !constr.contains(resource)){
			  newUnVisitedResource(seedName, resource);
		  }
	  }
  }

  /**
   * 追加已访问的links队列<br/>
   * 首先判断新抓取的link是否在已访问的队列中，<br/>
   * 然后判断是否在未抓取的队列中<br/>
   * 如果都不在的话则将其加进未访问的队列中<br/>
   * @param seedName
   * @param links
   * @return
   */
  public static final void addVisitedLinks(String seedName, HashSet<String> links){
	  for(String link : links){
		  ConcurrentQueue<String> constr = getVisitedLink(seedName);
		  if(constr == null || !constr.contains(link)){
			  newVisitedLink(seedName,link);
		  }
	  }
  }
  
  /**
   * 追加已访问的resources队列<br/>
   * 首先判断新抓取的resource是否在已访问的队列中，<br/>
   * 然后判断是否在未抓取的队列中<br/>
   * 如果都不在的话则将其加进未访问的队列中<br/>
   * @param seedName
   * @param resources
   * @return
   */
  public static final void addVisitedResources(String seedName, HashSet<String> resources){
	  for(String resource : resources){
		  ConcurrentQueue<String> constr = getVisitedResource(seedName);
		  if(constr == null || !constr.contains(resource)){
			  newVisitedResource(seedName,resource);
		  }
	  }
  }

  /**
   * 新增未访问的link到队列中
   * @param seedName
   * @param link
   */
  public static final void newUnVisitedLink(String seedName, String link){
    ConcurrentQueue<String> queue = UrlQueue.UN_VISITED_LINKS.get(seedName);
    if(queue == null){
      queue = new ConcurrentQueue<String>();
    }
    queue.add(link);
    UrlQueue.UN_VISITED_LINKS.put(seedName, queue);
  }
  
  /**
   * 新增未访问的resource到队列中
   * @param seedName
   * @param link
   */
  public static final void newUnVisitedResource(String seedName, String resource){
    ConcurrentQueue<String> queue = UrlQueue.UN_VISITED_RESOURCES.get(seedName);
    if(queue == null){
      queue = new ConcurrentQueue<String>();
    }
    queue.add(resource);
    UrlQueue.UN_VISITED_RESOURCES.put(seedName, queue);
  }

  /**
   * 新增已访问的link到队列中
   * @param seedName
   * @param link
   */
  public static final void newVisitedLink(String seedName, String link){
    ConcurrentQueue<String> queue = UrlQueue.VISITED_LINKS.get(seedName);
    if(queue == null){
      queue = new ConcurrentQueue<String>();
    }
    queue.add(link);
    UrlQueue.VISITED_LINKS.put(seedName, queue);
  }
  
  /**
   * 新增已访问的resource到队列中
   * @param seedName
   * @param resource
   */
  public static final void newVisitedResource(String seedName, String resource){
    ConcurrentQueue<String> queue = UrlQueue.VISITED_RESOURCES.get(seedName);
    if(queue == null){
      queue = new ConcurrentQueue<String>();
    }
    queue.add(resource);
    UrlQueue.VISITED_RESOURCES.put(seedName, queue);
  }

  /**
   * 增加已访问失败的url（包括link、资源文件）
   * @param seedName
   * @param url
   */
  public static final void newFailVisitedUrl(String seedName, String failurl){
    ConcurrentQueue<String> queue = UrlQueue.FAIL_VISITED_URLS.get(seedName);
    if(queue == null){
      queue = new ConcurrentQueue<String>();
    }
    queue.add(failurl);
    UrlQueue.FAIL_VISITED_URLS.put(seedName, queue);
  }

  /**
   * 获取未访问link队列
   * @param seedName
   * @return
   */
  public static final ConcurrentQueue<String> getUnVisitedLink(String seedName){
    return UN_VISITED_LINKS.get(seedName);
  }
  
  /**
   * 获取未访问resource队列
   * @param seedName
   * @return
   */
  public static final ConcurrentQueue<String> getUnVisitedResource(String seedName){
    return UN_VISITED_RESOURCES.get(seedName);
  }


  /**
   * 获取已访问link队列
   * @param seedName
   * @return
   */
  public static final ConcurrentQueue<String> getVisitedLink(String seedName){
    return VISITED_LINKS.get(seedName);
  }
  
  /**
   * 获取已访问resource队列
   * @param seedName
   * @return
   */
  public static final ConcurrentQueue<String> getVisitedResource(String seedName){
    return VISITED_RESOURCES.get(seedName);
  }

  /**
   * 获取已访问失败的url队列
   * @param seedName
   * @return
   */
  public static final ConcurrentQueue<String> getFailVisitedUrl(String seedName){
    return FAIL_VISITED_URLS.get(seedName);
  }

}
