# spring-framework

## 一、spring基础应用

### 1.1 spring优势

* 解耦
* AOP编程支持
* 声明式事务
* 对测试的支持
* 方便集成各种优秀的框架
* 对JavaEE API的封装
* 优秀的源码

### 1.2 spring 核心思想

**IOC和DI的区别是什么：**同一件事情的不同描述，IOC是针对于对象来说的，容器自动创建对象。DI是针对器来说的，当创建对象时发现此对象还需依赖别的对象，就把该对象注入进去。

**AOP的本质：** 在不改变原有代码逻辑的情况下，增加横切逻辑，避免横切逻辑代码重复。

### 1.3纯注解模式

**对应注解**
@Configuration注解，表明当前类是一个配置类

@ComponentScan注解，替代context:component-scan,包扫描

@PropertySource注解，引入外部配置文件

@Import注解，引入其他配置类

@value注解，对变量进行赋值，也可以使用${}读取配置文件中的信息

@Bean注解，将方法返回的对象加入SpringIOC容器

```java
// @Configuration 注解表明当前类是一个配置类
@Configuration
@ComponentScan({"com.lagou.edu"})
@PropertySource({"classpath:jdbc.properties"})
/*@Import()*/
public class SpringConfig {

    @Value("${jdbc.driver}")
    private String driverClassName;
    @Value("${jdbc.url}")
    private String url;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;


    @Bean("dataSource")
    public DataSource createDataSource(){
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(driverClassName);
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        return  druidDataSource;
    }
}

```

## 二、spring IOC高级特性

### 2.1	延迟加载

```xml
<bean id="lazyResult" class="com.lagou.edu.pojo.Result"   lazy-init="false"/>
```

lazy-init = "false",立即加载，表示在spring启动时，立刻进行实例化。

​		如果不想让一个singleton bean 在applicationContext启动时被提前实例化，而是第一次向容器通过getBean索取bean索取bean实例化可以向以下这样配置：

```xml
<bean id="lazyResult" class="com.lagou.edu.pojo.Result"   lazy-init="true"/>
```

​		如果一个立即加载的bean1,引用了一个延迟加载的bean2，那么bean1在容器启动时被实例化，而bean2由于被bean1引用也被实例化：这种情况也是符合延时加载bean，在第一次调用时才被实例化的规则。

纯注解模式下需要在被延迟加载的类上增加@Lazy注解，默认值为打开延迟加载

```java
@Lazy
```

**应用场景**

（1）	开启延迟加载一定程度上提高容器的启动和运转速度。

（2）	对于不常用的bean设置延迟加载，这样偶尔使用的时候在加载，不必要从一开始该bean就占用资源



### 2.2	spirngBean的生命周期

![image-20200525222822960](C:\Users\13276\AppData\Roaming\Typora\typora-user-images\image-20200525222822960.png)









### 2.3	如何解决循环依赖

#### 2.3.1	什么是循环依赖

​				类A依赖于类B，类B又依赖于类A，两个类互相依赖。

#### 2.3.2	什么样得循环依赖无法解决

* 单例bean构造器参数循环依赖，无法解决
* prototype原型bean循环依赖，无法解决

对于原型bean的初始化过程中不论是通过构造器参数循环依赖还是通过setxxx方法产生循环依赖，spring都会直接报错

#### 2.3.3	解决循环依赖

​		当类A对象在实例化后立即放入三级缓存（提前暴漏自己），当A发现自己需要依赖B就创建B对象，B在创建过程中发现依赖于A就去三级缓存中使用尚未成型(尚未成型：只用一个空的引用，属性尚未赋值)的Bean A。然后把bean A升级到二级缓存中，而bean B自生则创建成功是一个成型的SpringBean，放入一级缓存中（单例池，存放所有成型的Bean），这时候Bean A 使用一级缓存池中的Bean B得以创建。

解决循环依赖关键代码

```java
@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //desc  先从singletonObjects（一级缓存）取
    Object singletonObject = this.singletonObjects.get(beanName);
 
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        //desc  取不到且创建中,那么这里使用同步锁阻塞一会,等待创建完成
        //这里会发现 当真正创建完bean时会调用addSingletonFactory()  这时候也会锁住singletonObjects
        synchronized (this.singletonObjects) {
            //desc 同步阻塞+尝试从earlySingletonObjects(二级缓存)获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                //desc 如果二级缓存取不到&&允许从singletonFactories通过getObject获取
                //desc 通过singletonFactory.getObject()(三级缓存)获取工厂创建该bean
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    //desc 通过三级缓存的Factory创建目标bean  并放入2级缓存
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

