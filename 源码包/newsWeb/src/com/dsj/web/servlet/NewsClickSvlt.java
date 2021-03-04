package com.dsj.web.servlet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NewsClickSvlt extends BaseSvlt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Map<String, Object> newsCount = newsCount(request, response);
		Map<String, Object> periodCount =periodCount(request, response);
		int newssCount = getNewsCount(request, response);
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("name", newsCount.get("name"));
		map.put("newscount", newsCount.get("count"));
		map.put("newsSum", newssCount);
		map.put("logtime", periodCount.get("logtime"));
		map.put("periodcount", periodCount.get("count"));
		setData(response, map);
	}
	
	/**
	 * 新闻曝光总量
	 * @param request
	 * @param response
	 * @return
	 */
	public int getNewsCount(HttpServletRequest request, HttpServletResponse response){
		String sql = "select count(1)  from newscount";
		ResultSet rs = this.getResultSet(sql);
		try {
			if(rs.next()){
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			System.out.println("查询结果集出错！");
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * 查询每条新闻浏览量排行榜
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,Object> newsCount(HttpServletRequest request, HttpServletResponse response){
		String[] names = new String[10];
        String[] counts = new String[10];
        Map<String,Object> retMap = new HashMap<String, Object>();
		String sql = "select name,count from newscount where 1=1 order by count desc limit 10";
		ResultSet rs = this.getResultSet(sql);
		try {
			int i = 0;
            while (rs.next()){
                String name = rs.getString("name");
                String count = rs.getString("count");
                names[i] = name;
                counts[i] = count;
                ++i;
            }
            retMap.put("name", names);
            retMap.put("count", counts);
		} catch (SQLException e) {
			System.out.println("查询结果集出错！");
			e.printStackTrace();
		}
		return retMap;
	}
	/**
	 * 查询时段新闻浏览量排行榜
	 * @param request
	 * @param response
	 * @return
	 */
	public Map<String,Object> periodCount(HttpServletRequest request, HttpServletResponse response){
		String[] logtimes = new String[10];
        String[] counts = new String[10];
        Map<String,Object> retMap = new HashMap<String, Object>();
		String sql = "select logtime,count from periodcount where 1=1 order by count desc limit 10";
		ResultSet rs = this.getResultSet(sql);
		try {
			int i = 0;
            while (rs.next()){
                String logtime = rs.getString("logtime");
                String count = rs.getString("count");
                logtimes[i] = logtime;
                counts[i] = count;
                ++i;
            }
            retMap.put("logtime", logtimes);
            retMap.put("count", counts);
		} catch (SQLException e) {
			System.out.println("查询结果集出错！");
			e.printStackTrace();
		}
		return retMap;
	}
}
