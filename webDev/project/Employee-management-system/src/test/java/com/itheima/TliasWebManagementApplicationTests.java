package com.itheima;

import com.itheima.controller.DeptController;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class TliasWebManagementApplicationTests {

    @Autowired
    private DefaultErrorAttributes defaultErrorAttributes;

    @Test
    void contextLoads() {
    }

    /**
     * 生成Jwt Token
     */
    @Test
    public void testGenJwt(){
        Map<String, Object> claims = new HashMap<>();
        claims.put("nunotaba", 100);
        claims.put("shinobu", 22);


    String jwt = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, "nunotaba") // 签名算法
                .setClaims(claims) // 设置自定义的payload
                .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 设置有效期,可能加入了时间戳?
                .compact();

        System.out.println(jwt);

    }

    /**
     * 解析一个JWT令牌
     */
    @Test
    public void testParseJwt(){
    Claims claims = Jwts.parser()
                .setSigningKey("nunotaba")
                .parseClaimsJws("eyJhbGciOiJIUzI1NiJ9.eyJudW5vdGFiYSI6MTAwLCJzaGlub2J1IjoyMiwiZXhwIjoxNzYwODg2NTYzfQ.li9qVm3_GsI9CfoPkLO_wn44iQ1le6Hb_y15Q0j_8iw")
                .getBody();

        System.out.println(claims);

    }



    @Autowired
    // 注入一个IOC容器对象
    private ApplicationContext applicationContext;

    @Test
    public void testGetBean(){
        // 根据Bean的名称获取
        DeptController bean1 = (DeptController) applicationContext.getBean("deptController");
        System.out.println(bean1);

        // 直接根据class获取
        DeptController bean2 = applicationContext.getBean(DeptController.class);
        System.out.println(bean2);

        // 根据名称及类型获取
        DeptController bean3 = applicationContext.getBean("deptController", DeptController.class);
        System.out.println(bean3);

    }

}
