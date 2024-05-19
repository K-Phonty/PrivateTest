package org.god.ibatis.core.dataSource.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionPool {

    // 数据源配置
    private DataSourceConfig dataSourceConfig = new DataSourceConfig().getInstance();

    // 空闲连接池
    private List<Connection> idlePool = new CopyOnWriteArrayList<>();

    // 工作连接池
    private List<Connection> workPool = new CopyOnWriteArrayList<>();

    public ConnectionPool() {
        init();
    }

    /**
     * 初始化连接池
     */
    public void init(){
        try {
            // 注册驱动
            Class.forName(dataSourceConfig.getDriver());
            for (int i = 0; i < 5; i++) {
                // 创建连接对象放入空闲连接池中
                idlePool.add(createConn(dataSourceConfig));
                System.out.println("init idlePool的大小：" + idlePool.size());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建连接对象
     * @param config 数据源配置
     * @return 连接对象
     */
    public Connection createConn(DataSourceConfig config) throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * 获取连接对象
     * @return 连接对象
     */
    public Connection getConnection(){
        Connection connection = null;
        try {
            // 判断空闲连接池是否为空
            if (!idlePool.isEmpty()) {
                // 不为空，从空闲连接池取出连接并放入工作连接池
                connection = idlePool.remove(0);
                workPool.add(connection);
                System.out.println("getConnection idlePool的大小：" + idlePool.size() + "，workPool的大小：" + workPool.size());
            }else {
                // 为空，创建新连接
                connection = createConn(dataSourceConfig);
                // 放入工作连接池
                workPool.add(connection);
                System.out.println("getConnection idlePool的大小：" + idlePool.size() + "，workPool的大小：" + workPool.size());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * 将连接对象从工作连接池放回到空闲连接池
     * @param connection 连接对象
     */
    public void close(Connection connection){
        for (Connection conn : workPool) {
            if (connection.equals(conn)) {
                workPool.remove(conn);
                idlePool.add(conn);
                System.out.println("close idlePool的大小：" + idlePool.size() + "，workPool的大小：" + workPool.size());
            }
        }
    }
}
