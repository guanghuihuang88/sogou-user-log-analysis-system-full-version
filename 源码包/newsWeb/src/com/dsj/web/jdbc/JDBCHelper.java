package com.dsj.web.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

/**
 * JDBC辅助组件
 * @author Administrator
 * 
 */
public class JDBCHelper {
	static {
		try {
			String driver = ConfigurationManager
					.getProperty(Constants.JDBC_DRIVER);
			Class.forName(driver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 第二步，实现JDBCHelper的单例化
	private static JDBCHelper instance = null;

	/**
	 * 获取单例
	 * 
	 * @return 单例
	 */
	public static JDBCHelper getInstance() {
		if (instance == null) {
			synchronized (JDBCHelper.class) {
				if (instance == null) {
					instance = new JDBCHelper();
				}
			}
		}
		return instance;
	}

	// 数据库连接池
	private LinkedList<Connection> datasource = new LinkedList<Connection>();

	/**
	 * 
	 * 第三步：实现单例的过程中，创建唯�?��数据库连接池
	 * 
	 * 
	 */
	private JDBCHelper() {
		int datasourceSize = ConfigurationManager
				.getInteger(Constants.JDBC_DATASOURCE_SIZE);

		// 然后创建指定数量的数据库连接，并放入数据库连接池
		for (int i = 0; i < datasourceSize; i++) {
			String url = null;
			String user = null;
			String password = null;
			url = ConfigurationManager.getProperty(Constants.JDBC_URL);
			user = ConfigurationManager.getProperty(Constants.JDBC_USER);
			password = ConfigurationManager
					.getProperty(Constants.JDBC_PASSWORD);

			try {
				Connection conn = DriverManager.getConnection(url, user,
						password);
				datasource.push(conn);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 第四步，提供获取数据库连接
	 * 
	 */
	public synchronized Connection getConnection() {
		while (datasource.size() == 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return datasource.poll();
	}

	/**
	 * 执行查询SQL语句
	 * 
	 * @param sql
	 * @param params
	 * @param callback
	 */
	public synchronized void executeQuery(String sql, Object[] params,
			QueryCallback callback) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);

			if (null != params) {
				for (int i = 0; i < params.length; i++) {
					pstmt.setObject(i + 1, params[i]);
				}
			}

			rs = pstmt.executeQuery();
			callback.process(rs);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} finally {
			if (conn != null) {
				datasource.push(conn); // 当conn不为空的时�?将我们的数据库连接重新放入连接池当中，方便下�?��使用
			}
		}
	}

	/**
	 * 静�?内部类：查询回调接口
	 * 
	 * @author Administrator
	 * 
	 */
	public static interface QueryCallback {

		/**
		 * 处理查询结果
		 * 
		 * @param rs
		 * @throws Exception
		 */
		void process(ResultSet rs) throws Exception;

	}

}
