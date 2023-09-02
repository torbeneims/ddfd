import sys
import json
import matplotlib.pyplot as plt

all_labels = []

meta_lines, meta_labels = [], []





# Process input files from command line arguments
if len(sys.argv) <5:
    print("Usage: python3 script.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> <y_param> [output_file]")
    sys.exit(1)

ddfd_file = sys.argv[1]
hyfd_file = sys.argv[2]
tane_file = sys.argv[3]
smartfd_file = sys.argv[4]
X_PARAM = sys.argv[5]

output = sys.argv[6] if len(sys.argv) >= 7 else "bfhd_runtime_over_r"

X_SCALE = 1000 if X_PARAM == 'r' else 1

# Algorithm friendly names
algorithm_names = {
    "ddfd": "DDFD",
    "hyfd": "HyFD",
    "tane": "TANE*",
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
x_values = {}
timeouts_x = {}
timeouts_y = {}

EXIT_TIMEOUT = 124
Y_PARAM = "mean"

ax2 = None
isNCV = False
def plot_metadata():
    # Read additional metadata from stdin
    metadata_input = sys.stdin.read().strip()
    try:
        metadata = json.loads(metadata_input)
    except json.JSONDecodeError:
        print("Error decoding JSON metadata from stdin")
        sys.exit(1)

    # Extracting metadata for FDs
    metadata_x_values = [entry["meta"][X_PARAM] * X_SCALE for entry in metadata]
    metadata_fds = [entry["meta"]["fds"] for entry in metadata]

    # Plotting metadata FDs on a new axis
    ax2 = plt.gca().twinx()
    ax2.plot(metadata_x_values, metadata_fds, linestyle='dashed', color='black', alpha=0.5, label="FDs")
    ax2.set_ylabel("Number of FDs")
    all_labels.append("Number of FDs")

    if max(metadata_fds) < 400:
        isNCV = True

    print(f"Adding fds {metadata_fds}")
    
    ax2.tick_params(axis='y')

    plt.yscale('log')

    return ax2



def process_input_file(file_path):
    with open(file_path, "r") as file:
        file_data = file.read().strip()
        try:
            parsed_data = json.loads(file_data)
            return parsed_data["results"]
        except json.JSONDecodeError:
            print(f"Error decoding JSON data from: {file_path}")
            return []

for algorithm, file_path in data_files.items():
    results = process_input_file(file_path)
    filtered_results = [result for result in results if algorithm in result["command"]]
    mean_runtimes[algorithm] = [result[Y_PARAM] for result in filtered_results if 0 in result["exit_codes"] and len(set(result["exit_codes"])) == 1]
    timeouts_y[algorithm] = [result[Y_PARAM] for result in filtered_results if EXIT_TIMEOUT in result["exit_codes"]]

    stddevs[algorithm] = [result["stddev"] for result in filtered_results if 0 in result["exit_codes"] and len(set(result["exit_codes"])) == 1]

    x_values[algorithm] = [int(result["parameters"][X_PARAM]) * X_SCALE for result in filtered_results if 0 in result["exit_codes"] and len(set(result["exit_codes"])) == 1]
    timeouts_x[algorithm] = [int(result["parameters"][X_PARAM]) * X_SCALE for result in filtered_results if EXIT_TIMEOUT in result["exit_codes"]]


max_y = max([max(y) for y in mean_runtimes.values() if len(y) > 0])

# Create the line graph with error bars for each command type

ax = plt.figure().add_subplot(111)
for algorithm, _ in data_files.items():

    ax.errorbar(x_values[algorithm], mean_runtimes[algorithm], yerr=stddevs[algorithm], fmt='o-', label=f"{algorithm_names[algorithm]}")
    #plt.scatter(timeouts_x[algorithm], timeouts_y[algorithm], marker='x')  # 'rx' represents red x markers

    # Add red star marker to the last value if a timeout value exists
    #if timeouts_x[algorithm]:
    #    plt.plot(x_values[algorithm][-1], mean_runtimes[algorithm][-1] *1.3, '1', markersize=10)



#for algorithm, _ in data_files.items():
##    for i,x in enumerate(timeouts_x[algorithm]):
 #       plt.plot(x, timeouts_y[algorithm][i], 'r*', markersize=10)

#for algorithm, _ in data_files.items():
#    for i,x in enumerate(timeouts_x[algorithm]):
#        if x % 10 == 0:
#            index = i
#            plt.plot(x, mean_runtimes[command_type][index], 'r*', markersize=10)


if X_PARAM == "c":
    plt.xlabel("Number of Columns")
    plt.ylabel("Mean Runtime [s] (Log Scale)")

    # Set log scale for x-axis
    #plt.xscale('log')
    plt.yscale('log')

    # Set x-axis ticks for 1k, 10k, 100k, and 1M
    #plt.xticks([1000, 10000, 100000, 1000000], ["1k", "10k", "100k", "1M"])

if X_PARAM == "r":
    plt.xlabel("Number of Rows (Log Scale)")
    plt.ylabel("Mean Runtime [s] (Log Scale)")

    # Set log scale for x-axis
    plt.xscale('log')
    plt.yscale('log')

    #if isNCV:
    #    plt.yticks([200, 300], [200, 300])
    #else:
    #    plt.yticks([500, 1000], [500, 1000])


    # Set x-axis ticks for 1k, 10k, 100k, and 1M
    plt.xticks([1000, 10000, 100000, 1000000], ["1k", "10k", "100k", "1M"])


ax2 = plot_metadata()
meta_lines, meta_labels = ax2.get_legend_handles_labels()


# Combine labels from both parts of the code
#for algorithm, _ in data_files.items():
#    all_labels.append(f"{algorithm_names[algorithm]}")

ax.grid(True)
lines, labels = ax.get_legend_handles_labels()
plt.legend(lines + meta_lines, labels + meta_labels, loc='upper left')  # Combine the labels from both parts of the code



# Save the graph as a PNG image
plt.savefig(f"{output}.pgf", format='pgf')
plt.savefig(f"{output}.png", format='png')

# Show the plot
plt.show()
