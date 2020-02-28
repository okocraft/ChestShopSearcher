package net.okocraft.chestshopsearcher.database;

import java.util.List;
import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.hibernate.cfg.Configuration;

import net.okocraft.chestshopsearcher.Main;
import net.okocraft.chestshopsearcher.config.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * トランザクションの開始とコミット、EntityManagerのクローズを組み込んだEntityManagerのデコレータのようなクラス。
 */
class DatabaseManager<T> {

    private Class<T> type;
    private EntityManagerFactory emf;

    /**
     * エンティティの型を指定して生成する<br>
     * 永続性ユニット名は default_pu になる （例） Databasemanager dm = new
     * DatabaseManager(Employee.class);
     *
     * @param type {@code @Entity}注釈を持つエンティティの型
     * 
     * @throws IllegalArgumentException {@code type}が{@code @Entity}注釈を持たないとき
     */
    public DatabaseManager(@NotNull Class<T> type) throws IllegalArgumentException {
        if (!type.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("The type " + type.getName() + " do not have annotation " + Entity.class.getName());
        }

        this.type = type;
        this.emf = new Configuration()
                .addAnnotatedClass(type)
                .addProperties(getProperties())
                .buildSessionFactory();

        createTable();

        // NOTE: BukkitプラグインではプラグインからJPAのpersistence.xmlを指定できないため、HibernateからEntityManagerFactoryを生成している。
        // emf = Persistence.createEntityManagerFactory("default_pu", getProperties());
    }

    /**
     * 設定を参照し、使うDBに応じたプロパティを返す。
     * 
     * @return 使うDBに応じたプロパティ
     */
    @NotNull
    private Properties getProperties() {
        Properties properties = new Properties();
        
        Config config = Config.getInstance();
        
        String dbName = config.getDatabaseName();
        if (config.isUsingMySQL()) {
            String host = config.getMySQLHost();
            int port = config.getMySQLPort();
            String user = config.getMySQLUser();
            String password = config.getMySQLPassword();
            properties.put("hibernate.connection.username", user);
            properties.put("hibernate.connection.password", password);
            properties.put("hibernate.connection.url", "jdbc:mysql://" + host + ":" + port + "/" + dbName);
            properties.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
            properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        } else {
            // MySQLが使えない場合はSQLiteを使う
            properties.put("hibernate.connection.url", "jdbc:sqlite:" + Main.getInstance().getDataFolder().toPath().resolve(dbName + ".db").toString());
            properties.put("hibernate.connection.driver_class", "org.sqlite.JDBC");
            properties.put("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect");
        }

        properties.put("hibernate.show_sql", String.valueOf(config.isSQLLoggingEnabled()));
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.hbm2ddl.auto", "none");
        return properties;
    }

    private void createTable() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            String createTable = "create table if not exists shops (location varchar(100) not null, buy_price varchar(255) not null, item varchar(255) not null, owner varchar(36) not null, quantity integer not null, sell_price varchar(255) not null, stock integer not null, primary key (location))";
            em.createNativeQuery(createTable).executeUpdate();    

            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * エンティティを新規に保存する
     *
     * @param emp 保存するエンティティ
     * @return データベースマネージャーの参照
     */
    @NotNull
    public DatabaseManager<T> persist(T emp) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            em.persist(emp);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em != null) {
                em.close();
            }
        }
        return this;

    }

    /**
     * エンティティを更新して保存する
     *
     * @param emp 保存するエンティティ
     * @return データベースマネージャーの参照
     */
    @NotNull
    public DatabaseManager<T> merge(T emp) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            em.merge(emp);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em != null) {
                em.close();
            }
        }
        return this;
    }

    /**
     * 主キーを指定してエンティティを削除する
     *
     * @param key 主キーをセットしたエンティティ
     */
    public void remove(Object key) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            T obj = em.find(type, key);
            em.remove(obj);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * 主キーを指定してエンティティを検索する
     *
     * @param key 主キーをセットしたエンティティ
     * @return 検索結果のエンティティ。見つからなければnullを返す
     */
    @Nullable
    public T find(Object key) {
        EntityManager em = null;
        T obj = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            obj = em.find(type, key);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (em != null) {
                em.close();
            }
        }
        return obj;
    }

    /**
     * 全件取得
     *
     * @return 全エンティティのリスト
     */
    @Nullable
    public List<T> getAll() {
        return select("SELECT c FROM " + type.getSimpleName() + " c");
    }

    /**
     * 指定したフィールドの昇順に並び替えて全件取得
     *
     * @param orderItem 並び順のキーにするフィールド名
     * @return 全エンティティのリスト
     */
    @Nullable
    public List<T> getAll(String orderItem) {
        return select("SELECT c FROM " + type.getSimpleName() + " c ORDER BY c." + orderItem);
    }

    /**
     * 指定したフィールドの昇順に並び替えて全件取得。 descにtrueを指定すると降順になる
     *
     * @param orderItem 並び順のキーにするフィールド名
     * @param desc      降順にするかどうか
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> getAll(String orderItem, boolean desc) {
        return getAll(orderItem + (desc ? " desc" : " asc"));
    }

    /**
     * 開始位置（from）と最大取得件数（max)を指定して取得する
     *
     * @param from 開始位置（0オリジン）
     * @param max  最大取得する件数
     * @return 取得したエンティティのリスト
     */
    @Nullable
    public List<T> get(int from, int max) {
        return select("SELECT c FROM " + type.getSimpleName() + " c", from, max);
    }

    /**
     * 開始位置（from）と最大取得件数（max)、および並び替えのキーにする項目を指定して取得する
     *
     * @param from      開始位置（0オリジン）
     * @param max       最大取得する件数
     * @param orderItem 並び替えのキーにするフィールド名
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> get(int from, int max, String orderItem) {
        String queryString = "SELECT c FROM " + type.getSimpleName() + " c ORDER BY c." + orderItem;
        return select(queryString, from, max);
    }

    /**
     * 開始位置（from）と最大取得件数（max)、および並び替えのキーにする項目を指定して取得する descにtrueを指定すると降順になる
     *
     * @param from      開始位置（0オリジン）
     * @param max       最大取得する件数
     * @param orderItem 並び替えのキーにするフィールド名
     * @param desc      降順にするかどうか
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> get(int from, int max, String orderItem, boolean desc) {
        return get(from, max, orderItem + (desc ? " desc" : " asc"));

    }

    /**
     * クエリーを実行してエンティティをのリストを得る
     *
     * @param queryString JPQLで書いたクエリ
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> select(String queryString) {
        return select(queryString, new Object[0]);
    }

    /**
     * パラメータ付きのクエリーを指定してエンティティのリストを得る
     *
     * @param queryString パラメータ付きクエリ
     * @param param       パラメータにセットするオブジェクトの配列
     * @return エンティティのリスト
     */
    @Nullable
    public List<T> select(String queryString, @NotNull Object... param) {

        EntityManager em = null;
        List<T> ls = null;

        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            TypedQuery<T> q = em.createQuery(queryString, type);
            for (int i = 0; i < param.length; i++) {
                q.setParameter(i + 1, param[i]);
            }
            ls = q.getResultList();

            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Query=" + queryString);

        } finally {
            if (em != null) {
                em.close();
            }
        }

        return ls;

    }

    /**
     * クエリーを指定してfrom件目から最大max件取得する(並び順はクエリーの中に指定)
     *
     * @param queryString JPQLで書いたクエリ
     * @param from        開始位置（0オリジン）
     * @param max         最大取得する件数
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> select(String queryString, int from, int max) {
        return select(queryString, from, max, new Object[0]);
    }

    /**
     * パラメータ付きのクエリーを指定してfrom件目から最大max件取得する(並び順はクエリーの中に指定)
     *
     * @param queryString JPQLで書いたクエリ
     * @param from        開始位置（0オリジン）
     * @param max         最大取得する件数
     * @param param       パラメータにセットするオブジェクトの配列
     * @return 検索結果のリスト
     */
    @Nullable
    public List<T> select(String queryString, int from, int max, @NotNull Object... param) {

        List<T> ls = null;

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            TypedQuery<T> q = em.createQuery(queryString, type);
            for (int i = 0; i < param.length; i++) {
                q.setParameter(i + 1, param[i]);
            }
            ls = q.setFirstResult(from).setMaxResults(max).getResultList();

            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Query=" + queryString);

        } finally {
            if (em != null) {
                em.close();
            }
        }

        return ls;

    }

    /**
     * 保存されている全エンティティの件数を取得する
     *
     * @return 全件数
     */
    public int count() {

        String queryString = "SELECT count(c) FROM " + type.getSimpleName() + " c";
        List<Object> ls = null;
        EntityManager em = null;

        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            ls = em.createQuery(queryString, Object.class).getResultList();

            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Query=" + queryString);

        } finally {
            if (em != null) {
                em.close();
            }
        }
        if (ls == null) {
            return 0;
        }
        Object obj = ls.get(0);
        return Integer.valueOf(obj.toString());

    }

    /**
     * データベースマネージャーを閉じる<br>
     * プログラムの最後に実行する<br>
     * 毎回は不要
     */
    public void close() {
        emf.close();
    }

}
