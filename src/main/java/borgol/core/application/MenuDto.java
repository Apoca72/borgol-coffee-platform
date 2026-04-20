package borgol.core.application;

import borgol.core.domain.MenuCategory;

/**
 * Цэсний зүйлийн Data Transfer Object (Java Record).
 *
 * Record нь immutable бөгөөд accessor методуудыг автоматаар үүсгэнэ:
 * dto.id(), dto.name(), dto.category(), dto.price(), dto.available()
 *
 * Архитектурын дүрэм: UI нь domain объектыг шууд хүлээж авахгүй,
 * зөвхөн DTO-г ашиглана.
 */
public record MenuDto(int id, String name, MenuCategory category, double price, boolean available) {}
