package borgol.infrastructure.cache;

/**
 * Кэшийн түлхүүр үүсгэгч — статик хэрэгслийн класс.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Utility / Helper class — бүх методууд статик
 * ════════════════════════════════════════════════════════════
 * Зорилго: Borgol платформын кэш түлхүүрүүдийг нэг газар тодорхойлно.
 * Ингэснээр код бүрт бичлэгийг давтахгүй (DRY зарчим).
 *
 * Түлхүүрийн схем: borgol:{нөөц}:{танигч}
 * Жишээ:
 *   borgol:recipe:42
 *   borgol:user:7
 *   borgol:feed:userId:3
 *   borgol:cafes:nearby:47.9184:106.9177
 *
 * Зарчим: Namespace prefix (borgol:) ашиглах нь Redis дотор
 * бусад программын түлхүүртэй зөрчилдөхөөс сэргийлнэ.
 */
public final class CacheKeyBuilder {

    /** Статик хэрэгслийн класс — жишээ үүсгэхийг хориглоно */
    private CacheKeyBuilder() {}

    /** Кэш түлхүүрийн үндсэн угтвар — namespace тусгаарлагч */
    private static final String PREFIX = "borgol:";

    /**
     * Жорын кэш түлхүүр үүсгэнэ.
     * TTL: 300 секунд (5 минут)
     *
     * @param id жорын өвөрмөц танигч (recipe ID)
     * @return   жишээ: "borgol:recipe:42"
     */
    public static String forRecipe(int id) {
        return PREFIX + "recipe:" + id;
    }

    /**
     * Хэрэглэгчийн кэш түлхүүр үүсгэнэ.
     * TTL: 600 секунд (10 минут)
     *
     * @param id хэрэглэгчийн өвөрмөц танигч (user ID)
     * @return   жишээ: "borgol:user:7"
     */
    public static String forUser(int id) {
        return PREFIX + "user:" + id;
    }

    /**
     * Feed-ийн кэш түлхүүр үүсгэнэ.
     * TTL: 60 секунд (1 минут) — feed хурдан хуучирдаг тул богино TTL
     *
     * @param userId хэрэглэгчийн ID (feed нь хэрэглэгч бүрт өөр)
     * @return       жишээ: "borgol:feed:userId:3"
     */
    public static String forFeed(int userId) {
        return PREFIX + "feed:userId:" + userId;
    }

    /**
     * Ойролцоох кафены кэш түлхүүр үүсгэнэ.
     * TTL: 120 секунд (2 минут)
     *
     * Өргөрөг (lat) болон уртрагийн (lng) 4 аравтын нарийвчлал:
     *   ~11 метрийн нарийвчлал — хотын хэмжээнд хангалттай.
     *
     * @param lat өргөрөг (latitude)
     * @param lng уртраг (longitude)
     * @return    жишээ: "borgol:cafes:nearby:47.9184:106.9177"
     */
    public static String forCafesNearby(double lat, double lng) {
        return PREFIX + "cafes:nearby:"
                + String.format("%.4f", lat) + ":"
                + String.format("%.4f", lng);
    }

    /**
     * SOAP token баталгаажуулалтын кэш түлхүүр.
     * Бодит token-г хадгалахгүй — hashCode()-г ашиглана (богино, аюулгүй).
     * TTL: 300 секунд (5 минут)
     *
     * @param tokenHash Integer.toHexString(token.hashCode())-ийн утга
     * @return жишээ: "borgol:soap:token:1a2b3c4d"
     */
    public static String forSoapToken(String tokenHash) {
        return PREFIX + "soap:token:" + tokenHash;
    }

    /**
     * Ойролцоох кафены кэшийн бүх түлхүүрийг олох хэв загвар.
     * RedisClient.deleteByPattern()-д шилжүүлнэ.
     */
    public static final String NEARBY_PATTERN = PREFIX + "cafes:nearby:*";

    /**
     * Gateway proxy кэшийн бүх түлхүүрийг олох хэв загвар.
     */
    public static final String GATEWAY_CACHE_PATTERN = PREFIX + "gw:*";
}
