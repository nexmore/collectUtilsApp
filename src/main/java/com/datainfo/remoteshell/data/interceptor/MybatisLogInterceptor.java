package com.datainfo.remoteshell.data.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Intercepts({
        @Signature(type= StatementHandler.class, method="update", args={Statement.class})
        , @Signature(type= StatementHandler.class, method="query", args={Statement.class, ResultHandler.class})
})
@Slf4j
public class MybatisLogInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler)invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();     // 쿼리문을 가져온다(이 상태에서의 쿼리는 값이 들어갈 부분에 ?가 있다)
        Object param = handler.getParameterHandler().getParameterObject();  // 쿼리실행시 맵핑되는 파라미터를 구한다

        if(param == null) {                // 파라미터가 아무것도 없을 경우
            sql = sql.replaceFirst("\\?", "''");
        } else {
            if(param instanceof Integer || param instanceof Long || param instanceof Float || param instanceof Double) {
                sql = sql.replaceFirst("\\?", param.toString());
            } else if(param instanceof String) {	// 해당 파라미터의 클래스가 String 일 경우(이 경우는 앞뒤에 '(홑따옴표)를 붙여야해서 별도 처리
                sql = sql.replaceFirst("\\?", "'" + param + "'");
            } else if(param instanceof Map) {        // 해당 파라미터가 Map 일 경우
                @SuppressWarnings("rawtypes")
                Map paramterObjectMap = (Map) param;

                /*
                 * 쿼리의 ?와 매핑되는 실제 값들의 정보가 들어있는 ParameterMapping 객체가 들어간 List 객체로 return이 된다.
                 * 이때 List 객체의 0번째 순서에 있는 ParameterMapping 객체가 쿼리의 첫번째 ?와 매핑이 된다
                 * 이런 식으로 쿼리의 ?과 ParameterMapping 객체들을 Mapping 한다
                 */
                List<ParameterMapping> paramMapping = boundSql.getParameterMappings();
                for (ParameterMapping parameterMapping : paramMapping) {
                    String propertyKey = parameterMapping.getProperty();
                    try {
                        Object paramValue;
                        if(boundSql.hasAdditionalParameter(propertyKey)) {
                            // 동적 SQL로 인해 __frch_item_0 같은 파라미터가 생성되어 적재됨, additionalParameter로 획득
                            paramValue = boundSql.getAdditionalParameter(propertyKey);
                        } else {
                            paramValue = paramterObjectMap.get(propertyKey);
                        }
                        if(paramValue instanceof String) {            // SQL의 ? 대신에 실제 값을 넣는다. 이때 String 일 경우는 '를 붙여야 하기땜에 별도 처리
                            sql = sql.replaceFirst("\\?", "'" + paramValue + "'");
                        } else {
                            sql = sql.replaceFirst("\\?", paramValue.toString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {        // 해당 파라미터가 사용자 정의 클래스일 경우
                /*
                 * 쿼리의 ?와 매핑되는 실제 값들이 List 객체로 return이 된다.
                 * 이때 List 객체의 0번째 순서에 있는 ParameterMapping 객체가 쿼리의 첫번째 ?와 매핑이 된다
                 * 이런 식으로 쿼리의 ?과 ParameterMapping 객체들을 Mapping 한다
                 */
                List<ParameterMapping> paramMapping = boundSql.getParameterMappings();

                Class<?> paramClass = param.getClass();
                // logger.debug("paramClass.getName() : {}", paramClass.getName());
                for (ParameterMapping parameterMapping : paramMapping) {
                    String propertyKey = parameterMapping.getProperty();
                    try {
                        Object paramValue = null;
                        if(boundSql.hasAdditionalParameter(propertyKey)) {
                            // 동적 SQL로 인해 __frch_item_0 같은 파라미터가 생성되어 적재됨, additionalParameter로 획득
                            paramValue = boundSql.getAdditionalParameter(propertyKey);
                        } else {
                            Field field = ReflectionUtils.findField(paramClass, propertyKey);
                            if (field != null) {
                                field.setAccessible(true);
                            }
                            paramValue = Objects.requireNonNull(field).get(param);
                        }

                        Class<?> javaType = parameterMapping.getJavaType();
                        if(String.class == javaType) {                // SQL의 ? 대신에 실제 값을 넣는다. 이때 String 일 경우는 '를 붙여야 하기땜에 별도 처리
                            sql = sql.replaceFirst("\\?", "'" + paramValue + "'");
                        } else {
                            sql = sql.replaceFirst("\\?", "'" + paramValue.toString() + "'");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        log.debug("=====================================================================");
        log.debug("sql : {}", sql);
        log.debug("=====================================================================");

        return invocation.proceed(); // 쿼리 실행
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
