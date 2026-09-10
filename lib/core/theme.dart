import 'package:flutter/material.dart';

class AppTheme {
  static const Color seedColor = Color(0xFF5B4FE5); // Electric Indigo
  static const Color amberWarning = Color(0xFFF59E0B);
  static const Color riskRed = Color(0xFFEF4444);
  static const Color safeGreen = Color(0xFF10B981);
  static const Color amoledBlack = Color(0xFF000000);

  static ThemeData lightTheme({Color? dynamicSeed}) {
    final seed = dynamicSeed ?? seedColor;
    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: seed,
        brightness: Brightness.light,
      ),
      cardTheme: const CardTheme(
        shape: RoundedCornerShape(24.0),
        elevation: 1,
      ),
    );
  }

  static ThemeData darkTheme({Color? dynamicSeed, bool isAmoled = false}) {
    final seed = dynamicSeed ?? seedColor;
    final baseScheme = ColorScheme.fromSeed(
      seedColor: seed,
      brightness: Brightness.dark,
    );

    final scheme = isAmoled
        ? baseScheme.copyWith(
            background: amoledBlack,
            surface: const Color(0xFF0C0C10),
            surfaceVariant: const Color(0xFF181820),
          )
        : baseScheme;

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: isAmoled ? amoledBlack : null,
      cardTheme: const CardTheme(
        shape: RoundedCornerShape(24.0),
        elevation: 1,
      ),
    );
  }
}

class RoundedCornerShape extends RoundedRectangleBorder {
  const RoundedCornerShape(double radius)
      : super(borderRadius: BorderRadius.all(Radius.circular(radius)));
}
