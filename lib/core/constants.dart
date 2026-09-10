class AppConstants {
  static const String appName = 'Volume Boost';
  static const String appVersion = '1.0.0';

  // Audio Engine limits
  static const int minBoostPercent = 0;
  static const int normalBoostPercent = 100;
  static const int warningBoostPercent = 150;
  static const int highRiskBoostPercent = 200;
  static const int maxBoostPercent = 300;
  static const int maxGainMillibels = 2000; // ~20 dB hard ceiling

  // Method and Event Channels
  static const String methodChannelName = 'com.volumeboost.app/audio_engine';
  static const String eventChannelName = 'com.volumeboost.app/active_sessions';

  // Database
  static const String databaseName = 'volume_boost.db';
  static const String appProfilesTable = 'app_profiles';
}
