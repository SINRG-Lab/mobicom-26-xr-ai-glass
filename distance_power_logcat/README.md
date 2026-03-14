
# Distance & Power (logcat) — Plotting and Analysis

This folder contains measurement data, notebooks and helper files used to analyze distance-based latency and power (battery/voltage) traces captured from devices. Notebooks generate plots (CDFs, perceived-latency plots, power vs. resolution plots, etc.) which are saved to the `Plots/` subfolders.

## Folder layout (high level)

```
distance_power_logcat/
├── Distance_Data/                # Notebooks and CSVs for distance/latency experiments
│   ├── Distance_Latency_plots.ipynb
│   ├── Percieved Latency Data.csv
│   └── ...
├── Power-Resolution_Data/        # Notebooks and raw logcat and battery CSVs
│   ├── Power_Plots_Notebook.ipynb
│   ├── Resolutions_Plots_Notebook.ipynb
│   └── battery_data_elapsed.csv
├── Plots/                        # Generated images (CDFs, throughput/latency plots, power plots)
└── README.md                     # This file
```

## Notebooks (examples)

- `Distance_Data/Distance_Latency_plots.ipynb` — generates perceived-latency plots and CDFs using `Percieved Latency Data.csv` and device subfolders
- `Power-Resolution_Data/Power_Plots_Notebook.ipynb` — generates power and battery plots from `battery_data_elapsed.csv` and logcat exports
- `Power-Resolution_Data/Resolutions_Plots_Notebook.ipynb` — generates plots showing power vs. display resolution and related comparisons

Each notebook includes data loading cells and plotting cells; inspect the first cells for any path adjustments.

## Typical generated plots and where they appear

- `distance` plots, CDFs and perceived-latency visualizations → `Distance_Data/Plots/` or top-level `Plots/`
- `power` and `battery` plots → `Power-Resolution_Data/Plots/` or top-level `Plots/`

If you prefer a single place for outputs, create a `Plots/` folder at the top level (already present) and copy/move images there after generation.

## Requirements

Install common data-science packages used by the notebooks:

```bash
python -m pip install pandas numpy matplotlib seaborn jupyter openpyxl
```

Some notebooks parse logcat text files and may use `regex`/`pandas` text parsing — those are included in the standard library or pandas.

## How to run

Open the notebook you want to run (via Jupyter or VS Code) and run all cells. Example:

```bash
cd distance_power_logcat/Distance_Data
jupyter notebook Distance_Latency_plots.ipynb
```

Or run non-interactively to execute and save outputs (useful for CI or batch runs):

```bash
cd distance_power_logcat/Distance_Data
jupyter nbconvert --to notebook --execute Distance_Latency_plots.ipynb --output Distance_Latency_plots.executed.ipynb
```

After execution the plots will be displayed inline and any explicit `plt.savefig(...)` calls will write files to the notebook directory (or `Plots/` subfolder).
