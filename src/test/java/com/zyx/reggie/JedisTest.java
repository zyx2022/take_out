package com.zyx.reggie;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Set;

public class JedisTest {

    @Test
    public void testRedis(){
        //获取连接
        Jedis jedis = new Jedis("localhost", 6379);

        //执行具体的操作
        jedis.set("username", "xiaoming");

        String username = jedis.get("username");
        System.out.println(username);

//        jedis.del("username");

        jedis.hset("myhash","addr", "nanjing");
        String hget = jedis.hget("myhash", "addr");
        System.out.println(hget);

        System.out.println("*******************************");

        Set<String> keys = jedis.keys("*");
        for (String key : keys){
            System.out.println(key);
        }

        //关闭连接
        jedis.close();
    }
}
