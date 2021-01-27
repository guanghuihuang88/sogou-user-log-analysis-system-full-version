package com.hive;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCToHiveServer2 {

	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub
		// 通过反射机制获取驱动程序
		try {
			Class.forName(HiveConfigurationConstants.CONFIG_DRIVERNAME);
			// 建立连接
			Connection conn = DriverManager.getConnection(HiveConfigurationConstants.CONFIG_URL,	HiveConfigurationConstants.CONFIG_USER,HiveConfigurationConstants.CONFIG_PASSWORD);
			// 创建statement
			Statement stmt = conn.createStatement();
			// 准备SQL脚本
			String sql = "select * from stu";
			// statement执行脚本
			ResultSet res = stmt.executeQuery(sql);
			// 处理结果集
			while (res.next()) {
				System.out.println(res.getString(1)+"@"+res.getString(2));
				}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
