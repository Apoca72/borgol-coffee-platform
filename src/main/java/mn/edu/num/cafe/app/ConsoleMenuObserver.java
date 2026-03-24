package mn.edu.num.cafe.app;

import mn.edu.num.cafe.core.domain.MenuItem;
import mn.edu.num.cafe.core.ports.MenuChangeObserver;

// ═══════════════════════════════════════════════════════════════
// OBSERVER PATTERN — CONCRETE OBSERVER (конкрет сонсогч)
//
// MenuChangeObserver interface-г хэрэгжүүлсэн конкрет класс.
// Subject (MenuService)-аас мэдэгдэл ирэхэд яг юу хийхийг энд тодорхойлно.
// Одоогийн хэрэгжүүлэлт: консолд хэвлэнэ.
// Ирээдүйд MenuService-г өөрчлөхгүйгээр EmailMenuObserver,
// LogFileMenuObserver гэх мэт шинэ observer нэмж болно.
// ═══════════════════════════════════════════════════════════════
public class ConsoleMenuObserver implements MenuChangeObserver {

    // [CONCRETE OBSERVER] Subject onItemAdded() дуудахад энэ ажиллана
    @Override
    public void onItemAdded(MenuItem item) {
        System.out.printf("  [OBSERVER] ✓ Added  : %s%n", item);
    }

    // [CONCRETE OBSERVER] Subject onItemUpdated() дуудахад энэ ажиллана
    @Override
    public void onItemUpdated(MenuItem item) {
        System.out.printf("  [OBSERVER] ✎ Updated: %s%n", item);
    }

    // [CONCRETE OBSERVER] Subject onItemDeleted() дуудахад энэ ажиллана
    @Override
    public void onItemDeleted(int id) {
        System.out.printf("  [OBSERVER] ✗ Deleted: id=%d%n", id);
    }
}
