package borgol.infrastructure.cache;

/**
 * Thread-local cache status tracker.
 *
 * ════════════════════════════════════════════════════════════
 * Зорилго: Service давхаргад болсон Cache Hit/Miss үр дүнг
 * Router давхаргад дамжуулна — X-Cache HTTP header-т тусгана.
 * ════════════════════════════════════════════════════════════
 *
 * Урсгал:
 *   RecipeService.cacheGet() → CacheStatus.set(HIT/MISS)
 *       → RecipeRouter.getRecipe() → ctx.header("X-Cache", CacheStatus.get())
 *
 * ThreadLocal: Javalin нь хүсэлт бүрийг өөрийн thread-д ажиллуулдаг.
 * ThreadLocal ашиглан thread-д аюулгүйгээр дамжуулна.
 */
public final class CacheStatus {

    public enum Value { HIT, MISS, NONE }

    private static final ThreadLocal<Value> STATUS = ThreadLocal.withInitial(() -> Value.NONE);

    private CacheStatus() {}

    public static void set(Value value) { STATUS.set(value); }
    public static Value get()           { return STATUS.get(); }
    public static void clear()          { STATUS.remove(); }

    /** Returns "HIT", "MISS", or null (if not set — non-cached endpoint). */
    public static String headerValue() {
        Value v = STATUS.get();
        return v == Value.NONE ? null : v.name();
    }
}
