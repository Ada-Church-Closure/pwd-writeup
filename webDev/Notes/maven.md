# Maven

> 暂时能用明白就行.
>
> 注意版本不要太新.

构建管理Java项目的工具--->POM(project object model)项目对象模型

1.jar包依赖管理问题 xml描述jar包的信息

2.统一标准的项目结构

3.跨平台项目构建

本地仓库---中央仓库--->cache

maven创建

坐标--->唯一标识--->引入依赖

## 依赖配置

加载一些依赖

比如我们在xml里面添加一个依赖和描述--->坐标

```xml
 <dependencies>
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <version>1.2.12</version>
        </dependency>
    </dependencies>
```

## 依赖传递

自动处理引入的依赖关系--->拓扑排序

\<exclusions> 来做依赖的排除.

## 依赖范围

\<scope>--->控制引入的jar包的作用范围

确定是在test,还是main,或者是打包之后都需要生效.

## 生命周期

clean--->清理

default--->核心工作,比如编译测试打包等

site--->部署之类的方法

概念性的,插件的框架.



































