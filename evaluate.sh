# This script evaluates the DDFD algorithm on the following datasets:
#      Name     |    Type    | #columns  |   #rows 
# --------------|------------|-----------|----------
#    Uniprot    | real-world |    223    |   1,000
#    NCVoter    | real-world |    19     |   1,000     
#    Adult      | real-world |    14     |   48,842
#    Homicide   | real-world |    24     |
#    Flight     | real-world |    20     |  500,000
#    fd-reduced | synthetic  |    30     |  250,000
# TCP-H lineitem| synthetic  |
#    Uniprot2   | real-world |    24     |  569,793  

 
# Downloads for reproducability (most links are Chrome hightlight links):
# Uniprot: https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#:~:text=%3E1545-,uniprot,-uniprot.org
# Adult: https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#:~:text=12-,adult,-uci
# Homicide: https://www.kaggle.com/murderaccountability/homicide-reports/data (account required)
# fd-reduceed: https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#:~:text=3-,fd%2Dreduced%2D30,-dbtesma
# NCVoter: https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#:~:text=0-,ncvoter,-alt.ncsbe.gov
# Flight: https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#:~:text=982631-,flight,-bts.gov
# TPC-H lineitem: 



# Uniprot2: https://rest.uniprot.org/uniprotkb/stream?fields=accession%2Creviewed%2Cid%2Cprotein_name%2Cgene_names%2Corganism_name%2Clength%2Corganelle%2Cfragment%2Cerror_gmodel_pred%2Cft_var_seq%2Ccc_alternative_products%2Cmass%2Ccc_rna_editing%2Ccc_polymorphism%2Cft_non_ter%2Cft_non_std%2Cft_non_cons%2Cft_variant%2Ccc_mass_spectrometry%2Csequence%2Ccc_sequence_caution%2Cft_conflict%2Cft_unsure%2Csequence_version&format=tsv&query=%28*%29+AND+%28reviewed%3Atrue%29
# This includes all reviewd records and the columns Entry Name, Gene Names, Organsm, Protein names, all columns in the sequences section as well as reviewd
# 
# 
# Test 1 shows the runtime on fd-reduced over multiple cores along with the longest job
# Test 2 compares the runtimes of all datasets over number of threads
# Test 3 compares the performance on the ncvoter dataset over number of columns with a baseline of 8 cores (also include the number of deps)
# Test 4 compares the performance on the ncvoter dataset over number of rows with a baseline of 8 cores (also include the number of deps)
# Test 5 compares the performance on uniprot with 10 and 20 columns for 1,4 and 8 cores
# Test 6 compares the runtime of ncvoter with hyfd

# Test A compares the runtime over 1-30 (interval 1) threads and s=1,2,3 on ncv1000000r17c with the max job times

# Test B/J measures the runtime of DDFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on ncv17c @ t=8
# Test D/L measures the runtime of HyFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on ncv17c @ t=8
# Test F/N measures the runtime of TANE* over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on ncv17c @ t=8
# Test H/P measures the runtime of SmartFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on ncv17c @ t=8
# Test C/K measures the runtime of DDFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on lineitem?c @ t=8
# Test E/M measures the runtime of HyFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on lineitem?c @ t=8
# Test G/O measures the runtime of TANE* over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on lineitem?c @ t=8
# Test I/Q measures the runtime of SmartFD over 1k 2k 5k 10k 20k 50k 100k 200k 500k 1M rows with on lineitem?c @ t=8

# Tests J-Q measure the runtime of DDFD over 5 10 15 20 25 30 35 40 45 50 cols as with tests B-I over 100k rows

# Test R measures DDFDs runtime on 100k17r of ncv, lineitem, uniprot, adult

# Test commands:
# First, make sure the spark python packages in flake.nix are commented out, then join nix shell:
#> cd dfd/scripts; ../../nix-portable nix develop
# For running commands that require spark (as indicated):
# Create a screen for running the command:
#> screen -RR -D hyperfine
# Start the master (in background):
#> cd dfd; ../nix-portable nix develop -c master &
# Start the clients (in background):
#> sh 8_spark_workers.sh
# Then run your command and clean up by going into htop and killing everything spark

# A
hyperfine -m 3 -L t 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 -L s 0,1,2 "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data//ncvoter10000r17c.csv -t {t} -s {s} -j 1 p" --show-output --export-json result2.json > result2.log

# --- NCVoter ---
#B
hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
    "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/ncvoter{r}kr{c}c.csv -t 8 -s 0 -j 1 p" \
    --show-output --export-json result3.json > result3.log

#F (requires spark)
hyperfine -m 3 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
    "timeout 3h sh run_dist_tane.sh data/ncvoter{r}kr{c}c_int.json"\
    --show-output --export-json result5.json > result5.log
#H (requires spark)
hyperfine -m 3 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
     "timeout 3h sh run_spark_smartfd.sh data/ncvoter{r}kr{c}c_int.json \"\t\" data/ncvoter{r}kr{c}c.csv" \
      --show-output --export-json result6.json > result6.log
#D
hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
    "timeout 3h taskset -c 0-7 sh run_hyfd.sh \"data/ncvoter{r}kr{c}c.csv --separator \\t\""\
    --show-output --export-json result7.json > result7.log

# --- Lineitem ---
#C,E
hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
    "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/lineitem{r}kr{c}c.csv -t 8 -s 0 -j 1 p" \
    "timeout 3h taskset -c 0-7 sh run_hyfd.sh \"data/lineitem{r}kr{c}c.csv --separator \\t\""\
    --show-output --export-json result13.json > result13.log

#G,I (require spark)
hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
    "timeout 3h sh run_dist_tane.sh data/lineitem{r}kr{c}c_int.json"\
    "timeout 3h sh run_spark_smartfd.sh data/lineitem{r}kr{c}c_int.json \"\t\" data/lineitem{r}kr{c}c.csv" \
    --show-output --export-json result14.json > result14.log

# ===== Columns =====
# --- NCVoter ---
#J,L
hyperfine -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
    "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/ncvoter{r}kr{c}c.csv -t 8 -s 0 -j 1 p" \
    "timeout 3h taskset -c 0-7 sh run_hyfd.sh \"data/ncvoter{r}kr{c}c.csv --separator \\t\""\
    --show-output --export-json result8.json > result8.log

#N (require spark)
hyperfine -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
    "timeout 3h sh run_spark_smartfd.sh data/ncvoter{r}kr{c}c_int.json \"\t\" data/ncvoter{r}kr{c}c.csv" \
    --show-output --export-json result9.json > result9.log
#P (requires spark) 
hyperfine -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
    "timeout 3h sh run_dist_tane.sh data/ncvoter{r}kr{c}c_int.json"\
    --show-output --export-json result10.json > result10.log
# --- Lineitem ---
#K,M
hyperfine -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
    "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/lineitem{r}kr{c}c.csv -t 8 -s 0 -j 1 p" \
    "timeout 3h taskset -c 0-7 sh run_hyfd.sh \"data/lineitem{r}kr{c}c.csv --separator \\t\""\
    --show-output --export-json result11.json > result11.log

#O,Q (require spark)
hyperfine -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
    "timeout 3h sh run_dist_tane.sh data/lineitem{r}kr{c}c_int.json"\
    "timeout 3h sh run_spark_smartfd.sh data/lineitem{r}kr{c}c_int.json \"\t\" data/lineitem{r}kr{c}c.csv" \
    --show-output --export-json result12.json > result12.log

# (reverse lookup)
wget https://hpi.de/fileadmin/user_upload/fachgebiete/naumann/projekte/repeatability/DataProfiling/FD_Datasets/ncvoter_1001r_19c.csv 

# (unused reverse lookup of result4)
hyperfine -m 3 -L r 1000,2000,5000,10000,20000,50000,100000,200000,500000,1000000 -L c 17 \
    "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/lineitem{r}r{c}c.csv -t 8 -s 0 -j 1 p" \
    --show-output --export-json result4.json > result4.log
# (reverse lookup)
hyperfine -m 2 -M 2 -L t 1,16 "timeout 3h java -ea -jar algorithms/ddfd.jar -i data/adult.csv -t {t} -s 0 -j 1 p" --show-output --export-json result0.json > result0.log
hyperfine -m 2 -M 2 -L t 1,16 "timeout 3h java -Xms256g -Xmx256G -ea -jar algorithms/ddfd.jar -i data/ncvoter.csv -t {t} -s 0 -j 1 p" --show-output --export-json result1.json > result1.log


# Collect results
# First uncomment the spark python packages in flake.nix, then join nix shell:
#> cd ~/dfd/scripts; ../../nix-portable nix develop ..

# Usage: cat input.json | python 2line.py [output_file]
cat result2.log | node ../log-analyzer2.js | python ../2line.py a_scalability.png
# Usage: python 3line.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> [output_file]
cat result7.log | python ../hyfd_log_analyzer.py | python3 ../3line.py result3.json result7.json result5.json result6.json bfhd_runtime_over_r.png
echo | python3 ../8line.py result8.json result8.json result8.json result8.json jlnp_runtime_over_c.png

