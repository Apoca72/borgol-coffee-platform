package borgol.core.ports;

import borgol.core.domain.MenuItem;

// ═══════════════════════════════════════════════════════════════
// OBSERVER PATTERN — OBSERVER INTERFACE (сонсогчийн гэрээ)
//
// Энэ interface Observer-н гэрээг тодорхойлно.
// Хэн ч энэ interface-г хэрэгжүүлбэл цэсийн өөрчлөлтийг сонсож чадна.
// MenuService зөвхөн энэ interface-г мэднэ — конкрет классыг мэдэхгүй.
// ═══════════════════════════════════════════════════════════════
public interface MenuChangeObserver {

    // [OBSERVER INTERFACE] Шинэ зүйл нэмэгдсэн үед Subject дуудна
    void onItemAdded(MenuItem item);

    // [OBSERVER INTERFACE] Зүйл шинэчлэгдсэн үед Subject дуудна
    void onItemUpdated(MenuItem item);

    // [OBSERVER INTERFACE] Зүйл устгагдсан үед Subject дуудна
    void onItemDeleted(int id);
}
