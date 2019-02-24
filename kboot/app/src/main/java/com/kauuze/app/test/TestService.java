package com.kauuze.app.test;

import com.kauuze.app.domain.sso.dao.UdpDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class TestService {
    @Autowired
    private UdpDao udpDao;
    public void test(){
        System.out.println("test");
    }
}
