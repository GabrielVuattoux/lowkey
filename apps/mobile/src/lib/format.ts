import type { EventCategory } from '@/lib/api';

export const CATEGORY_LABELS: Record<EventCategory, string> = {
  SPORTS: '⚽️ Sports',
  MUSIC: '🎵 Music',
  FOOD: '🍕 Food',
  ART: '🎨 Art',
  SOCIAL: '💬 Social',
  OUTDOOR: '🌲 Outdoor',
  PARTY: '🎉 Party',
  OTHER: '✨ Other',
};

/**
 * The server sends local times without an offset, sometimes with nanoseconds. Keeping at most
 * milliseconds makes every JavaScript engine parse them the same way (as local time).
 */
export function parseServerTime(value: string): Date {
  return new Date(value.replace(/(\.\d{3})\d+/, '$1'));
}

const timeFormat = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
const dayFormat = new Intl.DateTimeFormat(undefined, {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
});

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

/** "Now · until 21:00", "Today · 18:00–20:00", "Tomorrow · …" or "Sat 3 Oct · …". */
export function formatWhen(start: Date, end: Date, now: Date = new Date()): string {
  if (start <= now && now < end) {
    return `Now · until ${timeFormat.format(end)}`;
  }
  const tomorrow = new Date(now);
  tomorrow.setDate(now.getDate() + 1);
  const day = isSameDay(start, now)
    ? 'Today'
    : isSameDay(start, tomorrow)
      ? 'Tomorrow'
      : dayFormat.format(start);
  return `${day} · ${timeFormat.format(start)}–${timeFormat.format(end)}`;
}

/** "< 100 m", "300 m", "1.2 km", "14 km". */
export function formatDistance(km: number): string {
  const meters = Math.round(km * 10) * 100;
  if (meters < 100) {
    return '< 100 m';
  }
  if (meters < 1000) {
    return `${meters} m`;
  }
  return km < 10 ? `${km.toFixed(1)} km` : `${Math.round(km)} km`;
}
