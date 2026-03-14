# AI Voice & Image Interaction — Plotting and Analysis

This folder contains scripts and CSV data used to analyze latency for audio/voice interaction and to produce CDFs and breakdown plots used in the study.

## What's in this folder

- `audio_breakdown_latency_test.py` — grouped/stacked bar plot of latency component medians (G2P, Network, VAD, STT, LLM, TTS)
- `audio_llm_model_latency_test.py` — CDF comparison of LLM model total latencies (GPT-4o, GPT-4o-mini, Claude, Gemini, etc.)
- `audio_total_latency_test.py` — CDF comparison across different smart-glass devices / configurations
- `audio_tts_model_latency_test.py` — test for TTS model latency comparisons
- Multiple CSV files containing measured latency data for different models and devices, for example:
	- `GPT-4o Total Latency Tests.csv`
	- `GPT-4o-mini Total Latency Tests.csv`
	- `Claude Haiku Total Latency Tests.csv`
	- `Claude Sonnet Total Latency Tests.csv`
	- `Gemini Total Latency Tests.csv`
	- `Meta Latency Tests.csv`
	- ... (see folder for complete list)

## Requirements

Install the common Python packages used by the scripts:

```bash
python -m pip install pandas numpy matplotlib seaborn
```

If you use VS Code or Jupyter you can run the scripts there; no additional libraries are required for basic plotting.

## How to run

From this folder run the desired script with your Python interpreter. Examples:

```bash
# Plot the latency component breakdown (bar plot)
python audio_breakdown_latency_test.py

# Plot LLM model latency CDFs
python audio_llm_model_latency_test.py

# Plot device CDF comparisons
python audio_total_latency_test.py
```

Each script reads CSV files from the current directory. If a script fails to find a CSV, check the filename in the script or move the CSV into this folder.

## Saving plots

By default these scripts call `plt.show()` and do not save PNG files. To save images automatically, either:

1. Edit the script and add a `plt.savefig('filename.png', dpi=150, bbox_inches='tight')` call before `plt.show()`; or
2. Run the script and redirect the display to a headless backend and save using `matplotlib` configuration, for example:

```bash
# Example: save figure by modifying the script or using an environment variable
python -c "import matplotlib; matplotlib.use('Agg'); import runpy; runpy.run_path('audio_llm_model_latency_test.py')"
```


