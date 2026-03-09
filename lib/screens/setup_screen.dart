import 'package:flutter/material.dart';
import '../services/overlay_bridge.dart';
import '../theme/theme.dart';

class SetupScreen extends StatefulWidget {
  const SetupScreen({super.key});
  @override
  State<SetupScreen> createState() => _SetupScreenState();
}

class _SetupScreenState extends State<SetupScreen> {
  final _keyCtrl = TextEditingController();
  bool _showKey    = false;
  bool _running    = false;
  bool _hasOverlay = false;
  bool _hasScreen  = false;
  bool _loading    = true;
  String _turn     = 'black';
  int    _boardSize = 19;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final s = await OverlayBridge.loadSettings();
    _keyCtrl.text = s['apiKey'] as String;
    _turn         = s['turn']   as String;
    _boardSize    = s['boardSize'] as int;
    _hasOverlay   = await OverlayBridge.hasOverlayPermission();
    _running      = await OverlayBridge.isRunning();
    setState(() => _loading = false);
  }

  Future<void> _grantOverlay() async {
    final ok = await OverlayBridge.requestOverlayPermission();
    setState(() => _hasOverlay = ok);
  }

  Future<void> _grantScreen() async {
    final ok = await OverlayBridge.requestScreenCapture();
    setState(() => _hasScreen = ok);
    if (!ok && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Screen capture permission denied')));
    }
  }

  Future<void> _startOverlay() async {
    final key = _keyCtrl.text.trim();
    if (key.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter your Claude API key first')));
      return;
    }

    await OverlayBridge.saveSettings(
        apiKey: key, turn: _turn, boardSize: _boardSize);

    // Ask for screen capture permission if not yet granted
    if (!_hasScreen) {
      final ok = await OverlayBridge.requestScreenCapture();
      setState(() => _hasScreen = ok);
      if (!ok) return;
    }

    await OverlayBridge.startOverlay(
        apiKey: key, turn: _turn, boardSize: _boardSize);
    setState(() => _running = true);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Overlay started! You can now open WeChat.'),
          duration: Duration(seconds: 3),
        ),
      );
    }
  }

  Future<void> _stopOverlay() async {
    await OverlayBridge.stopOverlay();
    setState(() { _running = false; _hasScreen = false; });
  }

  @override
  void dispose() { _keyCtrl.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Scaffold(
      body: Center(child: CircularProgressIndicator(color: AppTheme.gold)));

    return Scaffold(
      backgroundColor: AppTheme.bg,
      appBar: AppBar(title: const Text('Go Assistant')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [

          // ── Hero ──────────────────────────────
          _hero(),
          const SizedBox(height: 24),

          // ── Permissions ───────────────────────
          _label('PERMISSIONS'),
          Card(child: Column(children: [
            _PermTile(
              icon:    Icons.layers_rounded,
              title:   'Draw Over Other Apps',
              subtitle: 'Required to show circle on WeChat',
              granted: _hasOverlay,
              onGrant: _grantOverlay,
            ),
            const Divider(height: 1),
            _PermTile(
              icon:    Icons.screenshot_monitor_rounded,
              title:   'Screen Capture',
              subtitle: 'Required to read the board',
              granted: _hasScreen,
              onGrant: _grantScreen,
            ),
          ])),
          const SizedBox(height: 20),

          // ── API Key ───────────────────────────
          _label('CLAUDE API KEY'),
          Card(child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TextField(
                  controller: _keyCtrl,
                  obscureText: !_showKey,
                  style: const TextStyle(color: AppTheme.pri,
                      fontFamily: 'monospace', fontSize: 13),
                  decoration: InputDecoration(
                    labelText: 'API Key',
                    hintText: 'sk-ant-api03-…',
                    suffixIcon: IconButton(
                      icon: Icon(_showKey
                          ? Icons.visibility_off_rounded
                          : Icons.visibility_rounded,
                          color: AppTheme.muted),
                      onPressed: () => setState(() => _showKey = !_showKey),
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Get a free key at console.anthropic.com → API Keys',
                  style: TextStyle(color: AppTheme.muted, fontSize: 11),
                ),
              ],
            ),
          )),
          const SizedBox(height: 20),

          // ── Game Settings ─────────────────────
          _label('GAME SETTINGS'),
          Card(child: Column(children: [
            // Turn
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              child: Row(children: [
                const Text('Next to play',
                    style: TextStyle(color: AppTheme.sec, fontSize: 14)),
                const Spacer(),
                _StoneBtn(isBlack: true,  selected: _turn == 'black',
                    onTap: () => setState(() => _turn = 'black')),
                const SizedBox(width: 10),
                _StoneBtn(isBlack: false, selected: _turn == 'white',
                    onTap: () => setState(() => _turn = 'white')),
              ]),
            ),
            const Divider(height: 1),
            // Board size
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              child: Row(children: [
                const Text('Board size',
                    style: TextStyle(color: AppTheme.sec, fontSize: 14)),
                const Spacer(),
                _sizeChip('Auto', 0),
                const SizedBox(width: 6),
                _sizeChip('9×9', 9),
                const SizedBox(width: 6),
                _sizeChip('13×13', 13),
                const SizedBox(width: 6),
                _sizeChip('19×19', 19),
              ]),
            ),
          ])),
          const SizedBox(height: 28),

          // ── Start / Stop button ───────────────
          if (!_hasOverlay)
            ElevatedButton.icon(
              icon: const Icon(Icons.layers_rounded),
              label: const Text('Grant Overlay Permission First'),
              onPressed: _grantOverlay,
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 52)),
            )
          else if (_running)
            ElevatedButton.icon(
              icon: const Icon(Icons.stop_rounded),
              label: const Text('Stop Overlay'),
              onPressed: _stopOverlay,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.red, foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 52)),
            )
          else
            ElevatedButton.icon(
              icon: const Icon(Icons.play_arrow_rounded),
              label: const Text('Start Overlay'),
              onPressed: _startOverlay,
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 52)),
            ),

          const SizedBox(height: 16),

          // ── How it works ──────────────────────
          if (!_running) _howItWorks(),

          if (_running) ...[
            _runningCard(),
            const SizedBox(height: 16),
          ],

          // ── Ethics ────────────────────────────
          _ethicsNote(),
          const SizedBox(height: 30),
        ],
      ),
    );
  }

  // ── Widgets ─────────────────────────────────

  Widget _hero() => Column(children: [
    const SizedBox(height: 8),
    Row(mainAxisAlignment: MainAxisAlignment.center, children: [
      _stone(true, 32), const SizedBox(width: 12),
      const Text('Go Assistant',
          style: TextStyle(color: AppTheme.pri, fontSize: 26,
              fontWeight: FontWeight.w800, letterSpacing: 0.5)),
      const SizedBox(width: 12), _stone(false, 32),
    ]),
    const SizedBox(height: 6),
    const Text('AI overlay · powered by Claude',
        style: TextStyle(color: AppTheme.gold, fontSize: 13, letterSpacing: 0.3)),
  ]);

  Widget _stone(bool isBlack, double size) => Container(
    width: size, height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: isBlack ? const Color(0xFF111111) : Colors.white,
      border: Border.all(color: AppTheme.border, width: 1.5),
      boxShadow: [BoxShadow(
        color: AppTheme.gold.withOpacity(0.25), blurRadius: 8)],
    ),
  );

  Widget _label(String text) => Padding(
    padding: const EdgeInsets.only(bottom: 8, left: 4),
    child: Text(text, style: const TextStyle(color: AppTheme.muted,
        fontSize: 11, fontWeight: FontWeight.w700, letterSpacing: 1.2)),
  );

  Widget _sizeChip(String label, int value) => GestureDetector(
    onTap: () => setState(() => _boardSize = value),
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
      decoration: BoxDecoration(
        color: _boardSize == value
            ? AppTheme.gold.withOpacity(0.15) : AppTheme.elev,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: _boardSize == value ? AppTheme.gold : AppTheme.border),
      ),
      child: Text(label, style: TextStyle(
        color: _boardSize == value ? AppTheme.gold : AppTheme.sec,
        fontSize: 11, fontWeight: FontWeight.w600)),
    ),
  );

  Widget _howItWorks() => Card(child: Padding(
    padding: const EdgeInsets.all(14),
    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('How it works',
          style: TextStyle(color: AppTheme.pri,
              fontWeight: FontWeight.w700, fontSize: 14)),
      const SizedBox(height: 10),
      _step('1', 'Grant both permissions above'),
      _step('2', 'Enter your Claude API key'),
      _step('3', 'Tap Start Overlay — a floating bubble appears'),
      _step('4', 'Open WeChat and start your Go game'),
      _step('5', 'When it\'s your turn, tap the bubble ⚫'),
      _step('6', 'A gold circle appears on the best move!'),
      _step('7', 'The bubble auto-switches Black↔White each tap'),
    ]),
  ));

  Widget _runningCard() => Container(
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: AppTheme.green.withOpacity(0.08),
      borderRadius: BorderRadius.circular(12),
      border: Border.all(color: AppTheme.green.withOpacity(0.3)),
    ),
    child: Row(children: [
      Container(width: 10, height: 10,
          decoration: BoxDecoration(shape: BoxShape.circle,
              color: AppTheme.green,
              boxShadow: [BoxShadow(color: AppTheme.green.withOpacity(0.5),
                  blurRadius: 6)])),
      const SizedBox(width: 10),
      const Expanded(child: Text(
        'Overlay is running.\nOpen WeChat and tap the floating bubble when it\'s your turn.',
        style: TextStyle(color: AppTheme.green, fontSize: 13, height: 1.5),
      )),
    ]),
  );

  Widget _ethicsNote() => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: AppTheme.red.withOpacity(0.07),
      borderRadius: BorderRadius.circular(10),
      border: Border.all(color: AppTheme.red.withOpacity(0.2)),
    ),
    child: const Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Icon(Icons.warning_amber_rounded, color: AppTheme.red, size: 16),
      SizedBox(width: 8),
      Expanded(child: Text(
        'For offline/casual practice only. Using AI help in competitive online matches violates platform Terms of Service.',
        style: TextStyle(color: AppTheme.red, fontSize: 11, height: 1.5),
      )),
    ]),
  );

  Widget _step(String n, String text) => Padding(
    padding: const EdgeInsets.only(bottom: 6),
    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Container(
        width: 18, height: 18,
        margin: const EdgeInsets.only(right: 8, top: 1),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: AppTheme.gold.withOpacity(0.12),
          border: Border.all(color: AppTheme.gold.withOpacity(0.4)),
        ),
        alignment: Alignment.center,
        child: Text(n, style: const TextStyle(color: AppTheme.gold,
            fontSize: 10, fontWeight: FontWeight.w800)),
      ),
      Expanded(child: Text(text,
          style: const TextStyle(color: AppTheme.sec, fontSize: 13, height: 1.4))),
    ]),
  );
}

class _PermTile extends StatelessWidget {
  final IconData icon;
  final String title, subtitle;
  final bool granted;
  final VoidCallback onGrant;
  const _PermTile({required this.icon, required this.title,
      required this.subtitle, required this.granted, required this.onGrant});

  @override
  Widget build(BuildContext context) => ListTile(
    leading: Icon(icon,
        color: granted ? AppTheme.green : AppTheme.muted, size: 24),
    title: Text(title,
        style: const TextStyle(color: AppTheme.pri, fontSize: 14)),
    subtitle: Text(subtitle,
        style: const TextStyle(color: AppTheme.sec, fontSize: 12)),
    trailing: granted
        ? const Icon(Icons.check_circle_rounded, color: AppTheme.green)
        : TextButton(
            onPressed: onGrant,
            style: TextButton.styleFrom(foregroundColor: AppTheme.gold),
            child: const Text('Grant', style: TextStyle(fontWeight: FontWeight.w700)),
          ),
    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
  );
}

class _StoneBtn extends StatelessWidget {
  final bool isBlack, selected;
  final VoidCallback onTap;
  const _StoneBtn({required this.isBlack, required this.selected,
      required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: selected ? AppTheme.gold.withOpacity(0.12) : AppTheme.elev,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
            color: selected ? AppTheme.gold : AppTheme.border),
      ),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Container(width: 14, height: 14,
            decoration: BoxDecoration(shape: BoxShape.circle,
                color: isBlack ? const Color(0xFF111111) : Colors.white,
                border: Border.all(color: AppTheme.border))),
        const SizedBox(width: 5),
        Text(isBlack ? 'Black' : 'White',
            style: TextStyle(
                color: selected ? AppTheme.gold : AppTheme.sec,
                fontSize: 12, fontWeight: FontWeight.w600)),
      ]),
    ),
  );
}
