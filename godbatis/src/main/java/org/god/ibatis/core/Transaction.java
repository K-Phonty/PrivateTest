package org.god.ibatis.core;

import java.sql.Connection;

/**
 * 事务管理器接口
 * 所有的事务管理器都应该遵循该规范
 * JDBC事务管理器，MANAGED事务管理器都应该实现这个接口
 * Transaction事务管理器：提供管理事务方法
 * @author Z
 * @since 1.0
 * @version 1.0
 */
public interface Transaction {
    /**
     * 提交事务
     */
    void commit();

    /**
     * 回滚事务
     */
    void rollback();

    /**
     * 关闭事务
     */
    void close();

    /**
     * 还需要其他方法吗？后续再说
     */

    /**
     * 真正的开启数据库连接
     */
    void openConnection();

    /**
     * 获取数据库链接对象的
     * @return
     */
    Connection getConnection();
}
