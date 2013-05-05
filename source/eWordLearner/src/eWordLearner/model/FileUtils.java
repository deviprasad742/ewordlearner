package eWordLearner.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class FileUtils {

	public static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	public static void writeFile(String urlStr, File file) throws IOException {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(urlStr);
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			InputStream in = connection.getInputStream();
			FileOutputStream out = new FileOutputStream(file);
			try {
				byte[] chunk = new byte[1024];
				int readCount;
				while ((readCount = in.read(chunk)) != -1) {
					out.write(chunk, 0, readCount);
				}
			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			}
		}  finally {
			connection.disconnect();
		}
	}
	
	public static void writeListToFile(List<String> list, File file) throws Exception {
		List<String> unSyncList;
		synchronized (list) {
		  unSyncList = new ArrayList<String>(list);	
		}
		FileWriter writer = new FileWriter(file);
		for (String string : unSyncList) {
			writer.write(string);
			writer.write(LINE_SEPARATOR);
		}
		if (writer != null) {
			writer.close();
		}
	}
	
	public static List<String> readListFromFile(File file) throws Exception {
		List<String> list = new ArrayList<String>();
		if (file.exists()) {
			BufferedReader reader = new BufferedReader(new  FileReader(file));
			String nextLine = null;
			while ((nextLine = reader.readLine()) != null ) {
				list.add(nextLine);
			}
		}
		return list;
	}
	
	public static void writeMapToFile(Map<String, String> map, File file) throws Exception {
		Properties properties = new Properties();
		synchronized (map) {
			properties.putAll(map);
		}
		FileWriter writer = new FileWriter(file);
		properties.store(writer, null);
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}
	
	public static Map<String, String> readMapFromFile(File file) throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		if (file.exists()) {
			Properties properties = new Properties();
			FileReader reader = new FileReader(file);
			properties.load(reader);
			for (Entry<Object, Object> property : properties.entrySet()) {
				map.put(String.valueOf(property.getKey()), String.valueOf(property.getValue()));
			}
		}
		return map;
	}
	
}
