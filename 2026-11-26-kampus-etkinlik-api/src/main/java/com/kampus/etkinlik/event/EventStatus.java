package com.kampus.etkinlik.event;

/** DRAFT → PUBLISHED → (isteğe bağlı) CANCELLED. Sadece PUBLISHED etkinliğe kayıt olunur. */
public enum EventStatus {
  DRAFT,
  PUBLISHED,
  CANCELLED
}
