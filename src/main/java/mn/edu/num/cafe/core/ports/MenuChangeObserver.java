package mn.edu.num.cafe.core.ports;

import mn.edu.num.cafe.core.domain.MenuItem;

/**
 * Observer pattern — цэс өөрчлөгдөх үед мэдэгдэх интерфейс (Outbound Port).
 *
 * Загвар: Observer (GoF) — MenuService нь энэ портоор бүртгэгдсэн
 *         observer-уудад мэдэгдэл илгээнэ.
 *
 * Хэрэгжүүлэлт: ConsoleMenuObserver (app/) — консолд хэвлэнэ.
 */
public interface MenuChangeObserver {

    /** Шинэ зүйл нэмэгдсэн үед дуудагдана. */
    void onItemAdded(MenuItem item);

    /** Зүйл шинэчлэгдсэн үед дуудагдана. */
    void onItemUpdated(MenuItem item);

    /** Зүйл устгагдсан үед дуудагдана. */
    void onItemDeleted(int id);
}
