import json
import sys
from tabulate import tabulate

def compare_fds(file_paths):
    data_dict = {}  # Initialize the data dictionary

    for file_path in file_paths:
        with open(file_path, "r") as file:
            data = json.load(file)
            valid_data = [entry for entry in data if "meta" in entry and "r" in entry["meta"] and "c" in entry["meta"] and "dataset" in entry["meta"] and "fds" in entry["meta"]]
            if valid_data:
                for entry in valid_data:
                    identifier = (entry["meta"]["r"], entry["meta"]["c"], entry["meta"]["dataset"])
                    fds_value = entry["meta"]["fds"]
                    if identifier not in data_dict:
                        data_dict[identifier] = {}
                    data_dict[identifier][file_path] = fds_value

            else:
                print(f"Warning: {file_path} does not contain valid meta data and will be skipped.")

    mismatches = []
    matches = []
    insufficient_values = []

    for identifier, file_fds in data_dict.items():
        unique_fds = set(fds for fds in file_fds.values())
        if len(unique_fds) > 1:
            mismatches.append((identifier, file_fds))
        elif len(file_fds.values()) > 1:
            matches.append((identifier, list(unique_fds)[0]))
        else:
            insufficient_values.append((identifier, list(unique_fds)[0]))

    if mismatches:
        print("Mismatches:")
        table_data = []

        for identifier, file_fds in mismatches:
            row = [f"({identifier[0]}, {identifier[1]}, {identifier[2]})"]
            for file_path in file_paths:
                row.append(str(file_fds.get(file_path, "")))
            table_data.append(row)

        headers = ["(krows, cols, dataset))"] + file_paths
        table_data = sorted(table_data)
        print(tabulate(table_data, headers=headers, tablefmt="grid"))

    if matches:
        print("Matches:")
        matches_data = []

        for identifier, fds_value in matches:
            matches_data.append([f"({identifier[0]}, {identifier[1]}, {identifier[2]})", fds_value])

        print(tabulate(matches_data, headers=["(krows, cols, dataset))", "fds"], tablefmt="grid"))
    if matches:
        print("The following results are only present once:")
        matches_data = []

        for identifier, fds_value in insufficient_values:
            matches_data.append([f"({identifier[0]}, {identifier[1]}, {identifier[2]})", fds_value])

        print(tabulate(matches_data, headers=["(krows, cols, dataset))", "fds"], tablefmt="grid"))

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python script.py <file1.json> <file2.json> ...")
    else:
        file_paths = sys.argv[1:]
        compare_fds(file_paths)
