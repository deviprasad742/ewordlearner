package eWordLearner.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eWordLearner.model.DefaultWordRepository;


public class EleanerResponseTest {

	public  Map<String, String> createDefinitions() {
		Map<String, String> definitions = new LinkedHashMap<String, String>();
		Pattern titlePattern = Pattern.compile(".*<title>(.*)</title>");
		Pattern definitonPattern = Pattern.compile(".*<!\\[CDATA\\[(.*)\\]\\]>.*");
		StringBuffer urlFeedBuffer = new StringBuffer();
		try {
			URL url = new URL(DefaultWordRepository.BASE_URL);
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				InputStreamReader inStream = new InputStreamReader(connection.getInputStream());
				BufferedReader buff = null;
				try {
					String lastTitle = null;
					buff = new BufferedReader(inStream);
					while (true) {
						String nextLine = buff.readLine();
						if (nextLine != null) {
							urlFeedBuffer.append(nextLine.trim());
							Matcher titleMatcher = titlePattern.matcher(nextLine);
							if (titleMatcher.matches()) {
								lastTitle = titleMatcher.group(1);
							}
							Matcher definitonMatcher = definitonPattern.matcher(nextLine);
							if (definitonMatcher.matches()) {
								String defintion = definitonMatcher.group(1).trim();
								definitions.put(lastTitle, defintion);
							}
						} else {
							break;
						}
					}
				} finally {
					if (buff != null) {
						buff.close();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				connection.disconnect();
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return definitions;
	}
	
	private static long free() {
	    return Runtime.getRuntime().freeMemory();
	}
	
	public static void main(String[] args) {
		long free1 = free();
		EleanerResponseTest responseTest = new EleanerResponseTest();
		Map<String, String> definitions = responseTest.createDefinitions();
		System.out.println(definitions);
		
		
		
		/*WordRepository repository = new WordRepository();
		File imageFile = new File(repository.getImagesDirectory(), "test.jpg");
		imageFile.delete();
		writeFile("http://www.phocabulary.com/words/libation.jpg", imageFile);
		System.out.println(imageFile);*/
		
		//--------------------------------------------------------------------------------//
		/*System.out.println(urlFeedBuffer);
		long free2 = free();
        String feed = urlFeedBuffer.toString();
		long free3 = free();

		if (free2 == free1)
			System.err.println("You need to use -XX:-UseTLAB");
		System.out.println("Fetching took " + (free1 - free2)+ " bytes");
		System.out.println("String took " + (free2 - free3)+ " bytes");*/
	}
	
	private static boolean writeFile(String urlStr, File file) {
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
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			connection.disconnect();
		}
		return true;
	}
	
}
