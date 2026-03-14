import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

data = pd.read_csv("Ossian Audio Latency Tests.csv")
data1 = pd.read_csv("Meta Latency Tests.csv")
data2 = pd.read_csv("Even G1 Audio Latency Tests.csv")
data3 = pd.read_csv("HeyCyan Audio Latency Tests.csv")

# Get data
data = data["Latency (s)"]
data1 = data1["Total Latency (s)"]
data2 = data2["Latency (s)"]
data3 = data3["Actual Latency"]

print(np.median(data), np.std(data), np.var(data), "\n")
print(np.median(data1), np.std(data1), np.var(data1), "\n")
print(np.median(data2), np.std(data2), np.var(data2), "\n")
print(np.median(data3), np.std(data3), np.var(data3), "\n")

# Sort the data
sorted_data = np.sort(data)
sorted_data1 = np.sort(data1)
sorted_data2 = np.sort(data2)
sorted_data3 = np.sort(data3)

# Calculate CDF
cdf = np.arange(1, len(sorted_data) + 1) / len(sorted_data)
cdf1 = np.arange(1, len(sorted_data1) + 1) / len(sorted_data1)
cdf2 = np.arange(1, len(sorted_data2) + 1) / len(sorted_data2)
cdf3 = np.arange(1, len(sorted_data3) + 1) / len(sorted_data3)

# Plot CDF
fig, ax = plt.subplots(figsize=(10, 7))

ax.plot(sorted_data1, cdf1, linewidth=5, color="blue", label="RB-Meta G1")
ax.plot(sorted_data2, cdf2, linewidth=5, color="black", label="Even G1")
ax.plot(sorted_data, cdf, linewidth=5, color="red", label="Dragon")
ax.plot(sorted_data3, cdf3, linewidth=5, color="cyan", label="Cyan")

ax.set_xlabel('AI Voice Interaction Latency (s)', fontsize=28, )
ax.set_ylabel('CDF', fontsize=28)
ax.tick_params(axis="both", labelsize=28)
ax.set_xlim(0, 14)
ax.set_ylim(0, 1.0)
ax.grid(True, alpha=0.3)

ax.legend(fontsize=22, loc="lower right", frameon=True, fancybox=False, 
          edgecolor='lightgray', framealpha=0.9)

plt.tight_layout()
plt.savefig("ai_voice_interaction_latency_cdf.png", dpi=300, bbox_inches='tight')
plt.show()