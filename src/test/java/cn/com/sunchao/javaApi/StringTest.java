package cn.com.sunchao.javaApi;

import org.junit.jupiter.api.Test;

/**
 * @Description : 字符串测试
 * @Author :sunchao
 * @Date: 2020-07-13 13:39
 */
public class StringTest {

    StringTest() {
        this("");
        System.out.println();
    }

    StringTest(String string) {

    }

    @Test
    public void add() {
//        String a = "a";
//        String b = "b";
//        String c = a + b;
//        System.out.println(c);
        int i = 8;
        float f = 3.0f;
        System.out.println((int) (i / f));
    }
}