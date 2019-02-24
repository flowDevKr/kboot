package com.kauuze.app.include;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 常用方法工具类
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
public class BoxUtil {
    public static List<String> putStrList(String str){
        List<String> list = new ArrayList<>(1);
        list.add(str);
        return list;
    }

    public static String[] splitUnderline(String s,int length){
        String[] some = s.split("_");
        if(some.length != length){
            throw new RuntimeException("拆分下划线值length不匹配");
        }
        return some;
    }

    public static String getFormatTime(){
        String fomart = "yyyyMMddHHmmss";
        SimpleDateFormat df = new SimpleDateFormat(fomart);
        Calendar calendar = Calendar.getInstance();
        return df.format(calendar.getTime());
    }

    public static String convertSimpleTime(Long mill){
        String fomart = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat df = new SimpleDateFormat(fomart);
        return df.format(mill);
    }
    public static Long convertMill(String simpleTime){
        String fomart = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat df = new SimpleDateFormat(fomart);
        try {
            return df.parse(simpleTime).getTime();
        } catch (ParseException e) {
        }
        throw new RuntimeException("covertMill转换失败");
    }

    public static String[] getFormatTimeSub(){
        String fomart = "yyyy_MM_dd_HH_mm_ss";
        SimpleDateFormat df = new SimpleDateFormat(fomart);
        Calendar calendar = Calendar.getInstance();
        return splitUnderline(df.format(calendar.getTime()),6);
    }

    public static String toJsonString(Object source){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(source);
            return jsonString;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("toJsonString转换失败");
        }
    }

    public static <T> T parseJsonString(String jsonString,Class<T> targetType){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            T target = objectMapper.readValue(jsonString,targetType);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("parseJsonString转换失败");
        }
    }

    public static <T> T typeCast(Object o,Class<T> targetType){
        return parseJsonString(toJsonString(o),targetType);
    }

    /**
     * 不支持对象中包含数组和集合
     * @param source
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> T modelCopy(Object source, Class<T> targetType){
        try {
            T target = targetType.getConstructor().newInstance();
            BeanUtils.copyProperties(source,target);
            return target;
        } catch (Exception e){
            throw new RuntimeException("modelCopy,target对象生成失败");
        }
    }

    /**
     * 拷贝成对象的集合,不支持对象中包含数组和集合
     * @param source
     * @param objectType
     * @param <T>
     * @return
     */
    public static <T> List<T> listModelCopy(List source, Class<T> objectType){
        List<T> list = new ArrayList<>();
        T target = null;
        for (Object o : source) {
            try {
                target = objectType.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("listModelCopy,target对象生成失败");
            }
            BeanUtils.copyProperties(o,target);
            list.add(target);
        }
        return list;
    }


    /**
     * 也可以多条件排序
     * List< Order> orders=new ArrayList< Order>();
     orders.add( new Order(Direction. ASC, "c"));
     orders.add( new Order(Direction. DESC, "d"));
     Pageable pageable= new PageRequest(pageNumber, pageSize, new Sort(orders));
     * @param page
     * @param size
     * @param sortName
     * @param isFromBig
     * @return
     */
    public static Pageable getPageable(int page, int size, String sortName, boolean isFromBig){
        Sort sort = null;
        if(isFromBig){
            sort = new Sort(Sort.Direction.DESC,sortName);
        }else{
            sort =new Sort(Sort.Direction.ASC,sortName);
        }
        return PageRequest.of(page - 1,size,sort);
    }

    /**
     * 按id从大到小排序(按插入顺序),每页显示20条
     * @param page
     * @return
     */
    public static Pageable getPageableDefault(int page){
        return PageRequest.of(page - 1,20,new Sort(Sort.Direction.DESC,"id"));
    }

    public static Pageable getPageableDefaultFromSmall(int page){
        return PageRequest.of(page - 1,20,new Sort(Sort.Direction.ASC,"id"));
    }

    public static Sort getSort(boolean fromBig,String field){
        if(fromBig){
            return new Sort(Sort.Direction.DESC,field);
        }else{
            return new Sort(Sort.Direction.ASC,field);
        }
    }
}
