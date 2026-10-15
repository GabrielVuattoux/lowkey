import { StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import type { NearbyEvent } from '@/hooks/use-nearby-events';
import { CATEGORY_LABELS, formatDistance, formatWhen, parseServerTime } from '@/lib/format';

export function EventCard({ event }: { event: NearbyEvent }) {
  const start = parseServerTime(event.startTime);
  const end = parseServerTime(event.endTime);

  return (
    <ThemedView type="backgroundElement" style={styles.card}>
      <View style={styles.row}>
        <ThemedText type="smallBold" themeColor="textSecondary">
          {CATEGORY_LABELS[event.category]}
        </ThemedText>
        <ThemedText type="small" themeColor="textSecondary">
          {formatDistance(event.distanceKm)}
        </ThemedText>
      </View>

      <ThemedText style={styles.title}>{event.title}</ThemedText>
      <ThemedText type="small">{formatWhen(start, end)}</ThemedText>

      {event.description ? (
        <ThemedText type="small" themeColor="textSecondary" numberOfLines={2}>
          {event.description}
        </ThemedText>
      ) : null}

      <ThemedText type="small" themeColor="textSecondary">
        {event.participantCount} going
      </ThemedText>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  card: {
    gap: Spacing.one,
    padding: Spacing.three,
    borderRadius: Spacing.three,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 18,
    lineHeight: 24,
    fontWeight: 700,
  },
});
