# Live AI Interaction - Plot Generation Guide

This folder contains notebooks and data for analyzing and visualizing Meta AI performance metrics including latency and bitrate.

## Folder Structure

```
live_ai_interaction/
├── Meta_AI_Data/          # Raw data and notebooks for Meta AI analysis
├── Plots/                 # Generated visualization plots
└── README.md             # This file
```

## Generated Plots

Located in the `Plots/` directory:

- **Combined_Throughput_Plot.png** - Visualization of bitrate/throughput data across different test conditions (Figure 15a)
- **Meta_AI_Latency_CDF.png** - Cumulative Distribution Function plot of latency measurements (Figure 15b)

## How to Generate the Plots

### Prerequisites

Ensure you have the following Python packages installed:
```bash
pip install pandas matplotlib numpy openpyxl
```

### Meta AI Bitrate Plot (Figure 15a)

**Notebook:** `Meta_AI_Data/Meta_AI_Bitrate_Plot_Notebook.ipynb`

**Steps:**
1. Navigate to the `Meta_AI_Data/` folder
2. Open `Meta_AI_Bitrate_Plot_Notebook.ipynb` in Jupyter or VS Code
3. Run all cells (Shift+Enter or click "Run All")
4. Output plot: `Plots/Combined_Throughput_Plot.png`

### Meta AI Latency CDF Plot (Figure 15b)

**Notebook:** `Meta_AI_Data/Meta_AI_Latency Plot_Notebook.ipynb`

**Steps:**
1. Navigate to the `Meta_AI_Data/` folder
2. Open `Meta_AI_Latency Plot_Notebook.ipynb` in Jupyter or VS Code
3. Run all cells (Shift+Enter or click "Run All")
4. Output plot: `Plots/Meta_AI_Latency_CDF.png`

## Data Sources

All raw data used by the notebooks is stored in `Meta_AI_Data/`:

- **Meta_AI_Latency.xlsm** - Excel spreadsheet with latency measurements
- **Test*.pcapng** - Packet capture files from test runs
- **Test*.txt** - Log and annotation files
- **Test*.csv** - CSV formatted test data
- **PS2_Annotations_test.csv** - Annotation metadata
- **G2P_annotations_test.txt** - Additional annotations

## Running Notebooks

### Using VS Code
1. Open the notebook file
2. Click "Run All" button or execute cells sequentially with Shift+Enter

### Using Jupyter CLI
```bash
cd live_ai_interaction/Meta_AI_Data
jupyter notebook Meta_AI_Bitrate_Plot_Notebook.ipynb
jupyter notebook "Meta_AI_Latency Plot_Notebook.ipynb"
```

## Notes

- Ensure all data files are in the `Meta_AI_Data/` directory before running notebooks
- Plots are automatically saved to the `Plots/` directory
- Some notebooks may require adjustments to data file paths depending on your system configuration
