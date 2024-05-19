package org.god.ibatis.core;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.god.ibatis.core.dataSource.JNDIDataSource;
import org.god.ibatis.core.dataSource.PooledDataSource;
import org.god.ibatis.core.dataSource.UnPooledDataSource;
import org.god.ibatis.core.transaction.JdbcTransaction;
import org.god.ibatis.core.transaction.ManagedTransaction;
import org.god.ibatis.utils.Resources;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlSessionFactory构建对象
 * 通过SqlSessionFactoryBuilder的build方法来解析
 * godbatis-config.xml文件，然后创建SqlSessionFactory对象
 * @aythor Z
 * @since 1.0
 * @version 1.0
 */
public class SqlSessionFactoryBuilder {

    /**
     * 无参构造方法
     */
    public SqlSessionFactoryBuilder(){}

    /**
     * 解析godbatis-config.xml文件，来构建SqlSessionFactory对象
     * @param in 指向godbatis-config.xml文件的一个输入流
     * @return SqlSessionFactory对象
     */
    public SqlSessionFactory build(InputStream in){
        SqlSessionFactory factory = null;
        try {
            // 解析godbatis-config.xml文件
            SAXReader reader = new SAXReader();
            Document document = reader.read(in);
            Element element = (Element) document.selectSingleNode("/configuration/environments");
            String defaultId = element.attributeValue("default");
            Element environment = (Element) document.selectSingleNode("/configuration/environments/environment[@id='" + defaultId + "']");
            Element transactionElt = environment.element("transactionManager");
            Element dataSourceElt = environment.element("dataSource");
            List<String> sqlMapperXMLPathList = new ArrayList<>();
            List<Node> nodes = document.selectNodes("//mapper");  // 获取整个配置文件中所有的mapper标签
            nodes.forEach(node ->{
                Element mapper = (Element) node;
                String resource = mapper.attributeValue("resource");
                sqlMapperXMLPathList.add(resource);
            });

            // 获取数据源对象
            DataSource dataSource = getDataSource(dataSourceElt);
            // 获取事务管理器对象
            Transaction transaction = getTransaction(transactionElt,dataSource);
            // 获取mappedStatements
            Map<String, MappedStatement> mappedStatements = getMappedStatements(sqlMapperXMLPathList);
            // 解析完成之后，构建SqlSessionFactory对象
            factory = new SqlSessionFactory(transaction, mappedStatements);
        }catch (Exception e){
            e.printStackTrace();
        }
        return factory;
    }

    /**
     * 解析所有的SqlMapper.xml文件，然后构建Map集合
     * @param sqlMapperXMLPathList
     * @return
     */
    private Map<String, MappedStatement> getMappedStatements(List<String> sqlMapperXMLPathList) {
        Map<String,MappedStatement> mappedStatements = new HashMap<>();
        sqlMapperXMLPathList.forEach(sqlMapperXMLPath -> {
            try {
                SAXReader reader = new SAXReader();
                Document document = reader.read(Resources.getResourceAsStream(sqlMapperXMLPath));
                Element mapper = (Element) document.selectSingleNode("mapper");
                String namespace = mapper.attributeValue("namespace");
                List<Element> elements = mapper.elements();
                elements.forEach(element -> {
                    String id = element.attributeValue("id");
                    // 这里进行了namespace和id的拼接，生成最终的sqlId
                    String sqlId = namespace + "." + id;

                    String resultType = element.attributeValue("resultType");
                    String sql = element.getTextTrim();
                    MappedStatement mappedStatement = new MappedStatement(sql, resultType);

                    mappedStatements.put(sqlId, mappedStatement);
                });
            } catch (Exception e){
                e.printStackTrace();
            }
        });
        return mappedStatements;
    }

    /**
     * 获取数据源对象
     * @param dataSourceElt 数据源标签元素
     * @return
     */
    private DataSource getDataSource(Element dataSourceElt) {
        Map<String, String> map = new HashMap<>();
        // 获取所有的property
        List<Element> propertyElts = dataSourceElt.elements("property");
        propertyElts.forEach(propertyElt -> {
            String name = propertyElt.attributeValue("name");
            String value = propertyElt.attributeValue("value");
            map.put(name,value);
        });

        DataSource dataSource = null;
        String type = dataSourceElt.attributeValue("type").trim().toUpperCase();
        switch (type){
            case Const.UN_POOLED_DATASOURCE:
                dataSource = new UnPooledDataSource(map.get("driver"),map.get("url"),map.get("username"),map.get("password"));
                break;
            case Const.POOLED_DATASOURCE:
                dataSource = new PooledDataSource();
                break;
            case Const.JNDI_DATASOURCE:
                dataSource = new JNDIDataSource();
                break;
        }
        return dataSource;
    }

    /**
     * 获取事务管理器的
     * @param transactionElt 事务管理器标签元素
     * @param dataSource 数据源对象
     * @return
     */
    private Transaction getTransaction(Element transactionElt, DataSource dataSource) {
        Transaction transaction = null;
        String type = transactionElt.attributeValue("type").trim().toUpperCase();
        switch (type){
            case Const.JDBC_TRANSACTION:
                transaction = new JdbcTransaction(dataSource,false);  // 默认是开启事务的，将来需要手动提交的
                break;
            case Const.MANAGED_TRANSACTION:
                transaction = new ManagedTransaction();
                break;
        }
        return transaction;
    }
}
