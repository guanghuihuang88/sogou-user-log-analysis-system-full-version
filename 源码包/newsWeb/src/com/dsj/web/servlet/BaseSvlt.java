package com.dsj.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import com.dsj.web.jdbc.JDBCHelper;

public class BaseSvlt extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JDBCHelper jdbcHelper = null;
	
	ResultSet rs;//查询返回的结果
	//实例化jdbcHelper
	public void init(){
		jdbcHelper = JDBCHelper.getInstance();
	}
	
	
	/**
	 * 返回ResultSet对象
	 * @return
	 */
	public ResultSet getResultSet(String sql){
		jdbcHelper.executeQuery(sql, null, new JDBCHelper.QueryCallback() {
			@Override
			public void process(ResultSet rs1) throws Exception {
				rs = rs1;
			}
		});
		return rs;
	}
	
	/**
	 * 向页面输出数据
	 * @param response
	 * @param map
	 * @throws IOException
	 */
	public <K,V> void setData(HttpServletResponse response, Map<K, V> map)
			throws IOException {
		response.setContentType("text/html;charset=utf-8");
		PrintWriter pw = response.getWriter();
		pw.write(JSONObject.fromObject(map).toString());
		pw.flush();
		pw.close();
	}	
}
