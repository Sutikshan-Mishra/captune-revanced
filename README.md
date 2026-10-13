# CapTune ReVanced : unofficial HD 4.50 BTNC companion for Android v1.0.0-beta.1

An unofficial, community-built companion for the **Sennheiser HD 4.50 BTNC**: a real 10-band EQ with presets
and saved profiles, optional system-wide EQ for apps that support it, ambient listening (microphone
passthrough), connection and battery status, and honest NoiseGard guidance.
Developed by Sutikshan Mishra. Not affiliated with or endorsed by Sennheiser.

Native Android · Kotlin · Jetpack Compose · Material 3 · AndroidX Media3. minSdk 26 (Android 8.0), target 35.
Package ID is unchanged (`dev.hd450btnc.companion`, debug: `…companion.debug`), so it updates in place.

## Features and verification status

Legend : **U**: unit-tested logic · **B**: Android build verified · **D**: tested on a device.
Nothing has been built or device-tested yet (see BUILD_STATUS.md); B and D are therefore open for every row.

| Feature | Implementation | U | B | D |
|---|---|---|---|---|
| In-app 10-band EQ, presets, profiles, headroom | Media3 audio processor running RBJ biquads | ✓ | ✓ | ✓ |
| **System-wide EQ** (experimental, opt-in) | Android audio-session protocol + `DynamicsProcessing` (Android 9+) / `Equalizer` (8.x); effect-panel entry; optional legacy global mix | mapping + status ✓ | ✓ | ✓ |
| **Ambient listening** (phone mic or headset mic) | `AudioRecord` → DC block → ramped gain → limiter → `AudioTrack`, `microphone` foreground service | DSP, routing rules, state machine ✓ | ✓ | ✓ |
| Ambient gain slider, mute, reset | −30…+12 dB, default −6 dB at every start, ramped | ✓ | ✓ | ✓ |
| Bluetooth status, battery when reported | A2DP/HFP proxies, no scanning | ✓ | ✓ | ✓ |

### System-wide EQ:

Android offers no supported way for a normal app to process *all* other apps' audio. CapTune ReVanced uses the
documented route: players announce their audio session, and the EQ attaches to it. That covers only apps that
announce sessions (or that open CapTune from their own "Equalizer" button). Whether Spotify does so on your
phone is **not verified**. The app shows exactly which sessions it attached to, and whether it has control.

### Ambient listening :

- **Phone mic** (default): music keeps full quality and is mixed in; hears near the phone, not at your ears;
  Bluetooth adds delay.
- **Headset mic** (Android 12+): switches the headset to its hands-free call link (mono, call-grade; media usually
  pauses or degrades until Stop). Not verified on this headset. (Works way better on the headset mic)
- Not hearing protection. Never relies on or changes NoiseGard. Never records.

## Permissions

| Permission | Why | When |
|---|---|---|
| Nearby devices (`BLUETOOTH_CONNECT`, Android 12+) | Connection and battery status | Headphones tab |
| Microphone (`RECORD_AUDIO`) | Ambient listening only | When you press Start |
| Notifications (Android 13+) | Shows ambient/system-EQ notifications with Stop | With Start / when enabling system EQ |
| `MODIFY_AUDIO_SETTINGS` | Attach EQ effects; hands-free routing | Install-time |
| Foreground services (media playback, microphone, special use) | Player, ambient, system-EQ receiver | Only while those features run |

No location, scanning, internet, storage or analytics.


## Licence & acknowledgements

AndroidX (Compose, Material 3, Core, Activity, Lifecycle, Media3) — Apache License 2.0. EQ filters follow
R. Bristow-Johnson's Audio EQ Cookbook; pink noise uses Paul Kellet's filter. "Sennheiser", "HD 4.50 BTNC" and
"NoiseGard" are trademarks of their owners, used only to identify compatible hardware.
