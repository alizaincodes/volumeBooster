import 'package:flutter/material.dart';
import '../../home/models/app_profile.dart';
import '../../../providers/volume_boost_providers.dart';

class DiscoverScreen extends StatefulWidget {
  final VolumeBoostState state;

  const DiscoverScreen({super.key, required this.state});

  @override
  State<DiscoverScreen> createState() => _DiscoverScreenState();
}

class _DiscoverScreenState extends State<DiscoverScreen> {
  String _search = '';

  final List<Map<String, String>> _popularApps = [
    {'name': 'Spotify', 'package': 'com.spotify.music'},
    {'name': 'YouTube', 'package': 'com.google.android.youtube'},
    {'name': 'YouTube Music', 'package': 'com.google.android.apps.youtube.music'},
    {'name': 'Netflix', 'package': 'com.netflix.mediaclient'},
    {'name': 'SoundCloud', 'package': 'com.soundcloud.android'},
    {'name': 'Pocket Casts', 'package': 'au.com.shiftyjelly.pocketcasts'},
    {'name': 'VLC', 'package': 'org.videolan.vlc'},
    {'name': 'TikTok', 'package': 'com.zhiliaoapp.musically'},
  ];

  @override
  Widget build(BuildContext context) {
    final filtered = _popularApps.where((app) {
      final name = app['name']!.toLowerCase();
      final pkg = app['package']!.toLowerCase();
      return name.contains(_search.toLowerCase()) || pkg.contains(_search.toLowerCase());
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover & Add Apps'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          SearchBar(
            hintText: 'Search installed apps...',
            leading: const Icon(Icons.search),
            onChanged: (val) => setState(() => _search = val),
          ),
          const SizedBox(height: 20),
          const Text('All Installed Apps', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          ...filtered.map((app) {
            final isBoosted = widget.state.profiles.any((p) => p.packageName == app['package']);
            return Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: ListTile(
                leading: CircleAvatar(child: Text(app['name']![0])),
                title: Text(app['name']!, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text(app['package']!),
                trailing: isBoosted
                    ? const Chip(label: Text('Active'))
                    : FilledButton.tonal(
                        onPressed: () {
                          widget.state.profiles.add(
                            AppProfile(
                              packageName: app['package']!,
                              appName: app['name']!,
                              boostPercent: 140,
                              lastUsedTimestamp: DateTime.now().millisecondsSinceEpoch,
                            ),
                          );
                          widget.state.updateProfileBoost(app['package']!, 140);
                          setState(() {});
                        },
                        child: const Text('Boost'),
                      ),
              ),
            );
          }),
        ],
      ),
    );
  }
}
