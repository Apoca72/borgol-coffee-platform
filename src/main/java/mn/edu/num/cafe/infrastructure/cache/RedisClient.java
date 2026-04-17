package mn.edu.num.cafe.infrastructure.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis холболтын клиент — Double-Checked Locking Singleton.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Singleton (GoF) — DatabaseConnection-тэй ижил хэв маяг
 * ════════════════════════════════════════════════════════════
 * Зорилго: Програмын туршид ганц нэг JedisPool объект байна.
 *
 * Холболтын сан (Connection Pool):
 *  - JedisPool нь Redis холболтуудыг дахин ашиглана (pool pattern)
 *  - getResource() → холболт авна, close() → буцааж өгнө
 *  - try-with-resources ашиглан автоматаар буцаана
 *
 * volatile keyword: JVM-ийн instruction reordering-ээс хамгаалана.
 * Шалтгаан: instance = new RedisClient() нь 3 алхамтай —
 *  1) санах ой зааж өгнө, 2) конструктор ажиллана, 3) лавлагаа онооно.
 * volatile байхгүй бол 1,3,2 гэсэн дарааллаар хийж болно (буруу).
 */
public class RedisClient {

    /** DCL Singleton-д volatile зайлшгүй шаардлагатай */
    private static volatile RedisClient instance;

    /** Redis холболтын сан — thread-safe */
    private final JedisPool pool;

    /**
     * Redis тохиргоог орчны хувьсагчаас уншина.
     * REDIS_HOST, REDIS_PORT, REDIS_PASSWORD орчны хувьсагчид байхгүй бол
     * localhost:6379, нууц үггүй гэж тооцно (локал/тест горим).
     */
    private RedisClient() {
        String host     = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int    port     = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        String password = System.getenv("REDIS_PASSWORD");

        JedisPoolConfig config = new JedisPoolConfig();
        // Холболтын сангийн хэмжээ — нэгэн зэрэг хамгийн ихдээ 16 холболт
        config.setMaxTotal(16);
        // Хүлээлгийн горим: хэтэрвэл exception шиддэг
        config.setBlockWhenExhausted(true);
        // Ашиглагдахгүй холболтыг шалгах — нас барсан холболтыг цэвэрлэнэ
        config.setTestOnBorrow(true);

        if (password != null && !password.isBlank()) {
            // Нууц үгтэй Redis — Railway production горим
            pool = new JedisPool(config, host, port, 2000, password);
        } else {
            // Нууц үггүй Redis — локал тест горим
            pool = new JedisPool(config, host, port, 2000);
        }

        System.out.println("  [Redis] Pool initialized → " + host + ":" + port);
    }

    /**
     * Double-Checked Locking — thread-safe Singleton хандалт.
     *
     * Эхний null шалгалт: synchronized блокт орохгүйгээр хурдан буцаана.
     * Дотоод null шалгалт: synchronized блокт орсны дараа дахин шалгана,
     * учир нь хоёр thread нэгэн зэрэг эхний null-г давж болно.
     *
     * @return RedisClient-ийн цорын ганц жишээ (singleton instance)
     */
    public static RedisClient get() {
        if (instance == null) {
            synchronized (RedisClient.class) {
                if (instance == null) {
                    instance = new RedisClient();
                }
            }
        }
        return instance;
    }

    // ── Нийтийн API методууд ─────────────────────────────────────────────────
    // try-with-resources: Jedis implements Closeable →
    //   pool.getResource() авсан холболтыг close() автоматаар буцааж өгнө

    /**
     * Redis-ээс утга унших.
     *
     * @param key  кэшийн түлхүүр
     * @return     олдсон утга, олдохгүй бол null
     */
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }

    /**
     * Redis-д утга бичих — хугацааны хязгаартай (TTL).
     * SETEX: SET + EXpire нэг командад — atomic үйлдэл.
     *
     * @param key        кэшийн түлхүүр
     * @param ttlSeconds хугацаа (секундээр)
     * @param value      хадгалах утга (JSON)
     */
    public void setex(String key, int ttlSeconds, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }

    /**
     * Redis-ээс түлхүүрийг устгах — кэш хүчингүй болгох.
     * Бичих үйлдлийн дараа кэш invalidation хийхэд ашиглана.
     *
     * @param key устгах кэшийн түлхүүр
     */
    public void del(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }

    /**
     * Холболтын сангийн нөөцийг чөлөөлнэ.
     * Программ зогсохдоо дуудаж болно.
     */
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
