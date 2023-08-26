#!/bin/bash
# Run from scripts/data as "sh ../../make_datasets.sh"
# If execution fails, make sure you are in dev shell: ../../nix-portable nix develop ../..
# Use the python script first to get Uniprot data: python ../../download_uniprot.py 1000 50

#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50 ncvoter.csv > ncvoter50c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40,41,42,43,44,45 ncvoter.csv > ncvoter45c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35,36,37,38,39,40 ncvoter.csv > ncvoter40c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23,25,29,31,34,35 ncvoter.csv > ncvoter35c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18,19,20,21,22,23 ncvoter.csv > ncvoter30c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9,10,11,12,17,18 ncvoter.csv > ncvoter25c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26,1,2,9 ncvoter.csv > ncvoter20c.csv
#cut -f 4,3,6,7,5,8,32,30,27,28,13,14,15,16,24,33,26 ncvoter.csv > ncvoter17c.csv
#cut -f 4,3,6,5,32,30,27,28,13,14,15,16,24,33,26 ncvoter.csv > ncvoter15c.csv
#cut -f 4,3,6,5,32,30,27,28,33,26 ncvoter.csv > ncvoter10c.csv
#cut -f 3,6,5,32,30 ncvoter.csv > ncvoter5c.csv
#
#cp uniprot1000kr50c.csv uniprot50c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45 uniprot1000kr50c.csv > uniprot45c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40 uniprot45c.csv > uniprot40c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35 uniprot40c.csv > uniprot35c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 uniprot35c.csv > uniprot30c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25 uniprot35c.csv > uniprot25c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 uniprot25c.csv > uniprot20c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15 uniprot20c.csv > uniprot15c.csv
#cut -f 1,2,3,4,5,6,7,8,9,10 uniprot15c.csv > uniprot10c.csv
#cut -f 1,2,3,4,5 uniprot10c.csv > uniprot5c.csv

# rows
line_counts=(1 2 5 10 20 50 100 200 500 1000)
col_counts=(5 10 15 17 20 25 30 35 40 45 50)
generate_subsets() {
    local base_filename="$1"
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
generate_subsets "uniprot" "|"