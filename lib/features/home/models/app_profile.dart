class AppProfile {
  final String packageName;
  final String appName;
  final int boostPercent;
  final bool isEnabled;
  final int lastUsedTimestamp;
  final String? iconCachePath;

  const AppProfile({
    required this.packageName,
    required this.appName,
    this.boostPercent = 100,
    this.isEnabled = true,
    required this.lastUsedTimestamp,
    this.iconCachePath,
  });

  AppProfile copyWith({
    String? packageName,
    String? appName,
    int? boostPercent,
    bool? isEnabled,
    int? lastUsedTimestamp,
    String? iconCachePath,
  }) {
    return AppProfile(
      packageName: packageName ?? this.packageName,
      appName: appName ?? this.appName,
      boostPercent: boostPercent ?? this.boostPercent,
      isEnabled: isEnabled ?? this.isEnabled,
      lastUsedTimestamp: lastUsedTimestamp ?? this.lastUsedTimestamp,
      iconCachePath: iconCachePath ?? this.iconCachePath,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'packageName': packageName,
      'appName': appName,
      'boostPercent': boostPercent,
      'isEnabled': isEnabled ? 1 : 0,
      'lastUsedTimestamp': lastUsedTimestamp,
      'iconCachePath': iconCachePath,
    };
  }

  factory AppProfile.fromMap(Map<String, dynamic> map) {
    return AppProfile(
      packageName: map['packageName'] as String,
      appName: map['appName'] as String,
      boostPercent: (map['boostPercent'] as num?)?.toInt() ?? 100,
      isEnabled: (map['isEnabled'] as int?) == 1,
      lastUsedTimestamp: (map['lastUsedTimestamp'] as num?)?.toInt() ?? 0,
      iconCachePath: map['iconCachePath'] as String?,
    );
  }
}
