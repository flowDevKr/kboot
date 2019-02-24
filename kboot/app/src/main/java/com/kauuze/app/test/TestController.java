package com.kauuze.app.test;

import com.kauuze.app.include.annotation.permission.GreenWay;
import com.kauuze.app.include.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * todo 用于测试,线上环境一定要删除
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private TestService testService;

    @RequestMapping("/test")
    @GreenWay
    public JsonResult test(@Valid @RequestBody TestInje testInje){
        testService.test();
        return JsonResult.success(testInje);
    }

}
