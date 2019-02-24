package com.kauuze.app.api;

import com.kauuze.app.api.inje.sso.*;
import com.kauuze.app.include.annotation.permission.Authorization;
import com.kauuze.app.service.sso.model.UdpModel;
import com.kauuze.app.service.sso.model.UserBaseModel;
import com.kauuze.app.service.sso.model.UserDetailModel;
import com.kauuze.app.service.sso.model.UserPrivateModel;
import com.kauuze.app.include.JsonResult;
import com.kauuze.app.include.StateModel;
import com.kauuze.app.service.sso.SsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户信息api
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
@RestController
@RequestMapping("/sso")
public class SsoController {
    @Autowired
    private SsoService ssoService;


    @RequestMapping("/sendMsCode")
    public JsonResult sendMsCode(@Valid @RequestBody SendMsCodeInje sendMsCodeInje){
        ssoService.sendMsCode(sendMsCodeInje.getPhone());
        return JsonResult.success();
    }

    @RequestMapping("/register")
    public JsonResult register(@Valid @RequestBody RegisterInje registerInje){

        StateModel stateModel = ssoService.register(registerInje.getPhone(),registerInje.getMsCode(),registerInje.getPassword(),registerInje.getNickName());
        int result = stateModel.getState();
        if(result == 1){
            return JsonResult.success(stateModel.getData());
        }
        if(result == 0){
            return JsonResult.failure("验证码错误");
        }else if(result == 2){
            return JsonResult.failure("验证码过期");
        }else if(result == 3){
            return JsonResult.failure("已注册");
        }else if(result == 4){
            return JsonResult.failure("昵称已存在");
        }
        return null;
    }

    @RequestMapping("/login")
    public JsonResult login(@Valid @RequestBody LoginInje loginInje){
        if(loginInje.getPassword() == null && loginInje.getMsCode() == null){
            return JsonResult.mismatch();
        }
        StateModel stateModel = ssoService.login(loginInje.getPhone(),loginInje.getPassword(),loginInje.getMsCode(),loginInje.getProvince(),loginInje.getCity(),loginInje.getIp(),loginInje.getDeviceName());
        int state = stateModel.getState();
        Object data = stateModel.getData();
        if(state == 1){
            return JsonResult.success(data);
        }
        if(state == 0){
            return JsonResult.failure("用户名或密码验证码错误");
        }else if(state == 2){
            return JsonResult.failure("需修改密码");
        }else if(state == 3){
            return JsonResult.failure("验证码过期");
        }else if(state == 4){
            return JsonResult.failure("被禁止登陆",data);
        }
        return null;
    }

    @RequestMapping("/getLoginRecord")
    @Authorization
    public JsonResult getLoginRecord(@RequestAttribute Integer uid){
        return JsonResult.success(ssoService.getLoginRecord(uid));
    }
    /**
     *
     * 0--验证码错误,2--验证码过期,3--用户未注册
     * @return
     */
    @RequestMapping("/alterPassword")
    public JsonResult alterPassword(@Valid @RequestBody AlterPasswordInje alterPasswordInje){

        int result = ssoService.alertPassword(alterPasswordInje.getPhone(), alterPasswordInje.getMsCode(), alterPasswordInje.getNewPassword());
        if(result == 1){
            return JsonResult.success();
        }

        if(result == 0){
            return JsonResult.failure("验证码错误");
        }else if(result == 2){
            return JsonResult.failure("验证码过期");
        }else if(result == 3){
            return JsonResult.failure("用户未注册");
        }
        return null;
    }

    @RequestMapping("/alterPortrait")
    @Authorization
    public JsonResult alterPortrait(@RequestAttribute Integer uid, @Valid @RequestBody AlterPortraitInje alterPortraitInje){
        ssoService.alertPortrait(uid, alterPortraitInje.getUrl());
        return JsonResult.success();
    }

    @RequestMapping("/alterSex")
    @Authorization
    public JsonResult alterSex(@RequestAttribute Integer uid, @Valid @RequestBody AlterSexInje alterSexInje){
        ssoService.alertSex(uid, alterSexInje.getSex());
        return JsonResult.success();
    }

    @RequestMapping("/alterPersonalSign")
    @Authorization
    public JsonResult alterPersonalSign(@RequestAttribute Integer uid, @Valid @RequestBody AlterPersonalSignInje alterPersonalSignInje){
        ssoService.alertPersonalSign(uid, alterPersonalSignInje.getPersonalSign());
        return JsonResult.success();
    }

    @RequestMapping("/alterProvince")
    @Authorization
    public JsonResult alterProvince(@RequestAttribute Integer uid,@Valid @RequestBody AlterProvinceInje alterProvinceInje){
        ssoService.alertProvince(uid, alterProvinceInje.getProvince());
        return JsonResult.success();
    }

    /**
     * 参数不含则直接为空
     * @param uid
     * @return
     */
    @RequestMapping("/alterUserData")
    @Authorization
    public JsonResult alterUserData(@RequestAttribute Integer uid,@Valid @RequestBody AlterUserData alterUserData){
        ssoService.alertUserData(uid, alterUserData.getBirthday(), alterUserData.getTrueName(), alterUserData.getAddress());
        return JsonResult.success();
    }

    @RequestMapping("/getUdpModel")
    @Authorization
    public JsonResult getUdpModel(@RequestAttribute Integer uid){
        UdpModel udpModel = ssoService.getUdpModel(uid);
        return JsonResult.success(udpModel);
    }

    @RequestMapping("/getUserPrivateModel")
    @Authorization
    public JsonResult getUserPrivateModel(@RequestAttribute Integer uid){
        UserPrivateModel userPrivateModel = ssoService.getUserPrivateModel(uid);
        return JsonResult.success(userPrivateModel);
    }

    @RequestMapping("/getUserDetailModel")
    public JsonResult getUserDetailModel(@Valid @RequestBody UidInje uidInje){
        UserDetailModel userDetailModel = ssoService.getUserDetailModel(uidInje.getUid());
        return JsonResult.success(userDetailModel);
    }

    /**
     * 通过Uid nickName phone进行搜索
     * @return
     */
    @RequestMapping("/findUserDetailModelBySearch")
    public JsonResult findUserDetailModelBySearch(@Valid @RequestBody FindUserDetailModelBySearchInje findUserDetailModelBySearchInje){
        UserDetailModel userDetailModel = ssoService.findUserDetailModelByNickNameOrUidOrPhone(findUserDetailModelBySearchInje.getNickNameOrUidOrPhone());
        return JsonResult.success(userDetailModel);
    }

    @RequestMapping("/getUserBaseModel")
    public JsonResult getUserBaseModel(@Valid @RequestBody UidInje uidInje){
        UserBaseModel userBaseModel = ssoService.getUserBaseModel(uidInje.getUid());
        return JsonResult.success(userBaseModel);
    }

    @RequestMapping("/getListUserBaseModel")
    public JsonResult getListUserBaseModel(@Valid @RequestBody GetListUserBaseModelInje getListUserBaseModelInje){
        List<UserBaseModel> list = ssoService.getListUserBaseModel(getListUserBaseModelInje.getUidList());
        return JsonResult.success(list);
    }

}
