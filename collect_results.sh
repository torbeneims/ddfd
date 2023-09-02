# Collect results
# First uncomment the spark python packages in flake.nix, then join nix shell:
#cd ~/dfd/scripts; ../../nix-portable nix develop ..#pyShell

# Usage: cat input.json | python 2line.py [output_file]
cat result2.log | node ../log-analyzer2.js | python ../2line.py a_scalability || echo "Didn't create a_scalability"
# Usage: <metadata including fd counts> | python 3line.py <ddfd_file> <hyfd_file> <tane_file> <smartfd_file> [output_file]
cat result7_1.log | python3 ../hyfd_log_analyzer.py | python3 ../8line.py result3.json result7_1.json result5_2.json result6_1.json r bfhd_runtime_over_r || echo "Didn't create bfhd_runtime_over_r"
cat result8_2.log | python3 ../hyfd_log_analyzer.py | python3 ../8line.py result8_3.json result8_2.json result10_3.json result9_1.json c jlnp_runtime_over_c || echo "Didn't create jlnp_runtime_over_c"

cat result31_2.log | python ../hyfd_log_analyzer.py | python3 ../8line.py result13_2.json result31_2.json result14_2.json result32_2.json r cegi_runtime_over_r || echo "Didn't create cegi_runtime_over_r"
cat result33_3.log | python ../hyfd_log_analyzer.py | python3 ../8line.py result11_2.json result33_3.json result12_2.json result34_2.json c kmoq_runtime_over_c || echo "Didn't create kmoq_runtime_over_c"

#cd ~/dfd/scripts; ((cat result7_1.log | python3 ../hyfd_log_analyzer.py) && (cat result8_2.log | python3 ../hyfd_log_analyzer.py)) | python3 ../joinJSON.py > meta_hyfd.json
#cd ~/dfd/scripts/data; python3 ../../smartfd_result_analyzer.py > ../meta_smartfd.json
#cd ~/dfd/scripts; (cat result3_2.log && cat result8_3.log) | node ../log-analyzer2.js > meta_ddfd.json
#cd ~/dfd/scripts; (cat result5_2.log && cat result10_3.log) | python3 ../tane_log_analyzer.py > meta_tane.json
#python3 ../compareMeta.py meta_ddfd.json meta_hyfd.json meta_smartfd.json meta_tane.json 

#cd ~/dfd/scripts; ((cat result31.log | python3 ../hyfd_log_analyzer.py) && (cat result33.log | python3 ../hyfd_log_analyzer.py)) | python3 ../joinJSON.py > meta_hyfd_uniprot.json
#cd ~/dfd/scripts/data; python3 ../../smartfd_result_analyzer.py > ../meta_smartfd_uniprot.json
#cd ~/dfd/scripts; (cat result13.log && cat result11.log) | node ../log-analyzer2.js > meta_ddfd_uniprot.json
#cd ~/dfd/scripts; (cat result14.log && cat result12.log) | python3 ../tane_log_analyzer.py > meta_tane_uniprot.json
#python3 ../compareMeta.py meta_ddfd_uniprot.json meta_hyfd_uniprot.json meta_smartfd_uniprot.json meta_tane_uniprot.json 