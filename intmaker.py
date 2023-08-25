import sys
import csv

def build_hash_maps(data, delimiter):
    hash_maps = {}
    for row in data:
        for col_idx, value in enumerate(row):
            if col_idx not in hash_maps:
                hash_maps[col_idx] = {}
            if value not in hash_maps[col_idx]:
                hash_maps[col_idx][value] = len(hash_maps[col_idx])
    return hash_maps

def replace_with_index(data, hash_maps):
    for row_idx, row in enumerate(data):
        for col_idx, value in enumerate(row):
            data[row_idx][col_idx] = hash_maps[col_idx][value]

def main(delimiter):
    data = []
    header = None

    for line in sys.stdin:
        row = line.strip().split(delimiter)
        if header is None:
            header = row
        else:
            data.append(row)

    hash_maps = build_hash_maps(data, delimiter)
    replace_with_index(data, hash_maps)

    print(delimiter.join(header))
    for row in data:
        print(delimiter.join(str(val) for val in row))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <delimiter>")
        sys.exit(1)

    delimiter = sys.argv[1]
    main(delimiter)
