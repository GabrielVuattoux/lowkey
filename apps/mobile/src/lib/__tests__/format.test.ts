import { formatDistance, formatWhen, parseServerTime } from '@/lib/format';

describe('parseServerTime', () => {
  it('parses server local times, with or without nanoseconds', () => {
    const plain = parseServerTime('2026-10-01T19:00:00');
    const withNanos = parseServerTime('2026-10-01T19:00:00.123456789');

    expect(plain.getHours()).toBe(19);
    expect(plain.getDate()).toBe(1);
    expect(withNanos.getTime() - plain.getTime()).toBe(123);
  });
});

describe('formatWhen', () => {
  const now = new Date(2026, 9, 1, 12, 0);

  it('says "Now" for an event in progress', () => {
    expect(formatWhen(new Date(2026, 9, 1, 11, 0), new Date(2026, 9, 1, 14, 0), now)).toMatch(
      /^Now · until /
    );
  });

  it('says "Today" and "Tomorrow" for the next two days', () => {
    expect(formatWhen(new Date(2026, 9, 1, 18, 0), new Date(2026, 9, 1, 20, 0), now)).toMatch(
      /^Today · /
    );
    expect(formatWhen(new Date(2026, 9, 2, 18, 0), new Date(2026, 9, 2, 20, 0), now)).toMatch(
      /^Tomorrow · /
    );
  });

  it('uses the date for later events', () => {
    expect(
      formatWhen(new Date(2026, 9, 5, 18, 0), new Date(2026, 9, 5, 20, 0), now)
    ).not.toMatch(/^(Now|Today|Tomorrow)/);
  });
});

describe('formatDistance', () => {
  it('rounds short distances to 100 m', () => {
    expect(formatDistance(0.03)).toBe('< 100 m');
    expect(formatDistance(0.26)).toBe('300 m');
  });

  it('switches to kilometres from 1 km', () => {
    expect(formatDistance(0.96)).toBe('1.0 km');
    expect(formatDistance(4.26)).toBe('4.3 km');
    expect(formatDistance(14.6)).toBe('15 km');
  });
});
