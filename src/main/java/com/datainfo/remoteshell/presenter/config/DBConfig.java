package com.datainfo.remoteshell.presenter.config;

import com.datainfo.remoteshell.data.interceptor.MybatisLogInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(
    value = "com.datainfo.remoteshell.data.repository",
    sqlSessionFactoryRef = "sqlSessionFactory"
)
public class DBConfig {

    @Bean
    @ConfigurationProperties("mariadb.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setTypeAliasesPackage("com.datainfo.remoteshell.data.entity");
        sessionFactory.setPlugins(new MybatisLogInterceptor());
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(
                        "classpath*:mapper/mariadb/*.xml"
                )
        );
        return sessionFactory;
    }

    @Bean
    public SqlSessionTemplate sessionTemplate(SqlSessionFactory sqlSessionFactory) {
        sqlSessionFactory.getConfiguration().setMapUnderscoreToCamelCase(true);
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
