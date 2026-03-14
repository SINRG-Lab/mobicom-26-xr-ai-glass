# Live Video Streaming & Conferencing - Plot Generation Guide

This folder contains notebooks and data for analyzing and visualizing throughput, latency, and CPU utilization metrics for live video streaming and conferencing experiments.

## Folder Structure

```
live_video_streaming-conferencing/
├── CPU_Utilization_Data/      # Notebooks and logs for CPU utilization analysis
├── Throughput-Latency_Data/   # Notebooks and raw data for throughput & latency analysis
├── Plots/                     # Generated visualization plots
└── README.md                  # This file
```

## Generated Plots

Generated plots live in the `Plots/` directory:

### Live Streaming & Conferencing
- **CDF_Throughput_RB.png** - RayBan throughput CDF (Figure 10)
- **Cross_Platforms_Live_Stream_throughput.png** - Cross-platform live streaming throughput (Figure 11)
- **Latency_RB_insta_FB_live.png** - RayBan latency for Instagram & Facebook live streaming (Figure 14)
- **insta_live_codecs_cpu_utilization.png** - CPU utilization for Instagram live streaming (Figure 12)

### Video Conferencing (Meta Display)
- **Meta_Display_Messenger_Throughput_CDF.png** - Messenger throughput CDF (Figure 17a)
- **Meta_Display_WhatsApp_Throughput_CDF.png** - WhatsApp throughput CDF (Figure 17b)
- **Latency_RB_Video_Calls.png** - RayBan video call latency (Figure 16b)

## Notebooks

### Meta Display (Video Conferencing)

- `Throughput-Latency_Data/Meta_Display_Data/Meta_Display_Throughput_Plot_Notebook.ipynb` — generates Messenger and WhatsApp throughput CDFs (Figures 17a, 17b)
- `Throughput-Latency_Data/Meta_Display_Data/Meta_Display_Latency_Plot_Notebook.ipynb` — generates video call latency plots (Figure 16b)

### Live Streaming & Cross-Platform

- `Throughput-Latency_Data/Throughput-Bar_Data/Cross-platforms-bar-plot_Notebook.ipynb` — generates cross-platform throughput bar plots (Figure 11)
- `Throughput-Latency_Data/Throughput-Latency-Live-Streaming_Data/Latency-Live-Streaming_Data/Latency_Percieved_Plot.ipynb` — generates latency plots for live streaming (Figure 14)

### CPU Utilization

- `CPU_Utilization_Data/CPU_Plot_Notebook.ipynb` — generates CPU utilization plots from codec logs (Figure 12)

## How to Generate the Plots

### Prerequisites

Install the Python packages used across these notebooks (common set):
```bash
pip install pandas matplotlib numpy openpyxl
```

Some notebooks may use additional plotting or analysis libraries; check the top cells of each notebook for exact imports.

### Example: Run a Throughput Notebook

1. Change into the folder with the notebook, for example:

```bash
cd live_video_streaming-conferencing/Throughput-Latency_Data/Meta_Display_Data
```

2. Open the notebook in Jupyter or VS Code and run all cells:

```bash
jupyter notebook Meta_Display_Throughput_Plot_Notebook.ipynb
```

3. The generated plot files will be saved to the local `Plots/` directory (or a `Plots/` subfolder next to the notebook), depending on the notebook's save path.

### Example: Run the CPU Utilization Notebook

```bash
cd live_video_streaming-conferencing/CPU_Utilization_Data
jupyter notebook CPU_Plot_Notebook.ipynb
```

## Data Sources

This directory contains many raw data files used by the notebooks. Representative examples:

- Packet captures (`*.pcapng`) and throughput CSVs in `Throughput-Latency_Data/Meta_Display_Data/`
- Perceived latency CSVs in `Throughput-Latency_Data/Throughput-Latency-Live-Streaming_Data/Latency-Live-Streaming_Data/`
- CPU logs: `CPU_Utilization_Data/insta_live_codecs_logs.txt`

## Notes and Tips

- Inspect the first code cells of each notebook to confirm data file paths and required Python imports.
- If a notebook fails to find a data file, check whether the notebook expects data in a relative path; move or symlink the files as needed.
- Ensure all raw data files are present in their respective subdirectories before running notebooks.
