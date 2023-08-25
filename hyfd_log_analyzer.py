import sys
import re
import json

def analyze_fd_log(log_content):
    mismatched_benchmarks = []
    result = []

    benchmark_segments = re.split(r"\n(?=Benchmark \d+:)", log_content)
    
    for segment in benchmark_segments:
        benchmark_match = re.search(r"Benchmark (\d+):", segment)
        if benchmark_match:
            benchmark_number = benchmark_match.group(1)
            fd_count_matches = re.findall(r"... done! \((\d+) FDs\)", segment)  # Find all matches
            unique_fd_counts = set(fd_count_matches)
            if len(unique_fd_counts) > 1:
                mismatched_benchmarks.append(benchmark_number)
            
            # Extract dataset information from the benchmark line
            dataset_match = re.search(r"Benchmark .*?(data/\w+\d+kr\d+c\.csv)", segment)
            if dataset_match:
                dataset_filename = dataset_match.group(1)
                rows, columns, dataset = extract_info_from_filename(dataset_filename)
                
                # Create the dataset info dictionary
                dataset_info = {
                    "meta": {
                        "r": rows,
                        "c": columns,
                        "dataset": dataset,
                        "fds": int(fd_count_matches[0])
                    }
                }
                result.append(dataset_info)
    
    return result, mismatched_benchmarks

def extract_info_from_filename(filename):
    match = re.match(r"data/(\w+?)(\d+)kr(\d+)c\.csv", filename)
    if match:
        r = int(match.group(2))
        c = int(match.group(3))
        dataset = match.group(1)
        return r, c, dataset
    else:
        return None, None, None

if __name__ == "__main__":
    # Read log content from stdin
    log_content = sys.stdin.read()
    
    # Analyze the FD-related information
    result, mismatched_benchmarks = analyze_fd_log(log_content)
    
    # Print mismatched benchmarks
    if mismatched_benchmarks:
        print("Mismatched Benchmarks:")
        for benchmark in mismatched_benchmarks:
            print(f"Benchmark: {benchmark}")
    else:
        print(json.dumps(result, indent=2))
