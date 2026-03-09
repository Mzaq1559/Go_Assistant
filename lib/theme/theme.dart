import 'package:flutter/material.dart';

class AppTheme {
  static const bg     = Color(0xFF0D1117);
  static const card   = Color(0xFF161B22);
  static const elev   = Color(0xFF1C2230);
  static const gold   = Color(0xFFD4A843);
  static const green  = Color(0xFF3FB950);
  static const red    = Color(0xFFF85149);
  static const blue   = Color(0xFF58A6FF);
  static const pri    = Color(0xFFE6EDF3);
  static const sec    = Color(0xFF8B949E);
  static const muted  = Color(0xFF484F58);
  static const border = Color(0xFF30363D);

  static ThemeData dark() => ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: bg,
    colorScheme: const ColorScheme.dark(
      primary: gold, secondary: blue,
      surface: card, onPrimary: bg, onSurface: pri,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: bg, foregroundColor: pri, elevation: 0,
      titleTextStyle: TextStyle(color: pri, fontSize: 18,
          fontWeight: FontWeight.w700),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: gold, foregroundColor: bg,
        elevation: 0, padding: const EdgeInsets.symmetric(vertical: 15),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        textStyle: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15),
      ),
    ),
    cardTheme: CardThemeData(
      color: card, elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: border),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true, fillColor: elev,
      labelStyle: const TextStyle(color: sec),
      hintStyle: const TextStyle(color: muted),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: border)),
      enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: border)),
      focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: gold, width: 1.5)),
    ),
  );
}
