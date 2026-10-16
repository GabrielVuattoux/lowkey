import { PlaceholderScreen } from '@/components/placeholder-screen';

export default function EventsScreen() {
  return (
    <PlaceholderScreen
      title="Events"
      promise="Event chats and photos are end-to-end encrypted: only participants can read them."
      next={[
        'Create and join events',
        'Encrypted chat and board for each event',
        'Events and their photos expire after they end',
      ]}
    />
  );
}
