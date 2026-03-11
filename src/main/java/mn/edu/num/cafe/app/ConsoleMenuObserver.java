package mn.edu.num.cafe.app;

import mn.edu.num.cafe.core.domain.MenuItem;
import mn.edu.num.cafe.core.ports.MenuChangeObserver;

/**
 * Observer pattern хэрэгжүүлэлт — консолд мэдэгдэл хэвлэнэ.
 *
 * Загвар: Observer (GoF) — MenuService-ийн MenuChangeObserver портыг хэрэгжүүлнэ.
 *
 * Ирээдүйд: EmailMenuObserver, LogFileMenuObserver гэх мэт хэрэгжүүлэлтүүд
 * нэмж болно — MenuService-г өөрчлөхгүйгээр.
 */
public class ConsoleMenuObserver implements MenuChangeObserver {

    @Override
    public void onItemAdded(MenuItem item) {
        System.out.printf("  [OBSERVER] ✓ Added  : %s%n", item);
    }

    @Override
    public void onItemUpdated(MenuItem item) {
        System.out.printf("  [OBSERVER] ✎ Updated: %s%n", item);
    }

    @Override
    public void onItemDeleted(int id) {
        System.out.printf("  [OBSERVER] ✗ Deleted: id=%d%n", id);
    }
}
