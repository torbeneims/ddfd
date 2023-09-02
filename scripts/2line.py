import sys
import json
import matplotlib.pyplot as plt
import numpy as np

output_prefix = sys.argv[1] if len(sys.argv) >= 2 else "scalability"

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

# Define a function for common settings
def set_common_settings():
    plt.xlabel('Number of Threads')
    plt.ylabel('Runtime [s] (Log Scale)')
    #plt.title('Scalability with the number of threads for different space-partition factors and job values')
    plt.legend()
    plt.grid(True)
    plt.yscale('log')
    y_steps = [2, 4, 6, 8, 10, 20, 30, 40, 50, 60]
    plt.gca().set_yticks(y_steps)
    plt.gca().set_yticklabels(map(lambda y: f"{y}s", y_steps))

# Function to save a plot with filtering
def save_filtered_plot(filter_func, output):
    plt.figure(figsize=(10, 6))
    
    for i, ((s_value, j_value), group) in enumerate(grouped_data.items()):
        if filter_func(s_value, j_value):
            color = color_map(i)
            
            t_values = np.array(group["t_values"])
            
            filtered_values = filter_func(s_value, j_value)
            if "time" in filtered_values:
                mean_time_values = np.array(group["mean_time_values"])
                stdev_time_values = np.array(group["stdev_time_values"])
                plt.errorbar(t_values, mean_time_values, yerr=stdev_time_values, marker='.', label=f'Total Runtime (s={s_value}, j={j_value})', color=color)
            
            if "max" in filtered_values:
                mean_max_values = np.array(group["mean_max_values"])
                stdev_max_values = np.array(group["stdev_max_values"])
                plt.errorbar(t_values, mean_max_values, yerr=stdev_max_values, marker='.', linestyle='dashed', label=f'Max Single Job Time (s={s_value}, j={j_value})', color=color, alpha=0.5)
    
    set_common_settings()
    plt.legend()
    plt.savefig(f"{output}.pgf", format='pgf')
    plt.savefig(f"{output}.png", format='png')
    plt.show()

# Filter function: Multiple jobs per thread (s == 0)
def filter_multiple_jobs(s, j):
    if s == 0:
        return {"time", "max"}
    return set()

save_filtered_plot(filter_multiple_jobs, f"{output_prefix}_jobs")

# Filter function: Space partitioning (s != 0)
def filter_space_partitioning(s, j):
    if s != 0:
        return {"time", "max"}
    if s == 0 and j == 1:
        return {"max"}
    return set()

save_filtered_plot(filter_space_partitioning, f"{output_prefix}_both")

# Filter function: Many jobs on many threads (s == 0 or j == 1)
def filter_many_jobs_and_threads(s, j):
    if s == 0 or j == 1:
        return {"time"}
    return set()

save_filtered_plot(filter_many_jobs_and_threads, f"{output_prefix}_three")
