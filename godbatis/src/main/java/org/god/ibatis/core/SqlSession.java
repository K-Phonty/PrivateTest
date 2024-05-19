package org.god.ibatis.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;

/**
 * 专门负责执行SQL语句的会话对象
 * @author Z
 * @since 1.0
 * @version 1.0
 */
public class SqlSession {
    private SqlSessionFactory factory;

    public SqlSession(SqlSessionFactory factory) {
        this.factory = factory;
    }

    /**
     * 执行insert语句，向数据库表当中插入数据
     * @param sqlId sql语句的id
     * @param pojo 插入的数据
     * @return
     */
    public int insert(String sqlId, Object pojo){
        int count = 0;
        try {
            // JDBC代码，执行insert语句，完成插入操作
            Connection connection = factory.getTransaction().getConnection();
            // insert into t_user values(#{id},#{name},#{age})
            String godbatisSql = factory.getMappedStatements().get(sqlId).getSql();
            // insert into t_user values(?,?,?)
            String sql = godbatisSql.replaceAll("#\\{[a-zA-Z0-9_$]*}","?");
            PreparedStatement ps = connection.prepareStatement(sql);
            // 给占位符传值
            // 难度是什么：
            // 第一：你不知道有多少个?
            // 第二：你不知道该将pojo对象中的哪个属性赋值给哪个?
            int fromIndex = 0;
            int index = 1;
            while (true){
                fromIndex = godbatisSql.indexOf("#",fromIndex);
                if (fromIndex == -1){
                    break;
                }
                int toIndex = godbatisSql.indexOf("}",fromIndex);
                String property = godbatisSql.substring(fromIndex + 2,toIndex);
                fromIndex = toIndex + 1;
                // 有属性名id，怎么获取id的属性值呢？调用getId()方法
                String getMethodName = "get" + property.toUpperCase().charAt(0) + property.substring(1);
                Method getMethod = pojo.getClass().getDeclaredMethod(getMethodName);
                Object propertyValue = getMethod.invoke(pojo);
                ps.setString(index, propertyValue.toString());
                index++;
            }
            count = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 执行查询语句，返回一个对象，该方法只适合返回一条记录的sql语句
     * @param sqlId
     * @param param
     * @return
     */
    public Object selectOne(String sqlId,Object param){
        Object obj = null;
        try {
            Connection connection = factory.getTransaction().getConnection();
            MappedStatement mappedStatement = factory.getMappedStatements().get(sqlId);
            // 这是那个DQL查询语句
            // select * from t_user where id = #{id}
            String godbatisSql = mappedStatement.getSql();
            String sql= godbatisSql.replaceAll("#\\{[a-zA-Z0-9_$]*}","?");
            PreparedStatement ps = connection.prepareStatement(sql);
            // 给占位符传值
            ps.setString(1, param.toString());
            // 查询返回结果集
            ResultSet rs = ps.executeQuery();
            // 要封装的结果类型
            String resultType = mappedStatement.getResultType();
            // 从结果集中取数据，封装java对象
            if (rs.next()) {
                // 获取resultType的Class
                Class<?> resultTypeClass = Class.forName(resultType);
                // 调用无参构造方法创建对象
                obj = resultTypeClass.newInstance();  // Object obj = new User();
                // 给User类的id，name，age属性赋值
                // 给obj对象的哪个属性赋哪个值
                /*
                +------+----------+------+
                | id   | name     | age  |
                +------+----------+------+
                | 1111 | zhangsan | 20   |
                +------+----------+------+
                解决问题的关键：将查询结果的列名作为属性名
                列名是id，那么属性名就是：id
                列名是name，那么属性名就是：name
                 */
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    String propertyName = rsmd.getColumnName(i + 1);
                    // 拼接方法名
                    String setMethodName = "set" + propertyName.toUpperCase().charAt(0) + propertyName.substring(1);
                    // 获取set方法
                    Method setMethod = resultTypeClass.getDeclaredMethod(setMethodName, String.class);
                    // 调用set方法给对象obj属性赋值
                    setMethod.invoke(obj, rs.getString(propertyName));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return obj;
    }

    // 局部测试
    public static void main(String[] args) {
        String sql = "insert into t_user values(#{id},#{name},#{age})";
        int fromIndex = 0;
        int index = 1;
        while (true){
            fromIndex = sql.indexOf("#",fromIndex);
            if (fromIndex == -1){
                break;
            }
            System.out.println(index);
            index++;
            int toIndex = sql.indexOf("}",fromIndex);
            String property = sql.substring(fromIndex + 2,toIndex);
            System.out.println(property);
            fromIndex++;
        }
    }

    /**
     * 提交事务
     */
    public void commit(){
        factory.getTransaction().commit();
    }

    /**
     * 回滚事务
     */
    public void rollback(){
        factory.getTransaction().rollback();
    }

    /**
     * 关闭事务
     */
    public void close(){
        factory.getTransaction().close();
    }
}
