import { PlaceholderScreen } from '@/components/placeholder-screen';

export default function MapScreen() {
  return (
    <PlaceholderScreen
      title="Map"
      promise="Public events around you. Your position never leaves your phone."
      next={[
        'Public events, fetched by coarse area only',
        'OpenStreetMap tiles instead of Mapbox',
        'Friends-only events, end-to-end encrypted',
      ]}
    />
  );
}
