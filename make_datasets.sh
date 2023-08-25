#!/bin/bash
# run from scripts/data as "sh ../../make_datasets.sh"
# Use the python script first to get Uniprot data: python ../../download_uniprot.py 1000 50

cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ncvoter.csv > ncvoter50c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40,41,42,43,44,45 ncvoter.csv > ncvoter45c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40 ncvoter.csv > ncvoter40c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35 ncvoter.csv > ncvoter35c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23 ncvoter.csv > ncvoter30c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18 ncvoter.csv > ncvoter25c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9 ncvoter.csv > ncvoter20c.csv
cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26 ncvoter.csv > ncvoter17c.csv
cut -f 4,3,6,5,32,30,27,28,13,14,15,16,24,33,26 ncvoter.csv > ncvoter15c.csv
cut -f 4,3,6,5,32,30,27,28,33,26 ncvoter.csv > ncvoter10c.csv
cut -f 3,6,5,32,30 ncvoter.csv > ncvoter5c.csv

# rows
generate_subsets() {
    local base_filename="$1"
    line_counts=(1 2 5 10 20 50 100 200 500 1000)
    col_counts=(5 10 15 17 20 25 30 35 40 45 50)
    local delimiter="$2"

    for cols in "${col_counts[@]}"; do
        local input_file="${base_filename}${cols}c.csv"
        for rows in "${line_counts[@]}"; do
            output_file="${base_filename}${rows}kr${cols}c.csv"
            echo making "$output_file"
            head --lines "$((rows*1000 + 1))" "$input_file" > "$output_file"

            int_file="${base_filename}${rows}kr${cols}c_int.csv"
            echo making "$int_file"
            cat "$output_file" | python ../../intmaker.py "$delimiter" > "$int_file"
            echo creating json file
            python ../../sparkData.py "$int_file"  "	"
        done
    done
}

generate_subsets "ncvoter" "	"
#generate_subsets "lineitem" "${line_counts[@]}" "${col_counts[@]}" ","