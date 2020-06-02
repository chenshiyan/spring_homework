package com.lagou.edu.factory;

import com.lagou.edu.annotation.CustomAutowired;
import com.lagou.edu.annotation.CustomService;
import com.lagou.edu.annotation.CustomTransactional;
import com.lagou.edu.annotation.Repository;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象
    private static List<String> classes = new ArrayList<>(0);


    static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            Element element1 = rootElement.element("compent-scan");
            String basePackage = element1.attributeValue("base-package");
            //1、获取该包下的所有类
            String packagePath = getPackagePath(basePackage);
            getClassForPath(new File(packagePath),getPath());
            //2、查看类上是否有特定的注解并初始化类
            initClassMap(classes);
            //3、查看有没有autowired注解，为属性赋值
            initFiled();
            //4、查看该类有没有被CustomTransactional标记
            initTransactional();
            //5、重新覆盖更新
            initFiled();

//            List<Element> beanList = rootElement.selectNodes("//bean");
////            for (int i = 0; i < beanList.size(); i++) {
////                Element element =  beanList.get(i);
////                // 处理每个bean元素，获取到该元素的id 和 class 属性
////                String id = element.attributeValue("id");        // accountDao
////                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
////                // 通过反射技术实例化对象
////                Class<?> aClass = Class.forName(clazz);
////                Object o = aClass.newInstance();  // 实例化之后的对象
////
////                // 存储到map中待用
////                map.put(id,o);
////
////            }
////
////            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
////            // 有property子元素的bean就有传值需求
////            List<Element> propertyList = rootElement.selectNodes("//property");
////            // 解析property，获取父元素
////            for (int i = 0; i < propertyList.size(); i++) {
////                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
////                String name = element.attributeValue("name");
////                String ref = element.attributeValue("ref");
////
////                // 找到当前需要被处理依赖关系的bean
////                Element parent = element.getParent();
////
////                // 调用父元素对象的反射功能
////                String parentId = parent.attributeValue("id");
////                Object parentObject = map.get(parentId);
////                // 遍历父对象中的所有方法，找到"set" + name
////                Method[] methods = parentObject.getClass().getMethods();
////                for (int j = 0; j < methods.length; j++) {
////                    Method method = methods[j];
////                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
////                        method.invoke(parentObject,map.get(ref));
////                    }
////                }
////
////                // 把处理之后的parentObject重新放到map中
////                map.put(parentId,parentObject);
////
////            }


        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

    /**
     * 获取扫描包路径
     * @return
     */
    private static String getPackagePath(String packageName) throws UnsupportedEncodingException {
        //1.获取项目类路径
        String classPath = getPath();
        //2.将包名转换为类名
        String packagePath = packageName.replace(".", File.separator);
        //将项目路径和包路径合在一起
        packagePath = classPath + packagePath;
        return packagePath;
    }

    /**
     * 获取该包下所有类,放入集合内
     * @param packageFile
     */
    private static void getClassForPath(File packageFile, String packagePath) {
        if (packageFile.isDirectory()) {
            File[] files = packageFile.listFiles();
            for (File file : files) {
                getClassForPath(file, packagePath);
            }
        } else {
            String classPath = packageFile.getPath();
            if (packageFile.getName().endsWith(".class")) {
                String className = classPath.replace(packagePath.replace("/","\\").replaceFirst("\\\\",""),"").replace("\\",".").replace(".class","");
                classes.add(className);
            }
        }
    }
    /**
     * 获取项目路径
     * @return
     */
    private static String getPath() throws UnsupportedEncodingException {
        String classPath = BeanFactory.class.getResource("/").getPath();
        //解决path乱码
        classPath = URLDecoder.decode(classPath, "utf-8");
        return classPath;
    }

    /**
     * 初始化map
     * @param classes
     * @throws Exception
     */
    private static void initClassMap(List<String> classes) throws Exception {
        for (String className : classes) {
            Class clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(CustomService.class)){
                String value = ((CustomService) clazz.getAnnotation(CustomService.class)).value();
                classMapPutValue(value,className,clazz);
            }else if (clazz.isAnnotationPresent(Repository.class)){
                String value = ((Repository) clazz.getAnnotation(Repository.class)).value();
                classMapPutValue(value,className,clazz);
            }
        }
    }

    private static void initTransactional(){
        ProxyFactory proxyFactory = (ProxyFactory)map.get("proxyFactory");
        for (Map.Entry<String,Object> entry : map.entrySet()){
            Object value = entry.getValue();
            Class<?> aClass = value.getClass();
            if (aClass.isAnnotationPresent(CustomTransactional.class)){
                //该类实现接口使用JDK动态代理否则使用JDK动态代理
                if (aClass.getInterfaces().length>0){
                    Object jdkProxy = proxyFactory.getJdkProxy(value);
                    map.put(entry.getKey(),jdkProxy);
                }else{
                    Object cglibProxy = proxyFactory.getCglibProxy(value);
                    map.put(entry.getKey(),cglibProxy);
                }
            }
        }
    }

    /**
     * 填充map
     * @param value
     * @param className
     * @param clazz
     * @throws Exception
     */
    private static void classMapPutValue(String value, String className, Class clazz) throws Exception {
        value = value == null || "".equals(value) ?
                toLowerCaseFirstOne(
                        className.substring(className.lastIndexOf(".") + 1)) : value;
        Object object = clazz.newInstance();
//        //该类是否实现有事务注解
//        if (isTransactional){
//            //如果该类实现了接口使用JDK动态代理，
//            if (object.getClass().getInterfaces().length>0){
//
//            }
//        }
        map.put(value, object);
    }

    /**
     * 首字母转小写
     * @param str
     * @return
     */
    private static String toLowerCaseFirstOne(String str){
        if(Character.isLowerCase(str.charAt(0))) {
            return str;
        }
        else {
            return (new StringBuilder())
                    .append(Character.toLowerCase(str.charAt(0)))
                    .append(str.substring(1))
                    .toString();
        }
    }
    /**
     * 初始化字段值
     */
    private static void initFiled() throws IllegalAccessException {
        for(Map.Entry<String,Object> entry : map.entrySet()){
            Object object = entry.getValue();
            Field[] fields = object.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(CustomAutowired.class)){
                    field.setAccessible(true);
                    field.set(object,map.get(field.getName()));
                }
            }
        }
    }
}
