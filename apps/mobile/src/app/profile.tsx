import { PlaceholderScreen } from '@/components/placeholder-screen';

export default function ProfileScreen() {
  return (
    <PlaceholderScreen
      title="You"
      promise="Your identity is a random Lowkey ID and a key pair that never leaves this phone."
      next={[
        'Lowkey ID and keys created on first launch',
        'Name and avatar shared only with friends, encrypted',
        'Encrypted backup protected by a passphrase',
      ]}
    />
  );
}
