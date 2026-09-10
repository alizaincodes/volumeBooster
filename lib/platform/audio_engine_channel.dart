import 'package:flutter/services.dart';
import '../../core/constants.dart';

class AudioEngineChannel {
  static const MethodChannel _methodChannel =
      MethodChannel(AppConstants.methodChannelName);

  static const EventChannel _eventChannel =
      EventChannel(AppConstants.eventChannelName);

  static Future<bool> setMasterBoost(bool enabled) async {
    try {
      final result = await _methodChannel.invokeMethod<bool>(
        'setMasterBoost',
        {'enabled': enabled},
      );
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  static Future<bool> setAppBoost(String packageName, int boostPercent) async {
    try {
      final result = await _methodChannel.invokeMethod<bool>(
        'setAppBoost',
        {
          'packageName': packageName,
          'boostPercent': boostPercent,
        },
      );
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  static Future<bool> resetAllBoosts() async {
    try {
      final result = await _methodChannel.invokeMethod<bool>('resetAllBoosts');
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  static Future<bool> isHeadphonesConnected() async {
    try {
      final result = await _methodChannel.invokeMethod<bool>('isHeadphonesConnected');
      return result ?? false;
    } on PlatformException catch (_) {
      return false;
    }
  }

  static Future<String?> getHeadphoneName() async {
    try {
      final result = await _methodChannel.invokeMethod<String>('getHeadphoneName');
      return result;
    } on PlatformException catch (_) {
      return null;
    }
  }

  static Stream<dynamic> get activeSessionsStream {
    return _eventChannel.receiveBroadcastStream();
  }
}
