import 'package:flutter/material.dart';
import '../features/home/models/app_profile.dart';
import '../platform/audio_engine_channel.dart';

class VolumeBoostState extends ChangeNotifier {
  bool isMasterEnabled = false;
  bool isHeadphonesConnected = false;
  String? headphoneName;
  String? headphoneDisconnectedBanner;
  String themeMode = 'SYSTEM'; // SYSTEM, LIGHT, DARK, AMOLED
  bool dynamicColor = true;
  bool hapticsEnabled = true;
  bool resumeOnBoot = false;
  bool hasSeenOnboarding = false;

  List<AppProfile> profiles = [];
  String? activeSessionPackage;
  String? activeSessionName;
  int? activeSessionBoost;

  void setMasterEnabled(bool value) {
    isMasterEnabled = value;
    AudioEngineChannel.setMasterBoost(value);
    notifyListeners();
  }

  void updateProfileBoost(String packageName, int boost) {
    final index = profiles.indexWhere((p) => p.packageName == packageName);
    if (index != -1) {
      profiles[index] = profiles[index].copyWith(boostPercent: boost);
      AudioEngineChannel.setAppBoost(packageName, boost);
      notifyListeners();
    }
  }

  void toggleProfileEnabled(String packageName, bool enabled) {
    final index = profiles.indexWhere((p) => p.packageName == packageName);
    if (index != -1) {
      profiles[index] = profiles[index].copyWith(isEnabled: enabled);
      AudioEngineChannel.setAppBoost(packageName, enabled ? profiles[index].boostPercent : 100);
      notifyListeners();
    }
  }

  void resetAllBoosts() {
    profiles = profiles.map((p) => p.copyWith(boostPercent: 100)).toList();
    AudioEngineChannel.resetAllBoosts();
    notifyListeners();
  }

  void dismissHeadphoneBanner() {
    headphoneDisconnectedBanner = null;
    notifyListeners();
  }
}
