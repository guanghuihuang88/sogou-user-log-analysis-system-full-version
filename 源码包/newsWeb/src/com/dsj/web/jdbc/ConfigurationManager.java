package com.dsj.web.jdbc;

import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {
	
	private static Properties prop = new Properties();

	static {
		try {
			InputStream in = ConfigurationManager.class.getClassLoader().getResourceAsStream("my.properties"); 
			
			// 调用Properties的load()方法，给它传入一个文件的InputStream输入流
			// 即可将文件中的符合“key=value”格式的配置项，都加载到Properties对象中
			// 加载过后，此时，Properties对象中就有了配置文件中所有的key-value对了
			// 然后外界其实就可以通过Properties对象获取指定key对应的value
			prop.load(in);  
		} catch (Exception e) {
			e.printStackTrace();  
		}
	}
	
	/**
	 * 获取指定key对应的value
	 * @param key 
	 * @return value
	 */
	public static String getProperty(String key) {
		return prop.getProperty(key);
	}

	public static int getInteger(String jdbcDatasourceSize) {
		return Integer.parseInt(prop.getProperty(jdbcDatasourceSize));
	}

	public static Long getLong(String key){
		String value = getProperty(key);
		try {
			return Long.valueOf(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0L;
		
	}
}
