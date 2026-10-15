import { PlaceholderScreen } from '@/components/placeholder-screen';

export default function FriendsScreen() {
  return (
    <PlaceholderScreen
      title="Friends"
      promise="Add friends with a QR code or an invite link. No phone number, no contact upload."
      next={[
        "Scan a friend's QR code",
        'Verified badge for friends added in person',
        'Invite links that open the app',
      ]}
    />
  );
}
