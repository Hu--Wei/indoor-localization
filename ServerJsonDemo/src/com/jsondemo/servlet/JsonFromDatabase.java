package com.jsondemo.servlet;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class JsonFromDatabase {
	
	/** 从数据库中读取数据 
	 * @param name，所要读取用户的userName
	 * @return userInfos.toString(), 返回读取的用户信息的字符流
	 * 
	 * */
	public String getUserInfo(String name) {
		//准备userInfos json对象
		JSONObject userInfos =  new JSONObject();
		//准备 userInfos中的Json Array
		JSONArray userArray = new JSONArray();
		//准备 mySQL查询命令
		String sqlstr = "select * from users where  userName = ?";
		//准备连接
		Connection conn = null;
		CallableStatement cs = null;
		ResultSet rs = null;
		try {
			//连接mySQL 数据库
			conn = DBUtil.getConnForMySql();
			//输出连接成功到控制台
			System.out.println("Connect to database");
			//准备查询 prepareCall 返回CallableStatement
			cs =conn.prepareCall(sqlstr, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			cs.setString(1, name);
			//查询mySQL
			rs = cs.executeQuery();
			//用查询结果填充userinfo Json对象
			while(rs.next()){
				JSONObject userinfo = new JSONObject();
				userinfo.put("id", rs.getString(1));
				userinfo.put("name", rs.getString(2));
				userinfo.put("age", rs.getString(3));
				userinfo.put("sex", rs.getString(4));
				userArray.add(userinfo);
			}
			//将userArray填入userInfos Json对象
			userInfos.put("users", userArray);
			//捕获数据库操作异常
		}catch (Exception e) {
			e.printStackTrace();
			//无论如何关闭数据库连接
		}finally{
			DBUtil.CloseResources(conn, cs);
		}
		    //返回userInfo中所包含的字符串
		return userInfos.toString();
	}
	/** 向数据库中写入数据 
	 * @param json，要写入的数据的json格式
	 * @return ret_id, 写入的数据的id
	 * 
	 * */
	public int InsertJsonIntoDatabase(JSONObject json){
		int ret_id=0;
		Connection conn= null;
		
		String pos = json.getString("pos");
		Double x = json.getDouble("x");
		Double y = json.getDouble("y");
		Double z = json.getDouble("z");
		Integer num = json.getInt("num");
		System.out.println(pos + '\t' + x + '\t' + y + '\t' + z + '\t' + num);
		String[][] data = new String[num][];
		for(Integer i = 0; i < num; i++) {
			data[i] = (json.getString(((Integer)i).toString())).split("\\&");
			System.out.println(data[i][0] + '\t' + data[i][1]);
		}
		
		try {
	    	    conn = DBUtil.getConnForMySql();
				System.out.println("Connect to database");

				//创建声明
	            Statement stmt;
	            stmt = conn.createStatement();
	            
	            String query = "";
	            query = "ALTER TABLE wifi ADD post FLOAT(7,4)";
	            stmt.executeUpdate(query);
	            
	            //新增一条数据，将数据的信息写入数据库
	            query = "INSERT INTO wifi (pos, x, y, z) VALUES ('"+pos+"', '"+x+"', '"+y+"', '"+z+"')";
	            stmt.executeUpdate(query);
	            //查询插入的信息的ID
	            ResultSet res = stmt.executeQuery("select LAST_INSERT_ID()");
	            
	            if (res.next()) {
	                ret_id = res.getInt(1);
	                System.out.print(ret_id);
	            }

	        } catch (Exception e) {
	            System.out.print("MYSQL ERROR:" + e.getMessage());
	        }finally{
				DBUtil.CloseResources(conn);
	        }
	    return ret_id;
	}

}
