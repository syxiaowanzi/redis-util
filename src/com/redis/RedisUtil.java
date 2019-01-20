package com.redis;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/**
 * log4j(commons-logging/log4j)
 * redis(jedis/commons-pool2)
 * @author wangming
 *
 */
public class RedisUtil {
	private static JedisPool pool=null;
	//最大连接数
	private static int max_active=50;
	//最大空闲数
	private static int max_idle=40;
	//等待时间
	private static long max_waitmillis=3000;
	//redis主机地址
	private static String host;
	//redis端口
	private static int port;
	//redis用户密码
	private static String password;
	//过期时间
	private static int expireTime = 2*60*60*1000;
	
	static Logger logger = Logger.getLogger(RedisUtil.class);
	static{
		try {
			//获取配置文件内容
			InputStream in = RedisUtil.class.getResourceAsStream("redis.properties");
			Properties prop = new Properties();
			prop.load(in);
			RedisUtil.max_active = Integer.parseInt(prop.getProperty("max_active"));
			RedisUtil.max_idle = Integer.parseInt(prop.getProperty("max_idle"));
			RedisUtil.max_waitmillis = Long.parseLong(prop.getProperty("max_waitmillis").toString());
			RedisUtil.host = prop.getProperty("host");
			RedisUtil.port = Integer.parseInt(prop.getProperty("port"));
			RedisUtil.password = prop.getProperty("password");
			//初始化连接池
			RedisUtil.pool = initPool();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * 初始化连接池
	 * @return
	 */
	private static JedisPool initPool(){
		JedisPool result = null;
		try {
			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxIdle(max_idle);
			config.setMaxWaitMillis(max_waitmillis);
			config.setTestOnBorrow(true);
			if(RedisUtil.password==null||"".equals(RedisUtil.password)){
				result = new JedisPool(config,host,port);
			}else{
				result = new JedisPool(config,host,port,3000,RedisUtil.password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * 向redis添加String，成功返回0，失败返回1
	 * @param String
	 */
	public static int set(String key,String value){
		
		if(value==null || key==null){
			logger.debug("redis存储对象时传入参数为空");
			return 1;
		}
		Jedis jedis=null;
		try{
			jedis = RedisUtil.getJedis();
			jedis.set(key, value);
		}catch(Exception e){
			logger.debug("对象存储redis失败");
			return 1;
		}finally{
			RedisUtil.closeJedis(jedis);
		}
		return 0;
	}
	
	/**
	 * redis数据库查询
	 */
	public static String get(String key){
		if(key==null){
			logger.debug("redis查询方法，传入参数为空");
			return null;
		}	
		String result="";
		Jedis jedis=null;
		try{
			jedis = RedisUtil.getJedis();
			result = jedis.get(key);
		}catch(Exception e){
			logger.debug("redis查询方法执行失败"+e);
		}finally{
			RedisUtil.closeJedis(jedis);
		}
		return result;
	}
	
	/**
	 * 封装jedis的hset方法解释如下
	 * @param key	 
	 * @param field  实体类的类名   
	 * @param value  对应分项内容的json串
	 * @return
	 */
	public static long hset(String key,String field,String value){
		long result =0;
		Jedis jedis = null;
		try {
			jedis = RedisUtil.getJedis();
			result = jedis.hset(key, field, value);
			//设置两小时过期
			jedis.expire(key,expireTime);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			closeJedis(jedis);
		}
		return result;
	}
	
	/**
	 * 封装redis的hget方法,，解释如下
	 * @param  key		
	 * @param  field	对应实体类的类名
	 * @return result	对应分项内容的json串
	 */
	public static String hget(String key,String field){
		String  result =null;
		Jedis jedis = null;
		try {
			jedis = RedisUtil.getJedis();
			result = jedis.hget(key, field);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			closeJedis(jedis);
		}
		return result;
	}
	
	
	/**
	 * 设置过期时间
	 * @param value 秒
	 * @return 成功返回0，失败返回1
	 */
	public static int expire(String key,int value){
		if(key==null){
			logger.debug("redis设置失效时间，传入key参数为空");
			return 1;
		}
		Jedis jedis =null;
		try{
			jedis = RedisUtil.getJedis();
			jedis.expire(key, value);
		}catch(Exception e){
			logger.debug("redis设置"+key+"失效时间失败");
			return 1;
		}finally{
			RedisUtil.closeJedis(jedis);
		}
		return 0;
	}
	
	/***
	 * 获取Jedis实例
	 * @return  Jedis实例
	 */
	public static Jedis getJedis(){	
		Jedis result = null;
		try {
			result = RedisUtil.pool.getResource();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/***
	 * 关闭 Jedis
	 * @param jedis
	 */
	public static void closeJedis(Jedis jedis){
		if(jedis!=null){
			jedis.close();
		}
	}
	
	public static void main(String[] args) {
//		Jedis jedis = getJedis();
		String openid = "o5lHPww7MK_ca1-LFLzWUjTRJ7oU";
		RedisUtil.set(openid, "20180920/1537413108683.jpg|20180920/1537413108633.jpg|");
		String str = RedisUtil.get(openid);
		//RedisUtil.expire(RedisKeys.PERSONALINCOMETAX, 10*60*60*1000);
		System.out.println(str);
		String[] strs = str.split("\\|");
		System.out.println(strs[0]);
		/*int i = RedisUtil.expire(RedisKeys.IMAGEACQUISITION, 0);
		System.out.println(i);*/
	}
}
