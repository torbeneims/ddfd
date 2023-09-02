# This script evaluates the DDFD algorithm and related algotihms

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




>&2 echo A
# moved A down for a sec 


# Conclusions to be drawn:
# (ob) s significantly increases the runtime due to redundant work
# (ob) - until a significant number of threads is reached
# (b) blue is limited by the max single job time
# (obg) green achieves best scalability, especially with more threads where more workers idle because no jobs are left
# (bg) - max job time only differs once jobs are actually taken by concurrent workers instead of being almost completed by one worker
# (-ob) Space-partitioned max job time is around half as long
# (g/o/r + -b)Max job time of pure rhs partitioning exceeds the total runtime of combined parallization strategies 

# ===== Rows =====
# --- NCVoter ---
>&2 echo B
#hyperfine -i -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 30m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data/ncvoter{r}kr{c}c.csv -t 8 -s 0 -j 4 p" \
#   --show-output --export-json result3_2.json > result3_2.log

>&2 echo F #(requires spark)
#hyperfine -i -m 3 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 90m sh run_dist_tane.sh data/ncvoter{r}kr{c}c_int.json"\
#    --show-output --export-json result5_2.json > result5_2.log

>&2 echo H #(requires spark)
#hyperfine -i -m 3 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#     "timeout 30m sh run_spark_smartfd.sh data/ncvoter{r}kr{c}c_int.json \"\t\" {c}" \
#      --show-output --export-json result6_2.json &> result6_2.log
>&2 echo D
#hyperfine -i -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 30m taskset -c 0-7 sh run_hyfd.sh \"data/ncvoter{r}kr{c}c.csv --separator \\t\""\
#    --show-output --export-json result7.json > result7.log

# ===== Columns =====
# --- NCVoter ---

>&2 echo N #(requires spark)
#hyperfine -i -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
#    "timeout 30m sh run_spark_smartfd.sh data/ncvoter{r}kr{c}c_int.json \"\t\" {c}" \
#    --show-output --export-json result9_2.json > result9_2.log

# here's K
#TODO
#hyperfine -i -m 3 -M 4 -L r 100 -L c 25,30,35,40,45,50 \
#    "timeout 90m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data/uniprot{r}kr{c}c_int.csv -t 8 -s 0 -j 4 p" \
#    --show-output --export-json result11_4.json &> result11_4.log

>&2 echo J,L
#TODO
hyperfine -i -m 3 -M 4 -L r 100 -L c 35,40,45,50 \
    "timeout 90m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data/ncvoter{r}kr{c}c.csv -t 8 -s 0 -j 4 p" \
    --show-output --export-json result8_5b.json &> result8_5b.log
# started 20:54
#"timeout 90m taskset -c 0-7 sh run_hyfd.sh \"data/ncvoter{r}kr{c}c.csv --separator \\t\""\

>&2 echo P #(requires spark) 
#hyperfine -i -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
#    "timeout 90m sh run_dist_tane.sh data/ncvoter{r}kr{c}c_int.json"\
#    --show-output --export-json result10_3.json > result10_3.log





# Here we go, there is A:

#hyperfine -i -m 3 -L t 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 -L s 0,2 -L j 1,4 \
#    "timeout 30m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data//ncvoter100kr17c.csv -t {t} -s {s} -j {j} p" \
#    --show-output --export-json result2.json > result2.log
#hyperfine -i -m 3 -L t 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 -L s 0,4 -L j 1,8 \
#    "timeout 30m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data//ncvoter100kr17c.csv -t {t} -s {s} -j {j} p" \
#    --show-output --export-json result30.json > result30.log


# don't think we will really get further than here....











# ===== Rows =====
# --- Uniprot ---
>&2 echo C,E
#hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 90m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data/uniprot{r}kr{c}c_int.csv -t 8 -s 0 -j 4 p" \
#    --show-output --export-json result13_3.json > result13_3.log

#hyperfine -m 3 -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 90m taskset -c 0-7 sh run_hyfd.sh \"data/uniprot{r}kr{c}c_int.csv --separator ,\""\
#    --show-output --export-json result31_2.json > result31_2.log

>&2 echo G,I #(require spark)
#hyperfine -m 3 -i -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 90m sh run_dist_tane.sh data/uniprot{r}kr{c}c_int.json"\
#    --show-output --export-json result14_2.json > result14_2.log


#hyperfine -m 3 -i -M 4 -L r 1,2,5,10,20,50,100,200,500,1000 -L c 17 \
#    "timeout 90m sh run_spark_smartfd.sh data/uniprot{r}kr{c}c_int.json \",\" {c}" \
#    --show-output --export-json result32_3.json &> result32_3.log

# ===== Columns =====
# --- Uniprot ---
>&2 echo K,M
#moved K up
#hyperfine -i -m 3 -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
#    "timeout 90m taskset -c 0-7 sh run_hyfd.sh \"data/uniprot{r}kr{c}c_int.csv --separator ,\""\
#    --show-output --export-json result33_3.json > result33_3.log

>&2 echo O,Q #(require spark)
#hyperfine -m 3 -i -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
#    "timeout 90m sh run_spark_smartfd.sh data/uniprot{r}kr{c}c_int.json \",\" {c}" \
#    --show-output --export-json result34_2.json > result34_2.log

#hyperfine -m 3 -i -M 4 -L r 100 -L c 50 \
#    "timeout 90m sh run_spark_smartfd.sh data/uniprot{r}kr{c}c_int.json \",\" {c}" \
#    --show-output --export-json result34_2b.json > result34_2b.log
# =====> Fails to execute

#hyperfine -m 3 -i -M 4 -L r 100 -L c 5,10,15,20,25,30,35,40,45,50 \
#    "timeout 90m sh run_dist_tane.sh data/uniprot{r}kr{c}c_int.json"\
#    --show-output --export-json result12_3.json > result12_3.log
#hyperfine -m 3 -i -M 4 -L r 100 -L c 25,30,35,40,45,50 \
#    "timeout 90m sh run_dist_tane.sh data/uniprot{r}kr{c}c_int.json"\
#    --show-output --export-json result12_4.json > result12_4.log

# These commands are for testing whether the evaluation commands work:
#timeout 90m java -Xms256g -Xmx256G -jar algorithms/ddfd.jar -i data/uniprot1kr5c.csv -t 8 -s 0 -j 4 p
#timeout 90m taskset -c 0-7 sh run_hyfd.sh "data/uniprot1kr5c.csv --separator ,"
#timeout 90m sh run_dist_tane.sh data/uniprot1kr5c_int.json
#timeout 90m sh run_spark_smartfd.sh data/uniprot1kr50c_int.json "," 5 