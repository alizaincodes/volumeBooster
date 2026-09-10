import 'package:flutter/material.dart';
import '../../../core/theme.dart';
import '../../home/models/app_profile.dart';
import '../../../providers/volume_boost_providers.dart';
import '../../safety/presentation/headphone_safety_dialogs.dart';

class HomeScreen extends StatelessWidget {
  final VolumeBoostState state;
  final VoidCallback onNavigateToDiscover;

  const HomeScreen({
    super.key,
    required this.state,
    required this.onNavigateToDiscover,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Volume Boost', style: TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
        actions: [
          if (state.isHeadphonesConnected)
            Container(
              margin: const EdgeInsets.only(right: 12),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: AppTheme.amberWarning.withOpacity(0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: const Row(
                children: [
                  Icon(Icons.headphones, size: 16, color: AppTheme.amberWarning),
                  SizedBox(width: 4),
                  Text('Protected', style: TextStyle(color: AppTheme.amberWarning, fontWeight: FontWeight.bold, fontSize: 12)),
                ],
              ),
            ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: onNavigateToDiscover,
        icon: const Icon(Icons.add),
        label: const Text('Add App'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
        children: [
          // Headphone disconnected banner
          if (state.headphoneDisconnectedBanner != null)
            Container(
              margin: const EdgeInsets.only(bottom: 16),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.tertiaryContainer,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(Icons.speaker),
                  const SizedBox(width: 8),
                  Expanded(child: Text(state.headphoneDisconnectedBanner!)),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: state.dismissHeadphoneBanner,
                  ),
                ],
              ),
            ),

          // Master Boost Card
          Card(
            shape: const RoundedCornerShape(28.0),
            color: state.isMasterEnabled
                ? Theme.of(context).colorScheme.primaryContainer
                : Theme.of(context).colorScheme.surfaceVariant,
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            state.isMasterEnabled ? 'Boosting Active' : 'Boosting Paused',
                            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 4),
                          Text(state.isMasterEnabled ? 'Audio Engine Monitoring (0% CPU Idle)' : 'Engine Inactive'),
                        ],
                      ),
                      Switch(
                        value: state.isMasterEnabled,
                        onChanged: (val) {
                          if (val && state.isHeadphonesConnected) {
                            HeadphoneSafetyFlow.showTripleDialog(
                              context,
                              appName: 'All Audio',
                              targetPercent: 150,
                            ).then((ok) {
                              if (ok) state.setMasterEnabled(true);
                            });
                          } else {
                            state.setMasterEnabled(val);
                          }
                        },
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),

          // Per-App Boost Cards
          ...state.profiles.map((profile) => Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          CircleAvatar(
                            child: Text(profile.appName.isNotEmpty ? profile.appName[0] : 'A'),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(profile.appName, style: const TextStyle(fontWeight: FontWeight.bold)),
                                Text(profile.packageName, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                              ],
                            ),
                          ),
                          Text(
                            '${profile.boostPercent}%',
                            style: TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.w900,
                              color: profile.boostPercent > 200
                                  ? AppTheme.riskRed
                                  : (profile.boostPercent > 100 ? AppTheme.amberWarning : AppTheme.seedColor),
                            ),
                          ),
                          Switch(
                            value: profile.isEnabled,
                            onChanged: (val) => state.toggleProfileEnabled(profile.packageName, val),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Slider(
                        value: profile.boostPercent.toDouble(),
                        min: 0,
                        max: 300,
                        divisions: 30,
                        activeColor: profile.boostPercent > 200
                            ? AppTheme.riskRed
                            : (profile.boostPercent > 100 ? AppTheme.amberWarning : AppTheme.seedColor),
                        onChanged: profile.isEnabled
                            ? (val) {
                                final boost = val.round();
                                if (boost > 150 && state.isHeadphonesConnected) {
                                  HeadphoneSafetyFlow.showTripleDialog(
                                    context,
                                    appName: profile.appName,
                                    targetPercent: boost,
                                  ).then((ok) {
                                    if (ok) state.updateProfileBoost(profile.packageName, boost);
                                  });
                                } else {
                                  state.updateProfileBoost(profile.packageName, boost);
                                }
                              }
                            : null,
                      ),
                    ],
                  ),
                ),
              )),
        ],
      ),
    );
  }
}
