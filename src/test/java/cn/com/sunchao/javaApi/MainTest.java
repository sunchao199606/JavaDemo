package cn.com.sunchao.javaApi;

import org.junit.Test;

import java.time.Duration;

/**
 * @Description : 各种测试
 * @Author :sunchao
 * @Date: 2020-04-17 10:09
 */
public class MainTest {
    @Test
    public void testDuration() {
        Duration duration = Duration.ofMinutes(110);
        System.out.println((float) duration.toHours());
//        LocalTime lt = LocalTime.now();
//        System.out.println(lt.getHour() + lt.getMinute());
//        System.out.println(Float.compare(1,1.5f));
        System.out.println((long) 100 / 66);
    }
}
