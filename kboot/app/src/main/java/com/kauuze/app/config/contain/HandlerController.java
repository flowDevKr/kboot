package com.kauuze.app.config.contain;


import com.kauuze.app.include.annotation.permission.GreenWay;
import com.kauuze.app.include.BoxUtil;
import com.kauuze.app.include.JsonResult;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 全局错误拦截
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
@Controller
public class HandlerController implements ErrorController {
   
  @RequestMapping("/error")
  @GreenWay
  public String error(HttpServletResponse response) {
    try {
      int status = response.getStatus();
      if(status == 404){
        response.getWriter().write("not find");
      }else if(status == 400){
        response.setStatus(200);
        response.getWriter().write(BoxUtil.toJsonString(JsonResult.mismatch()));
      } else if(status == 500){
        response.getWriter().write("service go wrong");
      }
    } catch (IOException e) {
        throw new RuntimeException("/error error");
    }
    return null;
  }

  @Override
  public String getErrorPath() {
    return null;
  }
}
