import 'package:flutter/material.dart';
import '../../../core/theme.dart';

class OnboardingScreen extends StatefulWidget {
  final VoidCallback onComplete;

  const OnboardingScreen({super.key, required this.onComplete});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  final ScrollController _hearingScrollController = ScrollController();
  int _currentPage = 0;
  bool _scrolledToEndOfSafety = false;

  @override
  void initState() {
    super.initState();
    _hearingScrollController.addListener(() {
      if (_hearingScrollController.position.pixels >=
          _hearingScrollController.position.maxScrollExtent - 40) {
        if (!_scrolledToEndOfSafety) {
          setState(() => _scrolledToEndOfSafety = true);
        }
      }
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    _hearingScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              // Indicator
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(
                  4,
                  (index) => AnimatedContainer(
                    duration: const Duration(milliseconds: 300),
                    margin: const EdgeInsets.symmetric(horizontal: 4),
                    height: 8,
                    width: _currentPage == index ? 24 : 8,
                    decoration: BoxDecoration(
                      color: _currentPage == index
                          ? Theme.of(context).colorScheme.primary
                          : Colors.grey.withOpacity(0.3),
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              // Pages
              Expanded(
                child: PageView(
                  controller: _pageController,
                  onPageChanged: (page) => setState(() => _currentPage = page),
                  children: [
                    _buildWelcome(),
                    _buildHearingSafety(),
                    _buildPermissionInfo(),
                    _buildReady(),
                  ],
                ),
              ),
              // Actions
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (_currentPage > 0)
                    OutlinedButton(
                      onPressed: () {
                        _pageController.previousPage(
                          duration: const Duration(milliseconds: 300),
                          curve: Curves.easeInOut,
                        );
                      },
                      child: const Text('Back'),
                    )
                  else
                    const SizedBox.shrink(),
                  FilledButton(
                    onPressed: () {
                      if (_currentPage < 3) {
                        _pageController.nextPage(
                          duration: const Duration(milliseconds: 300),
                          curve: Curves.easeInOut,
                        );
                      } else {
                        widget.onComplete();
                      }
                    },
                    child: Text(_currentPage == 3 ? 'Get Started' : 'Continue'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWelcome() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.volume_up_rounded, size: 72, color: Theme.of(context).colorScheme.primary),
        const SizedBox(height: 24),
        const Text(
          'Welcome to Volume Boost',
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 12),
        const Text(
          'Push your sound output up to 300% with independent per-app boost profiles.',
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildHearingSafety() {
    return SingleChildScrollView(
      controller: _hearingScrollController,
      child: Column(
        children: [
          const Icon(Icons.hearing, size: 56, color: AppTheme.amberWarning),
          const SizedBox(height: 16),
          const Text(
            'Hearing Safety Education',
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          const Text('Read thoroughly before continuing:'),
          const SizedBox(height: 16),
          _card('The 60/60 Rule', 'Listen under 60% volume for at most 60 minutes per session.'),
          const SizedBox(height: 12),
          _card('Gradual Adjustment', 'Increase boost gradually in small steps; never jump to 300% instantly.'),
          const SizedBox(height: 12),
          _card('Rest Period', 'Take regular breaks to avoid ear fatigue and permanent hearing strain.'),
          const SizedBox(height: 16),
          Text(
            _scrolledToEndOfSafety ? '✓ Scrolled to end' : 'Scroll down to finish reading ⬇',
            style: TextStyle(color: _scrolledToEndOfSafety ? Colors.green : Colors.grey),
          ),
        ],
      ),
    );
  }

  Widget _buildPermissionInfo() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: const [
        Icon(Icons.notifications_active, size: 64, color: AppTheme.seedColor),
        SizedBox(height: 20),
        Text('Background Audio Service', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
        SizedBox(height: 12),
        Text(
          'Android requires a low-priority ongoing notification to keep the audio engine alive without being killed.',
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildReady() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: const [
        Icon(Icons.check_circle_outline, size: 72, color: AppTheme.safeGreen),
        SizedBox(height: 20),
        Text('You are Ready!', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
        SizedBox(height: 12),
        Text('Turn on the master switch on the Home tab to begin.', textAlign: TextAlign.center),
      ],
    );
  }

  Widget _card(String title, String body) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 4),
            Text(body),
          ],
        ),
      ),
    );
  }
}
