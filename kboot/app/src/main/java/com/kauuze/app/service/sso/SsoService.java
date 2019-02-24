package com.kauuze.app.service.sso;

import com.kauuze.app.api.inje.sso.UidInje;
import com.kauuze.app.domain.sso.dao.*;
import com.kauuze.app.domain.sso.entity.*;
import com.kauuze.app.especiallyutil.SHA256;
import com.kauuze.app.service.sso.model.UdpModel;
import com.kauuze.app.service.sso.model.UserBaseModel;
import com.kauuze.app.service.sso.model.UserDetailModel;
import com.kauuze.app.service.sso.model.UserPrivateModel;
import com.kauuze.app.especiallyutil.MsCodeSendUtil;
import com.kauuze.app.include.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author kauuze
 * @createTime 2019.01.24 11:13
 * @IDE IntelliJ IDEA 2017.2.5
 **/
@Service
@Transactional(rollbackOn = Exception.class)
public class SsoService {
    @Autowired
    private PhoneCodeDao phoneCodeDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private TokenDao tokenDao;
    @Autowired
    private UdpDao udpDao;
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private LoginRecordDao loginRecordDao;

    public void sendMsCode(String phone){
        long mill = Rand.mill();
        int code = Rand.getNumber(6);
        MsCodeSendUtil.sendTp1(phone,code);
        PhoneCode phoneCode = phoneCodeDao.findByPhone(phone);
        if(phoneCode == null){
            PhoneCode phoneCode2 = new PhoneCode(null,phone,code,mill + 1000*60*5,0);
            phoneCodeDao.save(phoneCode2);
        }else{
            PhoneCode phoneCode2 = new PhoneCode(phoneCode.getId(),phone,code,mill + 1000*60*5,0);
            phoneCodeDao.save(phoneCode2);
        }
    }

    /**
     * 0--错误,2--过期
     * @param phone
     * @param msCode
     * @return
     */
    public int validMsCode(String phone,int msCode){
        long mill = Rand.mill();
        PhoneCode phoneCode = phoneCodeDao.findByPhone(phone);
        if(phoneCode == null){
            return 0;
        }
        if(phoneCode.getOverdueTime() < mill){
            return 2;
        }
        if(phoneCode.getFailCount() > 3){
            return 2;
        }
        if(!RU.isIntegerEq(phoneCode.getMsCode(),msCode)){
            phoneCodeDao.incrFailCount(phoneCode.getId());
            return 0;
        }
        return 1;
    }

    /**
     * 用户私有信息
     * @param uid
     * @return
     */
    public UserPrivateModel getUserPrivateModel(int uid){
        Optional<User> optional = userDao.findById(uid);
        if (!optional.isPresent()){
            return null;
        }
        User user = optional.get();
        Token token = tokenDao.findByUid(uid);
        UserInfo userInfo = userInfoDao.findById(uid).get();
        UserPrivateModel userPrivateModel = new UserPrivateModel(user.getId(),token.getAuthorization(),user.getPhone()
                ,user.getNickName(),userInfo.getSex(),userInfo.getPortrait(),user.getExperience(),token.getRole(),token.getRoleEndTime(),token.getState(),token.getStateEndTime());
        return userPrivateModel;
    }

    /**
     * 0--验证码错误,2--验证码过期,3--已注册,4--昵称已存在
     * 1data--UserPrivateModel
     * @param phone
     * @param msCode
     * @param password
     * @return
     */
    public StateModel register(String phone,int msCode,String password,String nickName){
        int result = validMsCode(phone,msCode);
        StateModel stateModel = new StateModel();
        if(result != 1){
            return stateModel.setState(result);
        }
        if(userDao.findByPhone(phone) != null){
            return stateModel.setState(3);
        }

        if(userDao.findByNickName(nickName) != null){
            return stateModel.setState(4);
        }
        User user = new User(null,nickName,0,phone, SHA256.encrypt(password),0,null);
        userDao.save(user);
        int uid = user.getId();
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(uid);
        userInfoDao.save(userInfo);
        Token token2 = new Token(null,uid, Token.generateAuthorization(user.getId()),"user",null,"normal",null,null);
        tokenDao.save(token2);
        Udp udp = new Udp(null,uid,new BigDecimal("0"),new BigDecimal("0"),new BigDecimal("0"));
        udpDao.save(udp);
        return stateModel.setState(1).setData(getUserPrivateModel(uid));
    }

    /**
     * 有密码先密码,参数验证可以为null
     * 0--用户名或密码验证码错误,2--需修改密码,3--验证码过期,4--被禁止登陆
     * 1data--UserPrivateModel 4data--banEndTime
     * @param phone
     * @param password
     * @return
     */
    public StateModel login(String phone, String password, Integer msCode,String province,String city,String ip,String deviceName){
        long mill = Rand.mill();
        StateModel stateModel = new StateModel();
        User user = userDao.findByPhone(phone);
        if(user == null){
            return stateModel.setState(0);
        }
        if(user.getFailCount() > 10){
            return stateModel.setState(2);
        }
        int uid = user.getId();
        if(password != null){//密码登录
            if(!RU.isIntegerEq(user.getPassword(),SHA256.encrypt(password))){
                userDao.incrFailCount(uid);
                return stateModel.setState(0);
            }
            //已通过密码验证
        }else{//验证码登录
            int result = validMsCode(phone,msCode);
            if(result == 0){
                return stateModel.setState(0);
            }
            if(result == 2){
                return stateModel.setState(3);
            }
            //已通过验证码登录
        }
        //重置
        userDao.setZeroFailCount(uid);
        Token token = tokenDao.findByIdForUpdate(uid);
        Long stateEndTime = token.getStateEndTime();
        if(stateEndTime != null && stateEndTime <= mill){
            token.setState("normal");
            token.setStateEndTime(null);
        }
        if(RU.isIntegerEq(token.getState(),"ban")){
            Map<String,String> map = new HashMap<>();
            map.put("stateEndTime", String.valueOf(token.getStateEndTime()));
            map.put("stateCause",token.getStateCause());
            return stateModel.setState(4).setData(map);
        }
        String role = token.getRole();
        Long roleEndTime = token.getRoleEndTime();
        if(roleEndTime != null && roleEndTime <= mill && RU.isIntegerEq(role,"vip")){
            token.setRole("user");
            token.setRoleEndTime(null);
        }

        String authorization = Token.generateAuthorization(uid);
        tokenDao.save(token.setAuthorization(authorization));
        LoginRecord loginRecord = new LoginRecord(null,uid,mill,province,city,ip,deviceName);
        loginRecordDao.save(loginRecord);
        if( loginRecordDao.countByUid(uid) > 20){
            loginRecordDao.deleteById(loginRecordDao.getSmallIdByUid(uid));
        }
        return stateModel.setState(1).setData(getUserPrivateModel(uid));
    }

    /**
     * 获取用户登录记录
     * @param uid
     * @return
     */
    public List<LoginRecord> getLoginRecord(int uid){
        List<LoginRecord> list = loginRecordDao.findByUid(uid);
        return list;
    }

    /**
     * 修改密码
     * 0--验证码错误,2--验证码过期,3--用户未注册
     * @return
     */
    public int alertPassword(String phone,int msCode,String newPassword){
        User user = userDao.findByPhone(phone);
        if(user == null){
            return 3;
        }
        int result = validMsCode(phone,msCode);
        if(result != 1){
            return result;
        }
        user = userDao.findByIdForUpdate(user.getId());
        user.setPassword(SHA256.encrypt(newPassword));
        user.setFailCount(0);
        userDao.save(user);
        return 1;
    }

    public void alertPortrait(int uid,String url){
        UserInfo userInfo = userInfoDao.findByIdForUpdate(uid);
        userInfo.setPortrait(url);
        userInfoDao.save(userInfo);
    }

    public void alertSex(int uid,String sex){
        UserInfo userInfo = userInfoDao.findByIdForUpdate(uid);
        userInfo.setSex(sex);
        userInfoDao.save(userInfo);
    }

    public void alertPersonalSign(int uid, String personalSign){
        UserInfo userInfo = userInfoDao.findByIdForUpdate(uid);
        userInfo.setPersonalSign(personalSign);
        userInfoDao.save(userInfo);
    }

    public void alertProvince(int uid,String province){
        UserInfo userInfo = userInfoDao.findByIdForUpdate(uid);
        userInfo.setProvince(province);
        userInfoDao.save(userInfo);
    }
    public void alertUserData(int uid,Integer birthday,String trueName,String address){
        UserInfo userInfo = userInfoDao.findByIdForUpdate(uid);
        userInfo.setBirthday(birthday).setTrueName(trueName).setAddress(address);
        userInfoDao.save(userInfo);
    }


    /**
     * 用于详细查询用户
     * @param uid
     * @return
     */
    public UserDetailModel getUserDetailModel(int uid){
        Optional<User> optional = userDao.findById(uid);
        if(!optional.isPresent()){
            return null;
        }
        User user = optional.get();
        Token token = tokenDao.findByUid(uid);
        UserInfo userInfo = userInfoDao.findById(uid).get();
        UserDetailModel userDetailModel = new UserDetailModel(uid,token.getRole(),user.getNickName(),
                userInfo.getSex(),userInfo.getPortrait(),userInfo.getProvince(), userInfo.getBirthday(),user.getExperience(),userInfo.getPersonalSign());
        return userDetailModel;
    }

    /**
     * 用户详细信息查询
     * @param nickNameOrUidOrPhone
     * @return
     */
    public UserDetailModel findUserDetailModelByNickNameOrUidOrPhone(String nickNameOrUidOrPhone){
        Integer uid = null;
        if(!RU.isNumber(nickNameOrUidOrPhone)) {
            User user = userDao.findByNickName(nickNameOrUidOrPhone);
            if (user == null) {
                return null;
            } else {
                uid = user.getId();
            }
        }else {
            if(nickNameOrUidOrPhone.length() > 11){
                return null;
            }
            if(RU.isPhone(nickNameOrUidOrPhone)){
                User user = userDao.findByPhone(nickNameOrUidOrPhone);
                if(user == null){
                    return null;
                }else{
                    uid = user.getId();
                }
            }else if(RU.isId(Integer.valueOf(nickNameOrUidOrPhone))){
                uid = Integer.valueOf(nickNameOrUidOrPhone);
            }else {
                return null;
            }
        }
        return getUserDetailModel(uid);
    }

    /**
     * 用户基础信息查询
     * @param uid
     * @return
     */
    public UserBaseModel getUserBaseModel(int uid){
        Optional<User> optional = userDao.findById(uid);
        if(!optional.isPresent()){
            return null;
        }
        User user = optional.get();
        Token token = tokenDao.findByUid(uid);
        UserInfo userInfo = userInfoDao.findById(uid).get();
        UserBaseModel userBaseModel = new UserBaseModel(uid,token.getRole(),user.getNickName(),userInfo.getSex(),userInfo.getPortrait(),user.getExperience());
        return userBaseModel;
    }



    /**
     * 查询用户资产属性
     * @param uid
     * @return
     */
    public UdpModel getUdpModel(int uid){
        Optional<Udp> optional = udpDao.findById(uid);
        if(!optional.isPresent()){
            return null;
        }
        return BoxUtil.modelCopy(optional.get(),UdpModel.class);
    }

    /**
     * 查询多个用户信息
     * @param uidList
     * @return
     */
    public List<UserBaseModel> getListUserBaseModel(List<UidInje> uidList){
        List<UserBaseModel> list = new ArrayList<>();
        for (UidInje uidInje : uidList) {
            list.add(getUserBaseModel(uidInje.getUid()));
        }
        return list;
    }

}
