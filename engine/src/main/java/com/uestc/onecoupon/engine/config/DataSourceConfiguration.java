package com.uestc.onecoupon.engine.config;

import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DataSourceConfiguration {

    // 配置分页插件 PageInterceptor
    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql"); // 使用MySQL方言
        properties.setProperty("reasonable", "true"); // 分页合理化
        pageInterceptor.setProperties(properties);
        return pageInterceptor;
    }

    // 配置 SqlSessionFactory，添加分页插件
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 设置分页插件 PageInterceptor
        factoryBean.setPlugins(new Interceptor[]{pageInterceptor()});

        // 设置 mapper.xml 文件路径
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mybatis/mapper/*.xml"));

        // 设置 mybatis-config.xml 文件路径
        factoryBean.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:/mybatis/config/mybatis-config.xml"));

        return factoryBean.getObject();
    }
}
