import 'package:flutter/material.dart';
import 'core/theme.dart';
import 'features/home/presentation/home_screen.dart';
import 'features/discover/presentation/discover_screen.dart';
import 'features/settings/presentation/settings_screen.dart';
import 'features/onboarding/presentation/onboarding_screen.dart';
import 'providers/volume_boost_providers.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const VolumeBoostApp());
}

class VolumeBoostApp extends StatefulWidget {
  const VolumeBoostApp({super.key});

  @override
  State<VolumeBoostApp> createState() => _VolumeBoostAppState();
}

class _VolumeBoostAppState extends State<VolumeBoostApp> {
  final VolumeBoostState _state = VolumeBoostState();

  @override
  void initState() {
    super.initState();
    _state.addListener(() => setState(() {}));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Volume Boost',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme(),
      darkTheme: AppTheme.darkTheme(isAmoled: _state.themeMode == 'AMOLED'),
      themeMode: _state.themeMode == 'DARK' || _state.themeMode == 'AMOLED'
          ? ThemeMode.dark
          : (_state.themeMode == 'LIGHT' ? ThemeMode.light : ThemeMode.system),
      home: _state.hasSeenOnboarding
          ? MainNavigationShell(state: _state)
          : OnboardingScreen(
              onComplete: () {
                setState(() => _state.hasSeenOnboarding = true);
              },
            ),
    );
  }
}

class MainNavigationShell extends StatefulWidget {
  final VolumeBoostState state;

  const MainNavigationShell({super.key, required this.state});

  @override
  State<MainNavigationShell> createState() => _MainNavigationShellState();
}

class _MainNavigationShellState extends State<MainNavigationShell> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    final screens = [
      HomeScreen(
        state: widget.state,
        onNavigateToDiscover: () => setState(() => _currentIndex = 1),
      ),
      DiscoverScreen(state: widget.state),
      SettingsScreen(state: widget.state),
    ];

    return Scaffold(
      body: screens[_currentIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) => setState(() => _currentIndex = index),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.explore_outlined),
            selectedIcon: Icon(Icons.explore),
            label: 'Discover',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}
