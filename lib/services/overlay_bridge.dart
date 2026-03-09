import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

class OverlayBridge {
  static const _ch = MethodChannel('com.goassistant/overlay');

  static Future<bool> hasOverlayPermission() async {
    return await _ch.invokeMethod<bool>('hasOverlayPermission') ?? false;
  }

  static Future<bool> requestOverlayPermission() async {
    return await _ch.invokeMethod<bool>('requestOverlayPermission') ?? false;
  }

  static Future<bool> requestScreenCapture() async {
    return await _ch.invokeMethod<bool>('requestScreenCapture') ?? false;
  }

  static Future<void> startOverlay({
    required String apiKey,
    required String turn,
    required int boardSize,
  }) async {
    await _ch.invokeMethod('startOverlay', {
      'apiKey':    apiKey,
      'turn':      turn,
      'boardSize': boardSize,
    });
  }

  static Future<void> stopOverlay() async {
    await _ch.invokeMethod('stopOverlay');
  }

  static Future<bool> isRunning() async {
    return await _ch.invokeMethod<bool>('isOverlayRunning') ?? false;
  }

  // ── Saved settings ────────────────────────

  static Future<Map<String, dynamic>> loadSettings() async {
    final p = await SharedPreferences.getInstance();
    return {
      'apiKey':    p.getString('apiKey')    ?? '',
      'turn':      p.getString('turn')      ?? 'black',
      'boardSize': p.getInt('boardSize')    ?? 19,
    };
  }

  static Future<void> saveSettings({
    required String apiKey,
    required String turn,
    required int boardSize,
  }) async {
    final p = await SharedPreferences.getInstance();
    await p.setString('apiKey',    apiKey);
    await p.setString('turn',      turn);
    await p.setInt('boardSize',    boardSize);
  }
}
