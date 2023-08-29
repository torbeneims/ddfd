import sys
import json
import matplotlib.pyplot as plt
import numpy as np

output = sys.argv[1] if len(sys.argv) >= 2 else "a_scalability.png"

# Read data from stdin
data = json.load(sys.stdin)

# Define a color map for different 's' values
color_map = plt.get_cmap('tab10')

# Group data by 's' and 'j' values
grouped_data = {}
for entry in data[1:]:
    s_value = entry["meta"]["s"]
    j_value = entry["meta"]["j"]  # Get the 'j' value
    if (s_value, j_value) not in grouped_data:
        grouped_data[(s_value, j_value)] = {"t_values": [], "mean_time_values": [], "stdev_time_values": [], "mean_max_values": [], "stdev_max_values": []}
    grouped_data[(s_value, j_value)]["t_values"].append(entry["meta"]["t"])
    grouped_data[(s_value, j_value)]["mean_time_values"].append(entry["mean"]["time"])
    grouped_data[(s_value, j_value)]["stdev_time_values"].append(entry["stdev"]["time"])
    grouped_data[(s_value, j_value)]["mean_max_values"].append(entry["mean"]["max"] / 1000)  # Divide by 1000
    grouped_data[(s_value, j_value)]["stdev_max_values"].append(entry["stdev"]["max"] / 1000)  # Divide by 1000

# Create a line plot with error bars for both mean time and mean max values
plt.figure(figsize=(10, 6))
for i, ((s_value, j_value), group) in enumerate(grouped_data.items()):
    t_values = np.array(group["t_values"])
    mean_time_values = np.array(group["mean_time_values"])
    stdev_time_values = np.array(group["stdev_time_values"])
    mean_max_values = np.array(group["mean_max_values"])
    stdev_max_values = np.array(group["stdev_max_values"])
    
    # Get color from the color map
    color = color_map(i)
    
    # Plot Mean Time with error bars
    plt.errorbar(t_values, mean_time_values, yerr=stdev_time_values, marker='+', label=f'Total Runtime (s={s_value}, j={j_value})', color=color)
    
    # Plot Max. Job Time (renamed) with error bars and reduced saturation (alpha=0.5), using the same color as Mean Time
    plt.errorbar(t_values, mean_max_values, yerr=stdev_max_values, linestyle='dashed', marker='+', label=f'Max Single Job Time (s={s_value}, j={j_value})', color=color, alpha=0.5)

plt.xlabel('# Threads')
plt.ylabel('Runtime [s] (Log Scale)')
plt.title('Scalability with the number of threads for different space-partition factors and job values')
plt.legend()
plt.grid(True)

# Set y-axis to log scale
plt.yscale('log')

# Add labels to the y-axis
y_steps = [4, 6, 8, 10, 20, 30, 40, 50, 60, 70]
plt.gca().set_yticks(y_steps)
plt.gca().set_yticklabels(map(lambda y: f"{y}s", y_steps))

# Save the plot as an image
plt.savefig(output)

# Display the plot
plt.show()
