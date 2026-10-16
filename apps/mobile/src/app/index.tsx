import {
  ActivityIndicator,
  FlatList,
  Linking,
  Platform,
  RefreshControl,
  StyleSheet,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EventCard } from '@/components/event-card';
import { TextButton } from '@/components/text-button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';
import { useNearbyEvents, type NearbyEventsState } from '@/hooks/use-nearby-events';
import { useTheme } from '@/hooks/use-theme';
import { API_URL } from '@/lib/api';
import type { Coordinates } from '@/lib/geo';

// Development only: browse the demo events of the server's "dev" profile from anywhere,
// for example in the web preview or on a simulator without a location.
const DEMO_POSITION: Coordinates = { latitude: 47.3769, longitude: 8.5417 };

// Until the MapLibre map lands (roadmap phase 4), the Map tab lists public events nearby.
export default function MapScreen() {
  const { state, refreshing, refresh, positionOverride, overridePosition } = useNearbyEvents();
  const theme = useTheme();

  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
        <FlatList
          data={state.status === 'ready' ? state.events : []}
          keyExtractor={(event) => String(event.id)}
          renderItem={({ item }) => <EventCard event={item} />}
          ItemSeparatorComponent={Separator}
          ListHeaderComponent={<Header usingDemo={positionOverride !== undefined} />}
          ListEmptyComponent={
            <EmptyState
              state={state}
              onRetry={refresh}
              onUseDemo={() => overridePosition(DEMO_POSITION)}
            />
          }
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={refresh}
              tintColor={theme.textSecondary}
            />
          }
          contentContainerStyle={styles.list}
        />
      </SafeAreaView>
    </ThemedView>
  );
}

function Header({ usingDemo }: { usingDemo: boolean }) {
  return (
    <View style={styles.header}>
      <ThemedText type="subtitle">Around you</ThemedText>
      <ThemedText type="small" themeColor="textSecondary">
        {usingDemo
          ? 'Demo position: Zurich (development only).'
          : 'Your position stays on this phone. The server only learns which ~5 km area you are in.'}
      </ThemedText>
    </View>
  );
}

type EmptyStateProps = {
  state: NearbyEventsState;
  onRetry: () => void;
  onUseDemo: () => void;
};

function EmptyState({ state, onRetry, onUseDemo }: EmptyStateProps) {
  if (state.status === 'loading') {
    return <ActivityIndicator style={styles.empty} />;
  }

  if (state.status === 'no-permission') {
    return (
      <View style={styles.empty}>
        <ThemedText style={styles.center}>Location is off</ThemedText>
        <ThemedText type="small" themeColor="textSecondary" style={styles.center}>
          Lowkey needs your location to find events near you. It never leaves this phone.
        </ThemedText>
        {Platform.OS !== 'web' && (
          <TextButton label="Open settings" onPress={() => Linking.openSettings()} />
        )}
        <TextButton label="Try again" onPress={onRetry} />
        {__DEV__ && <TextButton label="Use Zurich (dev only)" onPress={onUseDemo} />}
      </View>
    );
  }

  if (state.status === 'error') {
    return (
      <View style={styles.empty}>
        <ThemedText style={styles.center}>Can&apos;t reach Lowkey</ThemedText>
        <ThemedText type="small" themeColor="textSecondary" style={styles.center}>
          {__DEV__
            ? `${state.message}. Is the server running at ${API_URL}?`
            : 'Check your connection and try again.'}
        </ThemedText>
        <TextButton label="Try again" onPress={onRetry} />
      </View>
    );
  }

  return (
    <View style={styles.empty}>
      <ThemedText style={styles.center}>No public events around you right now.</ThemedText>
      {__DEV__ && <TextButton label="Use Zurich (dev only)" onPress={onUseDemo} />}
    </View>
  );
}

function Separator() {
  return <View style={styles.separator} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  safeArea: {
    flex: 1,
    maxWidth: MaxContentWidth,
  },
  list: {
    paddingHorizontal: Spacing.four,
    // On the web, the tab bar floats over the top of the page
    paddingTop: Platform.select({ web: Spacing.six + Spacing.three, default: Spacing.three }),
    paddingBottom: BottomTabInset + Spacing.three,
  },
  header: {
    gap: Spacing.two,
    marginBottom: Spacing.four,
  },
  empty: {
    gap: Spacing.three,
    alignItems: 'center',
    paddingVertical: Spacing.six,
  },
  center: {
    textAlign: 'center',
  },
  separator: {
    height: Spacing.three,
  },
});
