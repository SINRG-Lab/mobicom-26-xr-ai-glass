# A First Look at Multimodal Mobile Application Performance on XR and AI Smart Glasses

<p align="center">
  <img src="generic_diagram.png" alt="Communication pipeline of smart glasses" width="700"/>
</p>

<p align="center">
  <em>Communication pipeline of smart glasses: short-range G2P/P2G links to the companion device and P2S/S2P links to cloud servers.</em>
</p>

---

## Overview

This repository contains the official implementation and supplementary materials for our paper:

> **"A First Look at Multimodal Mobile Application Performance on XR and AI Smart Glasses"**  
> Majd Khalaf, Mathew Garcia, Mayank Chadha, Mallesham Dasari  
> Northeastern University, Boston, Massachusetts, USA  
> MobiCom '26

We present the first in-depth measurement study across several smart glasses and core XR and AI applications. Our study dissects the end-to-end pipeline spanning the glass, companion device, and the cloud, revealing how emerging AI and video streaming applications reshape network traffic patterns, companion-device workloads, and quality of experience (QoE).

---

## Key Findings

- **Smartphones transcode glass video before upload:** Live streaming from RB-Meta G1 triggers a costly decode–upscale–re-encode pipeline on the companion phone, driving 2–3× higher latency than phone-native streaming. The phone's transcoding, not the G2P wireless link, is the dominant bottleneck.
- **Video conferencing reveals a fundamental upstream/downstream asymmetry:** The companion phone aggressively downscales incoming video for the glass display (600×600, ~20° FoV) while relaying the upstream feed without transcoding.
- **Live AI sacrifices video fidelity for continuous interaction:** Meta Live AI streams at ~300 kbps uplink, an order of magnitude lower than regular live streaming, to sustain always-on multimodal reasoning.
- **No glass meets the real-time AI interaction bar:** All glasses exhibit >1 s end-to-end latency across all five applications, far exceeding the sub-300 ms threshold for real-time interaction.
- **Glass design choices drive a 10× latency disparity:** RB-Meta G1 achieves ~1 s median voice AI latency while Cyan exceeds ~10 s.

---

## System Architecture

### Glass Platforms

| Glass | Type | Key Capabilities |
|-------|------|-----------------|
| **RB-Meta G1** | Commercial | Live streaming, AI voice/image, Live AI |
| **RB-Meta Display** | Commercial | Video conferencing (full color display), AI voice/image |
| **Even G1** | Commercial | AI voice (text display only) |
| **Cyan** | Commercial (SDK access) | AI voice/image via custom apps |
| **Dragon** | Custom prototype | Fully instrumented AI voice/image (ESP32-based) |

### Applications Studied

| # | Application | Description |
|---|------------|-------------|
| 1 | **Live Video Streaming** | Continuous camera-to-platform broadcast (Instagram, Facebook) |
| 2 | **Video Conferencing** | Two-way audiovisual calls (WhatsApp, Messenger) |
| 3 | **AI Voice Interaction** | Spoken query → cloud AI → audio response |
| 4 | **AI Voice-Image Interaction** | Spoken query + image capture → multimodal AI → audio response |
| 5 | **Live AI Interaction** | Always-on camera + mic for continuous context-aware AI |

### Measurement Diagrams

| Application | Setup |
|------------|-------|
| Live Video Streaming | <img src="Measure_liveStreaming.png" width="500"/> |
| Video Conferencing | <img src="Measured_VC_Diagram.pdf" width="500"/> |
| Live AI Interaction | <img src="Measure_liveAI.png" width="500"/> |
| AI Voice Interaction | <img src="Measure_AudioAI.png" width="500"/> |
| AI Voice-Image Interaction | <img src="Measure_ImageAI.png" width="500"/> |

### Communication Protocols

#### Smart Glasses ↔ Companion Device
- **BLE (Bluetooth Low Energy):** Control signaling and metadata exchange
- **Temp Wi-Fi Network:** High-bandwidth media import
- **Wi-Fi Direct:** Primary channel for live streaming (with BT-Classic fallback)
- **BT-Classic:** Audio streaming and image transmission for AI features

#### Companion Device ↔ Server
- **WebRTC/QUIC:** Low-latency live streaming and video conferencing
- **TCP/TLS (STT + LLM + TTS):** Speech-to-text, LLM inference, and text-to-speech for AI features

---

## Repository Structure

```
├── ai_voice_image_interaction/             # Audio & image AI interaction latency/throughput notebooks, CSVs, and plots
├── distance_power_logcat/                  # Distance and power measurement data and notebooks (logcat outputs)
├── live_ai_interaction/                    # Meta AI data, notebooks, and generated plots for Live AI tests
├── live_video_streaming-conferencing/      # Live streaming & conferencing notebooks, CPU/throughput-latency data + plots
├── power_tests/                            # Additional power test data and scripts
├── transmission_power_algorithm_Data/      # Adaptive transmission power algorithm data and notebook
├── dragon/                                 # Proprietary application framework for Dragon smart glasses
├── generic_diagram.pdf                     # Generic communication pipeline diagram
├── Measure_liveStreaming.png               # Live streaming measurement setup diagram
├── Measured_VC_Diagram.pdf                 # Video conferencing measurement setup diagram
├── Measure_liveAI.png                      # Live AI measurement setup diagram
├── Measure_AudioAI.png                     # AI voice interaction measurement setup diagram
├── Measure_ImageAI.png                     # AI voice-image interaction measurement setup diagram
└── README.md                               # This file
```

Each analysis folder typically contains Jupyter notebooks and a `Plots/` subfolder for exported figures. See folder-level READMEs for figure-to-notebook mappings.

---

## Quick Start: Regenerate Plots

### Prerequisites

- Python 3.8+
- Common libraries: `pandas`, `numpy`, `matplotlib`, `seaborn`, `openpyxl`

```bash
pip install pandas numpy matplotlib seaborn openpyxl
```

### Running Notebooks

All plots are produced by Jupyter notebooks inside the analysis folders:

```bash
# Example: regenerate Live AI plots
cd live_ai_interaction/Meta_AI_Data
jupyter notebook Meta_AI_Bitrate_Plot_Notebook.ipynb
```

Generated figures are saved to the folder's `Plots/` directory. Inspect the first code cell of each notebook to adjust data paths if needed.

---

## Reproducibility

To reproduce all figures and tables, run the notebooks in each analysis folder. Notebooks load local CSV/Excel/PCAP inputs from the same directory or a `data/` subfolder. We performed cross-platform validation (Meta on iOS vs. Android, Cyan vs. Dragon) to ensure consistency across OS, workloads, and measurement tools.

All data, scripts, and application code are available at: [github.com/SINRG-Lab/mobicom-26-xr-ai-glass](https://github.com/SINRG-Lab/mobicom-26-xr-ai-glass)

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

We thank the reviewers for their valuable feedback.

---

## Contact

For questions or issues, please open a GitHub issue or contact Majd Khalaf at khalaf.m@northeastern.edu.
