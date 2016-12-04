package com.dutycode.learning.consistenthash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

public class ConsistentHashDemo {

	// 虚拟节点信息
	private static TreeMap<Integer, VisualCacheNode> visualNodeMap = new TreeMap<Integer, VisualCacheNode>();
	// 真实节点列表
	private static List<CacheNode> realNodeList = new ArrayList<CacheNode>();
	// 每个真实节点对应的虚拟节点个数
	private static int SINGLE_VISUAL_NUM = 5;

	// 每个cacheNode上存储的数据，此处用Map做存储。key为Node的基本信息。实际上应该为Memcahe或者redis，此处用Map代替
	private static Map<String, CacheNode> dataCache = new HashMap<String, CacheNode>();

	// 记录每台Cache节点上存储的key值信息。真实机器上这部分就不需要在这里存储了，这里仅用作后面做数据迁移的时候展示用。
	//key为虚拟节点的名称，value为当前节点上存储的数据信息
	private static Map<String, List<String>> vNodeCacheDataMap = new HashMap<String, List<String>>();

	public static void main(String[] args) {
		// 初始化真实节点信息
		initRealNode();

		// 初始化虚拟节点
		initVisualNode();
		
		// 测试数据，看数据分布到什么虚拟节点上。
		initRadomData2VisualNode();
		
		//新增缓存节点：
		addCacheNode();
		
		
		//删除缓存节点
		removeCacheNode("10.0.0.2");
	}

	/**
	 * 初始化真实节点
	 * 
	 * @Title: initRealNode
	 * @author: zzh
	 * @date: 2016年12月1日 下午7:38:46
	 * @version: v1.0.0
	 *
	 */
	private static void initRealNode() {
		realNodeList.add(new CacheNode("10.0.0.1", "RealNode1"));
		realNodeList.add(new CacheNode("10.0.0.2", "RealNode2"));
		realNodeList.add(new CacheNode("10.0.0.3", "RealNode3"));
		realNodeList.add(new CacheNode("10.0.0.4", "RealNode4"));
	}

	/**
	 * 初始化虚拟节点信息，将虚拟节点散列到2^32-1的环上
	 * 
	 * @Title: initVisualNode
	 * @author: zzh
	 * @date: 2016年12月1日 下午7:48:30
	 * @version: v1.0.0
	 *
	 */
	private static void initVisualNode() {

		for (CacheNode cacheNode : realNodeList) {

			//添加虚拟节点信息到Hash环中
			addVisualCacheNodeIntoCircle(genVisualCacheNode(cacheNode));

		}

		System.out.println("最小值：" + visualNodeMap.firstKey());
		System.out.println("最大值：" + visualNodeMap.lastKey());

	}

	/**
	 * 根据key值获得落到的虚拟节点信息
	 * 
	 * @Title: getVisualCacheNode
	 * @param key
	 * @author: zzh
	 * @date: 2016年12月3日 下午7:58:55
	 * @version: v1.0.0
	 *
	 */
	private static VisualCacheNode getVisualCacheNode(String dataKey) {
		// 确认dataKey的hash值。
		int dataKeyHash = DJBHash(dataKey);
		// 查找距离当前key值最近的一个虚拟节点
		SortedMap<Integer, VisualCacheNode> vnodes = visualNodeMap.tailMap(dataKeyHash);
		// 最近的一个节点：
		VisualCacheNode nearestCacheNode = null;
		if (vnodes == null || vnodes.size() == 0) {
			// 找最小的一个数据
			nearestCacheNode = visualNodeMap.get(visualNodeMap.firstKey());
			System.out.println(dataKey + " 未找到虚拟节点，当前hashkey=" + dataKeyHash + ",使用最小节点存储："
					+ nearestCacheNode.getNodeName());
		} else {
			nearestCacheNode = vnodes.get(vnodes.firstKey());
		}
		return nearestCacheNode;
	}

	/**
	 * 随机初始化一部分数据分散到虚拟节点上
	 * 
	 * @Title: initRadomData2VisualNode
	 * @author: zzh
	 * @date: 2016年12月1日 下午7:59:23
	 * @version: v1.0.0
	 *
	 */
	private static void initRadomData2VisualNode() {
		for (int i = 0; i < 100; i++) {
			String dataKey = DigestUtils.md5Hex(new Random(System.nanoTime()).nextInt() + "");

			VisualCacheNode nearestCacheNode = getVisualCacheNode(dataKey);
			// 将key值和真实缓存节点的关联关系存储起来
			dataCache.put(dataKey, nearestCacheNode.getRealCacheNode());

			// 补充当前节点上存储的key值信息
			List<String> keyList = vNodeCacheDataMap.get(nearestCacheNode.getNodeName());
			if (keyList == null) {
				keyList = new ArrayList<String>();
			}
			keyList.add(dataKey);
			vNodeCacheDataMap.put(nearestCacheNode.getNodeName(), keyList);

			System.out.println("当前测试数据key=" + dataKey  + ",使用虚拟节点："
					+ nearestCacheNode.getNodeName() +",真实Cache节点：" + nearestCacheNode.getRealCacheNode().getNodeName());
		}

	}

	/**
	 * 增加一个新节点
	 * 
	 * @Title: addCacheNode
	 * @author: zzh
	 * @date: 2016年12月2日 下午8:56:38
	 * @version: v1.0.0
	 *
	 */
	private static void addCacheNode() {

		// 添加到真实Node列表
		CacheNode newNode = new CacheNode("10.9.1.100");
		realNodeList.add(newNode);
		// 生成虚拟节点
		List<VisualCacheNode> addVisualNodeList = genVisualCacheNode(newNode);
		
		//查询哪些节点受到影响，即数据需要分散
		for (VisualCacheNode vnode : addVisualNodeList){
			String nodeName= vnode.getNodeName();
			String md5VisualNodeKey = DigestUtils.md5Hex(nodeName);// 使用common-codec工具包
			int hashNodeName = DJBHash(md5VisualNodeKey);
			SortedMap<Integer, VisualCacheNode> vs = visualNodeMap.tailMap(hashNodeName + 1 );//排除掉自己。
			VisualCacheNode nodeInfo = null;
			if (vs != null && vs.size() > 0){
				int affectVnshash = vs.firstKey();
				nodeInfo = visualNodeMap.get(affectVnshash);
			}else {
				nodeInfo = visualNodeMap.get(visualNodeMap.firstKey());
			}
			System.out.println("新添节点，受到影响的虚拟节点：" + nodeInfo);
		}
		
		//将虚拟节点加入到hash环中
		addVisualCacheNodeIntoCircle(addVisualNodeList);
	}
	
	
	
	/**
	 * 删除节点。
	 * @Title: removeCacheNode   
	 * @author: zzh    
	 * @date: 2016年12月4日 下午8:34:15       
	 * @version: v1.0.0
	 *
	 */
	private static void removeCacheNode(String nodeIp){
		
		//删除当前IP的虚拟节点信息
		CacheNode cacheNode = new CacheNode(nodeIp);
		//获取当前节点的虚拟节点信息
		List<VisualCacheNode> vNodesList = genVisualCacheNode(cacheNode);
		//从hash环中删除虚拟节点信息 
		for (VisualCacheNode c : vNodesList){
			String vNodeName = c.getNodeName();
			String md5VisualNodeKey = DigestUtils.md5Hex(vNodeName);// 使用common-codec工具包
			int hashNodeName = DJBHash(md5VisualNodeKey);
			
			visualNodeMap.remove(hashNodeName);
			
			System.out.println("删除虚拟节点，虚拟节点名称：" + vNodeName +",真实节点：" + c.getRealCacheNode());
			
			
			//删除后，受到影响的节点：即数据和流量需要迁移到的节点信息
			SortedMap<Integer, VisualCacheNode> vs = visualNodeMap.tailMap(hashNodeName );
			VisualCacheNode nodeInfo = null;
			if (vs != null && vs.size() > 0){
				int affectVnshash = vs.firstKey();
				nodeInfo = visualNodeMap.get(affectVnshash);
			}else {
				nodeInfo = visualNodeMap.get(visualNodeMap.firstKey());
			}
			System.out.println("删除当前虚拟节点" + vNodeName + " ， 受到影响的虚拟节点（原本虚拟节点的数据和流量将迁移到此节点）：" + nodeInfo);
		}
		
		//踢出真实节点
		removeRealCache(nodeIp);
		
		
	}

	/**
	 * 生成虚拟节点信息
	 * 
	 * @Title: genVisualCacheNode
	 * @param cacheNode
	 * @author: zzh
	 * @date: 2016年12月2日 下午8:59:35
	 * @version: v1.0.0
	 *
	 */
	private static List<VisualCacheNode> genVisualCacheNode(CacheNode cacheNode) {
		List<VisualCacheNode> visualCacheNodeList = new ArrayList<VisualCacheNode>();
		for (int i = 0; i < SINGLE_VISUAL_NUM; i++) {
			String visualNodeKey = "VNode-" + cacheNode.getIp() + "-Num-" + i;

			// 对虚拟节点的key值做下md5
			String md5VisualNodeKey = DigestUtils.md5Hex(visualNodeKey);// 使用common-codec工具包
			int hashKey = DJBHash(md5VisualNodeKey);

			VisualCacheNode visualCacheNode = new VisualCacheNode(visualNodeKey, cacheNode);

			visualCacheNodeList.add(visualCacheNode);
			System.out.println("虚拟节点信息：" + visualNodeKey + ", hashkey=" + hashKey + " , RealIp=" + cacheNode.getIp());
		}
		
		return visualCacheNodeList;
	}
	
	
	/**
	 * 添加虚拟缓存节点加入到hash缓存环中
	 * @Title: addVisualCacheNodeIntoCircle   
	 * @param vNodesList
	 * @author: zzh    
	 * @date: 2016年12月4日 下午8:31:10       
	 * @version: v1.0.0
	 *
	 */
	private static void addVisualCacheNodeIntoCircle(List<VisualCacheNode> vNodesList){
		for (VisualCacheNode vNode: vNodesList){
			// 对虚拟节点的key值做下md5
			String md5VisualNodeKey = DigestUtils.md5Hex(vNode.getNodeName());// 使用common-codec工具包
			int hashKey = DJBHash(md5VisualNodeKey);

			// 将数据存储到虚拟节点数据结构中
			visualNodeMap.put(hashKey, vNode);
		}
	}

	/**
	 * 根据IP，删除真实节点
	 * @Title: removeRealCache   
	 * @param ip
	 * @author: zzh    
	 * @date: 2016年12月4日 下午9:20:24       
	 * @version: v1.0.0
	 *
	 */
	private static void removeRealCache(String ip){
		
		int removeCachePos = 0;
		for (int i = 0; i < realNodeList.size() ; i++){
			CacheNode c = realNodeList.get(i);
			if (ip.equals(c.getIp())){
				removeCachePos = i;
				break;
			}
		}
		realNodeList.remove(removeCachePos);
	}
	
	public static int DJBHash(String str) {
		int hash = 5381;
		for (int i = 0; i < str.length(); i++) {
			hash = ((hash << 5) + hash) + str.charAt(i);
		}
		return hash;
	}

}

/**
 * 缓存机器节点信息
 * 
 * @Title：CacheNode
 * @author: zzh
 * @date: 2016年12月1日 下午7:35:35
 * @version
 *
 */
class CacheNode {
	private String ip;
	private String pwd;
	private int port;
	private String nodeName;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public CacheNode(String ip, String pwd, int port) {
		this.ip = ip;
		this.port = port;
		this.pwd = pwd;
	}

	public CacheNode(String ip, String pwd, int port, String nodeName) {
		super();
		this.ip = ip;
		this.pwd = pwd;
		this.port = port;
		this.nodeName = nodeName;
	}

	public CacheNode(String ip, String nodeName) {
		this(ip, "pwd", 6379, nodeName);
	}

	public CacheNode(String ip) {
		this(ip, "pwd", 6379);// 默认密码pwd，端口6379
	}

	@Override
	public String toString() {
		return "CacheNode [ip=" + ip + ", pwd=" + pwd + ", port=" + port + ", nodeName=" + nodeName + "]";
	}

}

class VisualCacheNode {
	private String nodeName;
	private CacheNode realCacheNode;

	public VisualCacheNode(String nodeName, CacheNode readCacheNode) {
		super();
		this.nodeName = nodeName;
		this.realCacheNode = readCacheNode;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public CacheNode getRealCacheNode() {
		return realCacheNode;
	}

	public void setRealCacheNode(CacheNode readCacheNode) {
		this.realCacheNode = readCacheNode;
	}

	@Override
	public String toString() {
		return "VisualCacheNode [nodeName=" + nodeName + ", readCacheNode=" + realCacheNode + "]";
	}

}
