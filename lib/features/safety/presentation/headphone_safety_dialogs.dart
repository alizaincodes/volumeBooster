import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/theme.dart';

class HeadphoneSafetyFlow {
  static Future<bool> showTripleDialog(
    BuildContext context, {
    required String appName,
    required int targetPercent,
    String? deviceDescription,
  }) async {
    // Dialog 1: Informational
    final continueStep1 = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        icon: const Icon(Icons.headphones, color: AppTheme.amberWarning, size: 36),
        title: const Text('Headphones detected'),
        content: Text(
          'You\'re about to boost volume above safe levels while wearing ${deviceDescription ?? "headphones"}. Loud, boosted audio played directly into your ears carries a real risk of permanent hearing damage.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: FilledButton.styleFrom(backgroundColor: AppTheme.amberWarning),
            child: const Text('Continue', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );

    if (continueStep1 != true) return false;

    // Dialog 2: Explicit Risk
    final continueStep2 = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        bool acknowledged = false;
        return StatefulBuilder(
          builder: (ctx, setState) => AlertDialog(
            icon: const Icon(Icons.warning_amber_rounded, color: AppTheme.riskRed, size: 36),
            title: const Text(
              'This can cause permanent hearing loss',
              style: TextStyle(color: AppTheme.riskRed, fontWeight: FontWeight.bold),
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'Boosting beyond 100% increases sound pressure at your eardrum well past manufacturer-safe limits. Hearing damage is often gradual and irreversible.',
                ),
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppTheme.riskRed.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Checkbox(
                        value: acknowledged,
                        activeColor: AppTheme.riskRed,
                        onChanged: (val) => setState(() => acknowledged = val ?? false),
                      ),
                      const Expanded(
                        child: Text(
                          'I understand the risk of permanent hearing damage.',
                          style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(ctx).pop(false),
                child: const Text('Go back'),
              ),
              FilledButton(
                onPressed: acknowledged ? () => Navigator.of(ctx).pop(true) : null,
                style: FilledButton.styleFrom(backgroundColor: AppTheme.riskRed),
                child: const Text('Continue'),
              ),
            ],
          ),
        );
      },
    );

    if (continueStep2 != true) return false;

    // Dialog 3: Final confirmation with 3-second cooldown countdown
    final confirmed = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        int countdown = 3;
        Timer? timer;

        return StatefulBuilder(
          builder: (ctx, setState) {
            timer ??= Timer.periodic(const Duration(seconds: 1), (t) {
              if (countdown > 0) {
                setState(() => countdown--);
              } else {
                t.cancel();
              }
            });

            return AlertDialog(
              title: const Text('Confirm volume boost'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Boost level: $targetPercent% will be applied to $appName while headphones are connected. Start low and never boost suddenly.',
                  ),
                  const SizedBox(height: 16),
                  Slider(
                    value: targetPercent.toDouble(),
                    min: 0,
                    max: 300,
                    onChanged: null, // Disabled live preview
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () {
                    timer?.cancel();
                    Navigator.of(ctx).pop(false);
                  },
                  child: const Text('Cancel'),
                ),
                FilledButton(
                  onPressed: countdown == 0
                      ? () {
                          timer?.cancel();
                          Navigator.of(ctx).pop(true);
                        }
                      : null,
                  style: FilledButton.styleFrom(backgroundColor: AppTheme.riskRed),
                  child: Text(countdown > 0 ? 'Confirm ($countdown)' : 'Confirm'),
                ),
              ],
            );
          },
        );
      },
    );

    return confirmed == true;
  }
}
