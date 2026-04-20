package mn.edu.num.cafe.infrastructure.messaging;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Redis Pub/Sub үйл явдлын шину — мэдэгдлийг бодит цагт түгээнэ.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Event Bus / Observer (GoF) + Redis Pub/Sub
 * ════════════════════════════════════════════════════════════
 * Зорилго: BorgolService мэдэгдэл үүсгэхдээ Redis channel-д нийтэлнэ.
 * SSE endpoint тэр channel-ийг сонсон, хэрэглэгч рүү шилжүүлнэ.
 *
 * Урсгал:
 *   BorgolService.followUser()
 *       → eventBus.publish(userId, "follow", message)
 *           → Redis PUBLISH borgol:notify:{userId} {json}
 *               → globalSubscriber.onPMessage()
 *                   → SSE handler → browser
 *
 * Зарчим: Single Subscriber Thread — нэг PSUBSCRIBE thread бүх
 * channel-г сонсоно. SSE холболт бүрт Redis холболт нэмэхгүй.
 *
 * Resilience: Redis алдаа гарвал log бичиж алгасна (silent degradation).
 * Хэрэглэгч DB-ийн мэдэгдлийг polling-оор авах боломжтой хэвээр байна.
 */
public class RedisEventBus {

    private static final Logger log = LoggerFactory.getLogger(RedisEventBus.class);
    private static final String CHANNEL_PREFIX = "borgol:notify:";
    private static final Gson GSON = new Gson();

    private final JedisPool pool;

    /**
     * userId → SSE writer-уудын жагсаалт.
     * Хэрэглэгч нэгэн зэрэг хэд хэдэн tab нээгээд байж болно.
     * CopyOnWriteArrayList → thread-safe iteration + rare modification.
     */
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<Consumer<String>>> listeners =
            new ConcurrentHashMap<>();

    public RedisEventBus(JedisPool pool) {
        this.pool = pool;
    }

    // ── Нийтлэх (Publish) ────────────────────────────────────────────────────

    /**
     * Redis channel-д мэдэгдэл нийтэлнэ.
     * BorgolService-с дуудагдана — follow/like/comment үйлдлийн дараа.
     *
     * @param userId  мэдэгдэл хүлээн авах хэрэглэгчийн ID
     * @param type    мэдэгдлийн төрөл: "follow" | "like" | "comment"
     * @param message хэрэглэгчид харуулах текст
     */
    public void publish(int userId, String type, String message) {
        String payload = GSON.toJson(Map.of(
                "type",    type,
                "message", message,
                "ts",      Instant.now().toEpochMilli()
        ));
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(CHANNEL_PREFIX + userId, payload);
            log.debug("[EventBus] Published {} → user {}", type, userId);
        } catch (Exception e) {
            log.debug("[EventBus] Publish failed (user {}): {}", userId, e.getMessage());
        }
    }

    // ── Дэлхийн захиалагч нэвтрэх (Global Subscriber) ────────────────────────

    /**
     * Нэг virtual thread-д PSUBSCRIBE "borgol:notify:*" ажиллуулна.
     * Бүх channel-ийн мессежийг барьж, SSE handler руу дамжуулна.
     *
     * Java 21 virtual threads: platform thread-г хаахгүйгээр блоклоно.
     * Main.java-с нэг удаа дуудна.
     */
    public void startSubscriber() {
        Thread.ofVirtual().name("redis-pubsub-subscriber").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = pool.getResource()) {
                    log.info("[EventBus] Global subscriber started — pattern: {}*", CHANNEL_PREFIX);
                    jedis.psubscribe(new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            // channel = "borgol:notify:{userId}"
                            try {
                                int userId = Integer.parseInt(
                                        channel.substring(CHANNEL_PREFIX.length()));
                                fanOut(userId, message);
                            } catch (Exception e) {
                                log.warn("[EventBus] onPMessage parse error: {}", e.getMessage());
                            }
                        }
                    }, CHANNEL_PREFIX + "*");
                } catch (Exception e) {
                    log.warn("[EventBus] Subscriber disconnected, retrying in 3s: {}", e.getMessage());
                    try { Thread.sleep(3_000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    // ── SSE Handler бүртгэх / хасах ──────────────────────────────────────────

    /**
     * SSE холболт нээгдэхэд дуудагдана.
     * Consumer<String> нь SSE response writer руу бичинэ.
     */
    public void subscribe(int userId, Consumer<String> handler) {
        listeners.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.debug("[EventBus] SSE subscribed for user {}", userId);
    }

    /**
     * SSE холболт хаагдахад дуудагдана — cleanup.
     */
    public void unsubscribe(int userId, Consumer<String> handler) {
        List<Consumer<String>> list = listeners.get(userId);
        if (list != null) {
            list.remove(handler);
            if (list.isEmpty()) listeners.remove(userId);
        }
        log.debug("[EventBus] SSE unsubscribed for user {}", userId);
    }

    // ── Дотоод туслах ────────────────────────────────────────────────────────

    private void fanOut(int userId, String message) {
        CopyOnWriteArrayList<Consumer<String>> handlers = listeners.get(userId);
        if (handlers == null || handlers.isEmpty()) return;
        for (Consumer<String> h : handlers) {
            try {
                h.accept(message);
            } catch (Exception e) {
                log.debug("[EventBus] SSE write failed (user {}): {}", userId, e.getMessage());
            }
        }
    }
}
