package org.su18.ysuserial.payloads.config;


import java.util.HashMap;

/**
 * @author su18
 */
public class Config {


	// 是否使用混淆技术
	public static Boolean IS_OBSCURE = false;


	// 恶意类是否继承 AbstractTranslet
	public static Boolean IS_INHERIT_ABSTRACT_TRANSLET = false;


	// 是否在序列化数据流中的 TC_RESET 中填充脏数据
	public static Boolean IS_DIRTY_IN_TC_RESET = false;


	// jboss
	public static Boolean IS_JBOSS_OBJECT_INPUT_STREAM = false;

	// 填充的脏数据长度
	public static int DIRTY_LENGTH_IN_TC_RESET = 0;


	// 各种方式的内存马映射的路径
	public static String URL_PATTERN = "/su18";


	// 不同类型内存马的父类/接口与其关键参数的映射
	public static HashMap<String, String> KEY_METHOD_MAP = new HashMap<>();


	public static void init() {
		// Servlet 型内存马，关键方法 service
		KEY_METHOD_MAP.put("javax.servlet.Servlet", "service");
		// Filter 型内存马，关键方法 doFilter
		KEY_METHOD_MAP.put("javax.servlet.Filter", "doFilter");
		// Listener 型内存马，通常使用 ServletRequestListener， 关键方法 requestInitializedHandle
		KEY_METHOD_MAP.put("javax.servlet.ServletRequestListener", "requestInitializedHandle");
		// Websocket 型内存马，关键方法 onMessage
		KEY_METHOD_MAP.put("javax.websocket.MessageHandler$Whole", "onMessage");
		// Tomcat Upgrade 型内存马，关键方法 accept
		KEY_METHOD_MAP.put("org.apache.coyote.UpgradeProtocol", "accept");
		// Tomcat Executor 型内存马，关键方法 execute
		KEY_METHOD_MAP.put("org.apache.tomcat.util.threads.ThreadPoolExecutor", "execute");
	}


}
