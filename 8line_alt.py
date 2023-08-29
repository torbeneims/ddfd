import sys
import json
import matplotlib.pyplot as plt

def process_input_file(file_path):
    with open(file_path, "r") as file:
        file_data = file.read().strip()
        try:
            parsed_data = json.loads(file_data)
            return parsed_data["results"]
        except json.JSONDecodeError:
            print(f"Error decoding JSON data from: {file_path}")
            return []

# Process input files from command line arguments
if len(sys.argv) != 5:
    print("Usage: python3 script.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file>")
    sys.exit(1)

ddfd_file = sys.argv[1]
hyfd_file = sys.argv[2]
tane_file = sys.argv[3]
smartfd_file = sys.argv[4]

# Algorithm friendly names
algorithm_names = {
    "ddfd": "DDFD",
    "hyfd": "HyFD",
    "tane": "Distributed TANE (improved)",
    "smartfd": "SmartFD"
}

# Organize and filter data based on command type
data_files = {
    "ddfd": ddfd_file,
    "hyfd": hyfd_file,
    "tane": tane_file,
    "smartfd": smartfd_file
}

mean_runtimes = {}
stddevs = {}
r_values = {}

for command_type, file_path in data_files.items():
    results = process_input_file(file_path)
    filtered_results = [result for result in results if command_type in result["command"]]
    mean_runtimes[command_type] = [result["mean"] for result in filtered_results]
    stddevs[command_type] = [result["stddev"] for result in filtered_results]
    r_values[command_type] = [int(result["parameters"]["r"]) * 1000 for result in filtered_results]

# Create the line graph with error bars for each command type
for command_type, _ in data_files.items():
    plt.errorbar(r_values[command_type], mean_runtimes[command_type], yerr=stddevs[command_type], fmt='o-', label=f"{algorithm_names[command_type]}")

# Mark the 5x values with red stars
for command_type, _ in data_files.items():
    x_values = r_values[command_type]
    for x_val in x_values:
        if x_val % 5000 == 0:
            index = x_values.index(x_val)
            plt.plot(x_val, mean_runtimes[command_type][index], 'r*', markersize=10)

plt.xlabel("# Rows")
plt.ylabel("Mean Runtime [s]")
plt.title("Runtime on NCVoter with 17 columns")
plt.legend()
plt.grid(True)

# Set log scale for x-axis
plt.xscale('log')

# Set x-axis ticks for 1k, 10k, 100k, and 1M
plt.xticks([1000, 10000, 100000, 1000000], ["1k", "10k", "100k", "1M"])

# Save the graph as a PNG image
plt.savefig("runtime_vs_r.png")

# Show the plot
plt.show()
