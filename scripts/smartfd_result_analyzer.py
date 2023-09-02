import os
import re
import json

def count_fds(line):
    _, rhs = line.split(":")
    fds = rhs.split(",")
    return len(fds)

def extract_meta_info(directory_name):
    # Extract "r", "c", and "dataset" from the directory name using regular expressions
    match = re.search(r'(\w+?)(\d+)kr(\d+)c', directory_name)
    if match:
        r = int(match.group(2))
        c = int(match.group(3))
        dataset = match.group(1)
        return {"r": r, "c": c, "dataset": dataset}
    return None

def process_file(file_path):
    total_fds = 0
    
    with open(file_path, "r") as file:
        lines = file.readlines()
        for line in lines:
            line = line.strip()
            if line:
                fds_count = count_fds(line)
                total_fds += fds_count
    
    return total_fds

def main():
    base_dir = "."  # Replace with the base directory where subdirectories are located
    
    results = []
    for root, dirs, files in os.walk(base_dir):
        for directory in dirs[:]:
            meta_info = extract_meta_info(directory)
            if meta_info is None:
                dirs.remove(directory)
                continue
            
            file_path = os.path.join(root, directory, "part-00000")
            if not os.path.exists(file_path):
                dirs.remove(directory)
                continue
            
            fds_count = process_file(file_path)
            meta_info["fds"] = fds_count
            results.append({"meta": meta_info})
    
    print(json.dumps(results, indent=2))

if __name__ == "__main__":
    main()
