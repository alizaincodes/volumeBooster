import 'package:flutter/material.dart';
import '../../../core/theme.dart';
import '../../../providers/volume_boost_providers.dart';

class SettingsScreen extends StatelessWidget {
  final VolumeBoostState state;

  const SettingsScreen({super.key, required this.state});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Headphone Status Card
          Card(
            color: state.isHeadphonesConnected
                ? AppTheme.amberWarning.withOpacity(0.15)
                : Theme.of(context).colorScheme.surfaceVariant,
            child: ListTile(
              leading: Icon(
                state.isHeadphonesConnected ? Icons.headphones : Icons.speaker,
                color: state.isHeadphonesConnected ? AppTheme.amberWarning : null,
              ),
              title: Text(state.isHeadphonesConnected ? 'Headphones Connected' : 'Speaker Output'),
              subtitle: Text(state.isHeadphonesConnected
                  ? (state.headphoneName ?? 'Wired or Bluetooth Headset')
                  : 'Device Built-in Loudspeaker'),
              trailing: Chip(
                label: Text(
                  state.isHeadphonesConnected ? 'PROTECTED' : 'NORMAL',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ),

          const SizedBox(height: 20),
          const Text('Appearance', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.dark_mode),
                  title: const Text('Theme'),
                  subtitle: Text(state.themeMode),
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.palette),
                  title: const Text('Dynamic Color'),
                  value: state.dynamicColor,
                  onChanged: (val) {
                    state.dynamicColor = val;
                    state.notifyListeners();
                  },
                ),
                SwitchListTile(
                  secondary: const Icon(Icons.vibration),
                  title: const Text('Haptic Feedback'),
                  value: state.hapticsEnabled,
                  onChanged: (val) {
                    state.hapticsEnabled = val;
                    state.notifyListeners();
                  },
                ),
              ],
            ),
          ),

          const SizedBox(height: 20),
          const Text('Hearing Safety', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: [
                const ListTile(
                  leading: Icon(Icons.hearing),
                  title: Text('WHO Safe Listening'),
                  subtitle: Text('Keep volume under 60% and limit sessions to 60 minutes.'),
                ),
                Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: FilledButton.tonal(
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: const Text('Reset all volume boosts?'),
                          content: const Text('This will reset all app profiles back to 100% (normal volume).'),
                          actions: [
                            TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancel')),
                            FilledButton(
                              onPressed: () {
                                state.resetAllBoosts();
                                Navigator.of(ctx).pop();
                              },
                              style: FilledButton.styleFrom(backgroundColor: AppTheme.riskRed),
                              child: const Text('Reset All'),
                            ),
                          ],
                        ),
                      );
                    },
                    child: const Text('Reset All Boosts to 100% (Emergency)', style: TextStyle(color: AppTheme.riskRed)),
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 20),
          const Text('Battery & Performance', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  secondary: const Icon(Icons.power_settings_new),
                  title: const Text('Resume Boosting on Restart'),
                  subtitle: const Text('Re-enable engine when device boots'),
                  value: state.resumeOnBoot,
                  onChanged: (val) {
                    state.resumeOnBoot = val;
                    state.notifyListeners();
                  },
                ),
                const ListTile(
                  leading: Icon(Icons.shield),
                  title: Text('Zero-Polling Architecture'),
                  subtitle: Text('CPU usage is 0% when no audio is actively playing.'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
