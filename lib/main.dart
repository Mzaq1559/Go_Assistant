import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'screens/setup_screen.dart';
import 'theme/theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations(
      [DeviceOrientation.portraitUp]);
  runApp(const GoAssistantApp());
}

class GoAssistantApp extends StatelessWidget {
  const GoAssistantApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Go Assistant',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.dark(),
        home: const SetupScreen(),
      );
}
